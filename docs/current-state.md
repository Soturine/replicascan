# Estado atual do Scanora

O Scanora `0.2.9` é um scanner Android local-first. O fluxo principal abre o ML Kit Document Scanner; CameraX e o seletor de imagens permanecem como alternativas editáveis. Não há conta, backend, sincronização ou envio automático de documentos.

## Fluxo do produto

1. Captura ou importação copia cada entrada válida para a área privada do app.
2. Room cria o lote e mantém páginas, ordem, crop, rotação, filtro, OCR, título, tags e favorito.
3. O editor deriva previews leves da fonte privada; os quatro cantos continuam ajustáveis.
4. OCR usa ML Kit Text Recognition no aparelho e salva o texto consolidado da página.
5. PDF, JPG e PNG são rederivados da fonte e das transformações aprovadas.

## Persistência

- `sourceUri` aponta para a fonte canônica privada em `filesDir/scan-sources`.
- Room guarda metadados e estado lógico. O schema atual permanece na versão 1 e é exportado em `core-data/schemas`.
- `processedUri` é somente um hint para cache regenerável em `cacheDir/processed`.
- DataStore guarda preferências não documentais.
- Excluir página ou lote remove os registros e tenta remover todos os arquivos privados gerenciados correspondentes.

## Módulos

- `app`: bootstrap, navegação e coordenação de drafts/compartilhamento;
- `core-common`: modelos e contratos;
- `core-data`: Room, lifecycle de arquivos, processamento, OCR, exportação e cleanup;
- `core-ui`: tema, componentes e carregamento de imagem com fallback;
- `feature-*`: fluxos de produto por contexto.

## Limitações atuais

- auto crop local ainda perde para o scanner do Google em caderno com espiral, perspectiva extrema e fundo poluído;
- PDF ainda possui oportunidades de streaming e sizing; imagens já são processadas sequencialmente;
- OCR não promete precisão perfeita, busca global ou PDF pesquisável;
- testes instrumentados exigem emulador/aparelho e QA físico ainda está pendente;
- minificação/R8 e assinatura de loja ainda não são gates de produção.
