# Arquitetura

## Visão geral

O projeto foi dividido em módulos para manter o app simples no MVP, mas já com fronteiras claras entre domínio, dados e interface.

## Módulos

- `app`
  Responsável por `Application`, container de dependências, splash, onboarding e `NavHost`.
- `core-common`
  Contém modelos de domínio, contratos de repositório, resultado/validação e use cases centrais.
- `core-data`
  Implementa Room, DataStore, OCR, exportação, workers e pipeline local de imagem.
- `core-ui`
  Define tema, paleta, tipografia e componentes reutilizáveis de Compose.
- `feature-home`
  Entrada do produto com CTA principal de scanner rápido, ajuste manual secundário e lista recente.
- `feature-camera`
  Captura manual com CameraX.
- `feature-editor`
  Ajuste de cantos, filtros, rotação e revisão do lote.
- `feature-export`
  Escolha progressiva de PDF/Imagem, geração de arquivos e pós-exportação.
- `feature-history`
  Histórico pesquisável e detalhe do scan salvo.
- `feature-settings`
  Preferências locais e tela sobre.
- `feature-ocr`
  Reconhecimento de texto, revisão por trechos e cópia rápida.

## Fluxo de dados

1. A UI dispara eventos para um `ViewModel`.
2. O `ViewModel` conversa com contratos definidos em `core-common`.
3. As implementações concretas em `core-data` acessam Room, DataStore, OCR, exportação ou processamento de imagem.
4. O resultado volta como `Flow`/estado para a UI.

## Persistência

- `Room`
  Guarda scans, páginas, tags serializadas, favoritos, timestamps e estado de rascunho.
- `DataStore`
  Guarda onboarding, tema, modo manual padrão e qualidade padrão do PDF.

## Pipeline de imagem

`CanonicalImageDecoder` lê bounds, aplica sampling por dimensão/pixels e normaliza as oito orientações EXIF, incluindo mirror. Preview, processamento e OCR consomem esse sistema canônico.

O app usa duas estratégias complementares:

- `ML Kit Document Scanner`
  Caminho principal de captura/importação rápida quando disponível, com experiência guiada e menor atrito.
- Pipeline local em `DefaultDocumentProcessingRepository` e `HeuristicDocumentDetector`
  Voltado para captura manual e importação da galeria, com:
  - `sourceUri` como fonte canônica da página;
  - `processedUri` tratado apenas como derivado/cache visual;
  - chave pura de pipeline em `core-common` para versionar finalidade, crop, rotação, filtro e tamanho de saída;
  - candidatos de quadrilátero avaliados por geometria, contraste e perfil (`GENERAL`, `NOTEBOOK`, `RECEIPT`);
  - resultado tipado com confiança, `NO_DOCUMENT` e fallback conservador sem fingir detecção bem-sucedida;
  - warp de perspectiva com `Matrix.setPolyToPoly`;
  - ordem consistente de transformação: fonte, crop/perspectiva, rotação do usuário e filtro ou preparação de OCR;
  - normalização local de iluminação;
  - filtros recalibrados para documento, cinza, colorido e recibo;
  - prévia em duas etapas com cache;
  - saída dedicada para OCR e limpeza de borda preta.

Antes de criar um lote no Room, imagens vindas do scanner rápido, galeria ou CameraX são copiadas para `filesDir/scan-sources`. Assim preview, filtros, OCR e exportação não dependem de URIs temporárias fornecidas por outro app ou pelo scanner do Google.

Desde a `v0.2.7`, `ScanFileStore` faz a cópia atômica, valida o namespace gerenciado, executa rollback e remove fontes/derivados junto da deleção no repositório. Na v0.3.0, o schema Room passou à versão 2 por migration explícita, com unicidade de ordem, artefatos OCR e tabela virtual FTS. A configuração de produção não aceita migration destrutiva.

Derivados em `cacheDir/processed` são descartáveis. O carregador visual tenta a fonte canônica quando o derivado falha e encerra em erro discreto se nenhuma entrada puder ser aberta. O worker usa grace period para fontes órfãs e nunca toca em export final do usuário.

## OCR

O OCR local usa clientes reutilizáveis do ML Kit Text Recognition para escrita latina, devanágari, japonesa e coreana. A imagem enviada para reconhecimento não depende de thumbnail nem de `processedUri` salvo. A base deriva uma versão específica a partir de `sourceUri`, crop e rotação, registra engine/versão/script/readiness/fingerprint e preserva confiança e bounds de blocos, linhas e elementos.

Esse pós-processamento ordena linhas por posição visual, agrupa linhas próximas em parágrafos, descarta ruídos pequenos quando são claramente inúteis e gera texto consolidado. O artefato fica persistido por página e alimenta tanto `Copiar tudo` quanto a busca FTS. Idioma de interface e script OCR são decisões independentes.

## Exportação

PDF, JPG e PNG continuam sendo gerados localmente. Em Android 10+ a saída vai para `Downloads/ReplicaScan`, enquanto versões anteriores usam o armazenamento do app. Quando há crop, rotação ou filtro, a exportação rederiva a página a partir de `sourceUri`. PDF aceita página automática, A4 ou Letter e inclui uma camada de texto pesquisável quando existe OCR persistido; a imagem opaca aprovada continua definindo a aparência visual.

Se uma página não puder ser renderizada, a exportação falha com índice/ID e não publica sucesso incompleto. Imagens são escritas uma por vez e saídas parciais são removidas. O PDF escreve diretamente no destino, sem `ByteArrayOutputStream` ou roundtrip JPEG, e limita a resolução incorporada conforme a qualidade escolhida.

## Privacidade de plataforma

Backup e device transfer automáticos são desativados em profundidade pelo Manifest e pelas regras de extração. O FileProvider publica apenas diretórios específicos de export, nunca `scan-sources`, banco, preferências ou raízes de armazenamento. Consulte [data-lifecycle.md](data-lifecycle.md) e [threat-model.md](threat-model.md).

## Dependências principais

- Compose + Material 3
- Navigation Compose
- Lifecycle ViewModel / Runtime Compose
- Room
- DataStore
- WorkManager
- CameraX
- ML Kit Document Scanner
- ML Kit Text Recognition

## Rationale técnico

- Sem Hilt no MVP:
  DI manual reduz atrito inicial e mantém o projeto legível para contribuição.
- Room + DataStore:
  cobertura suficiente para offline-first sem backend.
- Scanner híbrido:
  o fluxo rápido cobre a maioria dos casos e o manual segue como fallback editável.
- Coordenadas normalizadas:
  simplificam preview, edição manual e reprocessamento em diferentes resoluções.
- `sourceUri` como fonte canônica:
  evita que preview reduzido ou cache antigo virem base de OCR/exportação.
