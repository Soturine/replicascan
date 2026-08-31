# Estado atual do ReplicaScan

O ReplicaScan `0.4.0` é um scanner Android local-first. O fluxo principal usa a câmera própria com detecção ao vivo e lote multipágina; o ML Kit Document Scanner permanece como alternativa assistida e o seletor de imagens como entrada editável. Não há conta, backend, sincronização ou envio automático de documentos.

A v0.4.0 altera a identidade do pacote para `com.soturine.replicascan` e instala separadamente das builds históricas. O document engine e a UX funcional permanecem os da base endurecida na v0.3.1; esta versão concentra identidade, licença e release engineering.

## Fluxo do produto

1. Captura ou importação copia cada entrada válida para a área privada do app.
2. Room cria o lote e mantém páginas, ordem, crop, rotação, filtro, OCR, título, tags e favorito.
3. O editor deriva previews leves da fonte privada; os quatro cantos continuam ajustáveis.
4. OCR seleciona explicitamente o script, registra readiness, estrutura e fingerprint do pipeline.
5. O texto entra no índice FTS local e pode compor a camada pesquisável do PDF.
6. PDF, JPG e PNG são rederivados da fonte e das transformações aprovadas.

## Experiência e idiomas

- o tema claro/escuro compartilha a mesma hierarquia em creme, laranja, navy e teal;
- o onboarding aceita toque e gesto horizontal, respeita safe insets e usa três poses próprias da raposa;
- OCR, processamento, sucesso, estado vazio e atenção usam mascotes contextuais em PNG transparente;
- os nove módulos possuem catálogos completos para 12 idiomas;
- o árabe usa `supportsRtl`, ícones direcionais espelháveis e alinhamento lógico do Compose.

## Persistência

- `sourceUri` aponta para a fonte canônica privada em `filesDir/scan-sources`.
- Room guarda metadados, estado lógico, artefatos OCR e FTS. O schema atual é a versão 2, exportada em `core-data/schemas`, com migration 1→2.
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

- auto crop local continua conservador e pode devolver baixa confiança ou `NO_DOCUMENT`; caderno com espiral, perspectiva extrema e fundo poluído ainda exigem avaliação em dataset maior;
- PDF pesquisável depende de OCR já disponível e ainda requer validação ampla em leitores externos;
- OCR não promete precisão perfeita e árabe não é anunciado como script reconhecido nesta versão;
- testes instrumentados exigem emulador/aparelho e QA físico ainda está pendente;
- as traduções adicionais estão completas estruturalmente, mas ainda requerem revisão linguística nativa antes de publicação em loja;
- minificação/R8 e assinatura de loja ainda não são gates de produção.
