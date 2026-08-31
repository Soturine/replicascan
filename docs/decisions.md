# Decisões Técnicas

## 1. MVP offline-first

Foi priorizado funcionamento local para reduzir complexidade operacional, risco de privacidade e custo de infraestrutura.

## 2. DI manual no `app`

Não foi introduzido framework de injeção para manter o bootstrap simples e fácil de entender. O `AppContainer` já cria uma base boa para migrar para Hilt/Koin no futuro, se fizer sentido.

## 3. Scanner híbrido

O projeto usa:

- ML Kit Document Scanner para o caminho principal, rápido e confiável;
- CameraX para captura manual quando o usuário precisa de controle fino;
- editor manual local como fallback obrigatório quando o palpite inicial não basta.

Isso evita prometer um pipeline local perfeito em todos os cenários, mas mantém autonomia de edição quando o scanner guiado não é suficiente.

## 4. Imagens de entrada persistidas

Desde a `v0.2.2`, imagens retornadas pelo scanner rápido, galeria ou CameraX são copiadas para armazenamento interno antes de entrar no Room. A decisão corrige a dependência de URIs temporárias e mantém preview, filtros, OCR e exportação apontando para uma fonte estável.

## 5. Quadrilátero normalizado

As coordenadas do crop são armazenadas em formato proporcional (`0..1`) em vez de pixels absolutos. Isso simplifica:

- preview responsivo;
- edição manual no overlay;
- reprocessamento da imagem em tamanhos diferentes.

## 6. Preview em duas etapas

A prévia de filtros não usa full-res na UI. O editor primeiro renderiza uma imagem intermediária rápida e depois substitui por uma refinada em segundo plano, com debounce, cancelamento de job e cache.

## 7. OCR com imagem preparada e pós-processamento

O OCR local não depende mais só do filtro final salvo da página. A base atual cria uma saída dedicada para leitura e preserva bounding boxes de blocos/linhas retornados pelo ML Kit.

Desde a `v0.2.5`, a UI deixa de tratar esses blocos crus como produto final. Um pós-processador puro em `core-common` ordena linhas por posição visual, agrupa parágrafos, reduz ruído pequeno e gera o texto consolidado usado por `Copiar tudo`. A decisão mantém ML Kit como engine, melhora a utilidade do resultado e evita levar um refactor profundo de pipeline para dentro desta fase.

## 8. Exportação local mais encontrável

PDF, JPG e PNG seguem locais, mas a base atual usa `Downloads/ReplicaScan` em Android 10+ para resolver o problema prático de “exportei e não achei o arquivo”. A UI escolhe primeiro entre `PDF` e `Imagem`, revela apenas opções relevantes e mostra nome, tipo, tamanho, local salvo, abrir e compartilhar depois de gerar.

## 9. Fonte canônica da página

Desde a `v0.2.6`, `sourceUri` é a base canônica da página. `processedUri` continua existindo para acelerar/mostrar derivados visuais, mas não é mais tratado como fonte final para exportação quando há estado lógico suficiente para reconstruir a página.

O pipeline local usa uma ordem explícita: carregar fonte, aplicar crop/perspectiva salvo, remover bordas pretas, aplicar rotação do usuário e então aplicar filtro ou preparar OCR. Ausência de crop salvo significa página inteira, o que evita recortar de novo imagens já tratadas pelo scanner rápido.

Essa decisão preserva a arquitetura atual, evita uma migração Room artificial e deixa as próximas versões com uma base mais previsível para QA visual, thumbnails e melhorias de cache.

## 10. Migration nunca destrutiva

Room exporta seu schema no repositório e abre sem `fallbackToDestructiveMigration`. Uma versão futura só pode alterar o schema com migration explícita e teste de preservação. Falhar de forma visível é preferível a apagar histórico.

## 11. Ownership e deleção física

Somente arquivos dentro dos namespaces canônicos do ReplicaScan podem ser removidos automaticamente. Fotos externas e exports finais não pertencem ao lifecycle da página. Falhas físicas produzem resultado parcial e o worker tenta apenas orphan cleanup conservador.

## 12. Dados documentais fora de backup automático

Imagens, Room, OCR e preferências são excluídos de cloud backup e device transfer. A decisão evita que uma regra implícita mova documentos sensíveis para outra máquina/conta.

## 13. FileProvider mínimo

O provider expõe apenas subdiretórios de export necessários. Intents concedem leitura temporária, incluindo `ClipData`, e nunca oferecem a raiz de `filesDir` ou `cacheDir`.

## 14. Sem Firebase no MVP

Foi uma decisão consciente para manter o escopo honesto, offline e sem dependências de backend antes da hora.
