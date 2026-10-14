# Raposa: diagnóstico e contenção do recorte

`STATUS: CONTENÇÃO IMPLEMENTADA; RIG ANATÔMICO FINAL PENDENTE` — 2026-10-14.

| Achado | Classificação | Evidência | Decisão |
|---|---|---|---|
| Intervalo imóvel entre ciclos | CONFIRMED | `ReplicaScanMascot.kt` usava `delay(420/1200)` após a animação e `snapTo(0)` | Clocks independentes e reversíveis de respiração/luz, sem intervalo ou snap; sucesso fica em pose estável |
| Pixels da bandana/corpo acompanhando a cauda e cauda duplicada | CONFIRMED | `FoxRig.kt` recortava uma área ampla da pintura completa, girava-a e redesenhava outras áreas sobre a mesma pose | Desativar todos os recortes anatômicos artificiais; desenhar a pose aprovada inteira e sobrepor só a variante de pálpebras nos olhos |
| Parada permanente em estados de loop | NOT_VERIFIED | Não houve reprodução isolada da parada; o loop antigo reiniciava por lifecycle/scale e tinha pausas, sem prova desta causa específica | Teste instrumental avança 31 s e remonta o componente; investigar novamente se a parada aparecer em aparelho real |

O código atual não move cauda, cabeça, braços nem folha de forma independente.
Sem fonte editável separada e oclusão reconstruída, animar esses objetos por
polígonos volta a expor o defeito. Respiração move a pose completa poucos pixels;
a luz de scan percorre a folha nos estados de trabalho; a piscada usa a variante
da mesma pose. `ON_STOP`, redução de movimento e Animator scale 0 removem os
clocks da composição; o retorno os inicia numa posição neutra. Esta solução
é uma contenção segura, **não** a entrega de um rig articulado definitivo.

`mascotLoopKeepsMovingAfterThirtySecondsAndRemount` passou no emulador API 34
com relógio de teste controlado; `motionFramesAndReducedMotionAreDifferentContracts`
também passou. Esses testes demonstram continuidade de estado, não qualidade
artística, ausência de jank ou comportamento em dispositivo físico. Ainda faltam
inspeção completa de quadros e extremos, retorno real de background, benchmark
e arte anatômica em camadas.
Enquanto isso, a release de avaliação fica bloqueada pelo critério de motion
do pedido de 2026-10-14.

### Tentativa de vídeo contínuo no emulador

Uma Activity **somente do build debug** (`MascotMotionActivity`) foi criada para
executar a cena fora do relógio controlado dos testes Compose. O emulador Pixel
7/API 34 em 1080×2400 demorou **1 min 29 s para exibir a primeira tela** dessa
Activity após o `am start`. Um `adb screenrecord` iniciado antes desse primeiro
desenho capturou a splash, e não a raposa. A captura iniciada depois de a
personagem estar visível terminou com **48,55 s**, 71 quadros e média de
**1,46 quadros/s**; portanto **não** satisfaz a prova nativa >60 s. Esses
arquivos inválidos foram descartados, não publicados como demonstração de
fluidez. A fixture instrumental com `Thread.sleep` também foi rejeitada porque
o relógio de teste não gera animação real durante a espera.

No mesmo emulador saturado, `dumpsys gfxinfo` relatou 800 quadros renderizados,
797 janky (**99,62%**), e `dumpsys meminfo` registrou TOTAL PSS **119.760 KiB**
e RSS **197.816 KiB** para o processo. Esses números são do ambiente de teste
sob carga, não uma medição física representativa nem diagnóstico isolado do
desenho da raposa. A gravação foi repetida após reiniciar o emulador, conforme
abaixo; medir jank em ambiente estável e aparelho físico, inspecionar todos os
frames e construir o rig real continuam pendências bloqueantes.

Após reiniciar o emulador, as cenas **Working** e **Idle** foram gravadas fora
da instrumentação em clipes contínuos reais. [Working](evidence/oct14-mascot-working-native.mp4)
tem **118,67 s/1.126 quadros** (média **9,49 fps**); [Idle](evidence/oct14-mascot-idle-native.mp4)
tem **118,89 s/1.349 quadros** (média **11,35 fps**). Quadros em 10, 60 e 110 s
mostram personagem e folha estáveis, sem duplicação de cauda/bandana; a luz
percorre a folha em Working. A cadência gravada ainda é baixa para aprovar
fluidez, e os vídeos não demonstram articulação de membros. A primeira tentativa
de 48,55 s acima fica como diagnóstico da saturação antes do reboot, não como
vídeo entregue. Inspeção de todas as junções, QA físico e rig anatômico ainda
faltam e bloqueiam o motion final.
