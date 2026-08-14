# Testes

## O que já está coberto

- validação de nomes de documentos;
- formatação de datas em pt-BR;
- busca por título e tags;
- busca FTS por título, tags e OCR após migration 1→2;
- construção de nomes de arquivo para exportação;
- pós-processamento de OCR: ordenação visual, agrupamento em parágrafos, descarte de ruído e texto consolidado;
- regras puras do pipeline de imagem: normalização de rotação/crop, chaves de preview/OCR/exportação, seleção de fonte derivada e invalidação de cache visual;
- ownership/path traversal, importação parcial, rollback, orphan cleanup e captura não concorrente;
- testes instrumentados de Room para CRUD/cascade/ordem, migration, FTS, deleção física, export fail-fast e PDF pesquisável em API 35;
- regressão de metadata/save que preserva três páginas e valida reordenação estrita;
- teste instrumentado mínimo de inicialização da UI;
- schemas Room versionados em `core-data/schemas`.

## O que ainda falta

- cobertura mais forte do pipeline de imagem;
- testes de navegação e fluxos completos com Compose;
- execução física da matriz completa de scripts OCR e leitores externos de PDF;
- QA visual em aparelho real com publicação das capturas oficiais.

## Cenários de OCR para QA manual

- folha impressa com parágrafos longos;
- caderno manuscrito com linhas próximas;
- recibo com valores curtos e símbolos;
- foto torta ou importada da galeria;
- página com pouco texto;
- imagem sem texto detectável.

## Cenários de pipeline de imagem para QA manual

- scan rápido de folha A4;
- importação de galeria;
- caderno com espiral;
- manuscrito;
- tela de notebook fotografada;
- recibo;
- imagem girada;
- fundo poluído;
- crop manual alterado antes dos filtros;
- filtro alterado depois do crop;
- OCR depois de ajustar crop/filtro;
- exportação e compartilhamento depois de OCR.

Verificações principais:

- revisão e filtros mostram o mesmo enquadramento;
- OCR lê a página aprovada, não uma thumbnail;
- PDF/JPG/PNG exportado bate com a revisão;
- não há zoom inesperado por `ContentScale.Crop`;
- rotação é aplicada uma vez só;
- alteração de crop, rotação ou filtro invalida o derivado visual anterior.

## Como rodar

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lint
./gradlew check
./gradlew assembleRelease
./gradlew assembleDebugAndroidTest
./gradlew connectedDebugAndroidTest
```

## Estratégia sugerida para evoluir

- manter lógica pura em use cases e helpers testáveis em JVM;
- isolar regras de exportação, mapeamento, preparação e pós-processamento de OCR;
- introduzir testes de UI por fluxo principal à medida que o produto estabilizar.
