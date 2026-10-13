# Checkpoint 0 — direção de composição (12 de outubro de 2026)

`STATUS: PROPOSTAS PARA REVISÃO; NÃO IMPLEMENTADAS NO APK; ACEITE PENDENTE`.

Abra [a prancha HTML](preview.html) para comparar as duas direções. Cada uma tem
Home com documento recente e sem documentos, exportação em preparação, trabalho e
sucesso, além da primeira cena da introdução. As 12 imagens em [`screens/`](screens/)
foram renderizadas no Chromium em **390 × 844 px**. São composições de navegador,
não capturas Android. [Capturas do APK atual](../evidence/review.html) e o
[vídeo de movimento atual](../evidence/mascot-motion-v1.mp4) ficam separados para
que a proposta não se passe por implementação.

## Fontes e comparação

O baseline foi confirmado em `052651bfe24bd3925c4c157d829c1b970721b657`
(`main == origin/main` em 2026-10-12), `0.4.1` / `versionCode=20` em preparação,
release pública `v0.4.0`. O ZIP `docs/design/visual-recovery.zip`, não versionado,
contém 58 entradas que duplicam a pasta de recuperação; foi apenas inspecionado
e permanece intacto. Os quatro ZIPs originais de Pipa/Lume foram encontrados em
`Downloads`; nomes e SHA-256 estão no [mapa de referências](../REFERENCE_MAP.md).
Os protótipos Pipa e Lume foram abertos em Chromium após a splash. As capturas
temporárias desses anexos permaneceram em `.tmp/`, fora do Git.

| Fonte | O que foi aproveitado | O que foi evitado |
|---|---|---|
| Pipa · Mesa de Domingo, marca e protótipo HTML | calor do papel, tipografia editorial, convite claro e continuidade da tarefa | nome Pipa, papel de carta com fita repetida, SVG da mascote, viewport como regra de produto |
| Lume · Biblioteca Acesa, marca e protótipo HTML | documento sob luz dirigida, contraste noturno e progresso visível | nome Lume, lanterna, raposa geométrica, depender só de tema escuro |
| APK atual | raposa aprovada, prévia real da página, formatos, navegação e estados funcionais | cartão hero genérico, mascote isolada, formulário de chips, movimento de imagem inteira |

**A — Mesa editorial:** bancada curta, página e raposa na mesma cena; CTA fica
dentro do trabalho principal. Recentes são uma lista compacta, não mais um cartão.
Na exportação, a página vira ficha do arquivo, as escolhas têm uma única linha,
e o sucesso mostra o documento identificável. Usa papel claro como superfície
útil. A raposa precisa interagir com a folha na implementação, não ficar sobreposta
como um PNG isolado.

**B — Luz no documento:** página grande dentro de um enquadramento de captura,
informação e ações em trilhas distintas; o CTA fica num dock inferior. A
exportação mantém a página como centro da sequência preparar → guardar → abrir.
A cena noturna demonstra a iluminação funcional de Lume, mas a estrutura é
diferente de A também no posicionamento, densidade e ordem das ações. Se B for
escolhida, a mesma composição deverá ganhar tema claro coerente; ela não pode
virar uma segunda marca exclusiva do modo escuro.

Ambas mantêm a raposa estática aprovada de `../candidates/` como referência de
identidade. Os dois PNGs de ícone desta pasta foram gerados como **explorações**
com a ferramenta de imagem integrada, usando `master-v1.png` como referência:
A enfatiza rosto e cantos de captura; B enfatiza uma folha iluminada nas patas.
Nenhum foi reduzido/avaliado a 24–108 dp, convertido em camadas de ícone
adaptativo, testado em máscaras, instalado no launcher ou aceito como arte final.
Não há fonte, SVG ou ilustração de terceiros incorporada às propostas; fontes
no HTML são substitutos do sistema e requerem decisão/licença antes do app.

## Diagnóstico verificável

| Achado | Evidência atual | Critério de fechamento |
|---|---|---|
| Movimento da raposa só por pose inteira | `ReplicaScanMascot.kt` anima `graphicsLayer` de `Image`; vídeo Android atual | camadas reais, piscada independente, cauda e pata articuladas; vídeo Android, escala e redução de movimento |
| Ícone plano sem parentesco artístico suficiente | `ic_launcher_foreground.xml` e `../evidence/launcher-masks.png` | nova arte original, três opções comparadas em 24–108 dp, máscaras e launcher instalado |
| Home com hero genérico | `ScanHero()` e `../evidence/home-recent.png` | página e personagem compõem uma tarefa; CTA, recentes e vazio funcionam a 200%/RTL |
| Exportação parece formulário | `ExportOptions()` usa `OutlinedTextField` e `FilterChip`; `../evidence/export.png` | prévia, destino, formato e ação formam um percurso com estados reais sem perder opções |
| Progresso tem pouca narrativa e é indeterminado | `ExportProgress()` e `../evidence/export-progress.png` | trabalho e resultado ligados à página real; não inventar porcentagem nem etapas que o domínio não informa |

As propostas não qualificam função, acessibilidade, tipografia, rig ou release.
Em particular, as barras/cenas de trabalho são linguagem visual: a implementação
deverá refletir apenas progresso real ou usar indicação indeterminada honesta.
O próximo checkpoint, depois de escolher a composição, deve produzir peças
editáveis da raposa e três ícones, validar movimento no Android e então aplicar
uma identidade única nas telas. O crop manual, `sourceUri`, OCR, Room, exportação
PDF/JPG/PNG e privacidade continuam protegidos pelos contratos do app.

Nenhuma tag ou release `v0.4.1` foi criada neste checkpoint. O aceite de uma
composição não é aceite final do movimento, de todas as telas ou da publicação.
