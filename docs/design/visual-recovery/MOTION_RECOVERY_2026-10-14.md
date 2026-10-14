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
vídeos nativos contínuos de idle/working acima de 60 s, inspeção de quadros
e extremos, retorno real de background, benchmark e arte anatômica em camadas.
Enquanto isso, a release de avaliação fica bloqueada pelo critério de motion
do pedido de 2026-10-14.
