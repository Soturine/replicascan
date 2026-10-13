# Raposa — arte, runtime e limites

`STATUS: POSES APROVADAS; RIG CANDIDATO, MOVIMENTO E TELAS EM AVALIAÇÃO` (2026-10-13).

A referência de identidade é a raposa sentada com bandana turquesa da tag
`v0.4.0`, preservada em `visual-recovery/candidates/original-v0.4.0.png`.
O master `master-v1.png` foi editado a partir dessa arte pela ferramenta
imagegen e aprovado pelo proprietário como estilo. As poses de saudação,
leitura, trabalho, sucesso, atenção e vazio receberam aceite visual expresso
na [prancha comparativa](visual-recovery/candidates/review.html). Não foram
copiados assets Pipa/Lume. A pose de atenção foi corrigida após o proprietário
identificar patas dianteiras extras: agora uma aponta para a folha, outra a
segura, e duas patas traseiras ficam apoiadas. O código e a arte empacotada seguem `LICENSE` e os
termos de uso da ferramenta; aprovação estética não é garantia de exclusividade
ou registrabilidade.

## Implementação atual

`core-ui` contém sete PNGs RGBA de pose completa e seis variantes de olhos
fechados. `ReplicaScanMascot.kt` e `FoxRig.kt` mapeiam estados do produto para
essas poses, respeitam `RESUMED`, a escala global de animação e `reducedMotion`.
O render Compose recorta a arte em camadas para girar cauda, cabeça, patas e
folha em pivôs independentes; os olhos fechados substituem apenas a região dos
olhos durante um piscar curto. Estados de trabalho acrescentam um traço de
leitura sobre a folha. O documento/arquivo real governa os estados, nunca o relógio da
arte. A sombra fica ancorada ao tamanho solicitado. A pose estática continua
legível com animações desativadas.

| Estado do componente | Pose | Ação visual |
|---|---|---|
| Welcome | greeting | balanço de saudação |
| Idle | master | respiração pausada |
| Scanning / Processing / Working | working | respiração e traço na folha |
| Ocr | reading | leitura com pequeno balanço |
| Empty | empty | gesto convidativo |
| Attention | error | expressão de cuidado |
| Success | success | celebração curta, sem loop |

O atlas 3×3 rejeitado foi removido do runtime, permanecendo no histórico Git.
O rig candidato articula regiões das poses aprovadas durante o render, mas os
PNGs de origem não são arquivos autorais separados por membro. Máscaras,
oclusão e intervalos de movimento ainda precisam de revisão visual final.
Rive exigiria um `.riv` autoral inexistente e runtime novo; Lottie exigiria
JSON de animação autoral inexistente. Compose usa o stack presente e preserva
lifecycle e acessibilidade, mas a qualidade do movimento ainda precisa ser
avaliada pelo proprietário. Não foi adicionada dependência apenas para parecer
mais sofisticado.

## Evidência e custo

O [clipe v2 de 8 s](visual-recovery/evidence/mascot-motion-v2.mp4) foi montado de
64 frames PNG capturados do render Compose no emulador Android 14, com relógio
de teste avançado em passos de 125 ms. Mostra idle → trabalho → sucesso →
atenção; não é gravação de câmera nem prova de fluxo real de exportação.
`RebrandVisualTest` também salva dois frames consecutivos e captura estática
com movimento reduzido. As telas de UI usam fixture sintética e estados
injetados. O custo do novo APK, jank, RAM e energia ainda não foi medido após
as variantes de olhos fechados.

O projeto ainda precisa de inspeção humana das transições e das telas em
dispositivos reais, tema escuro, fonte 200%, RTL e `Animator scale=0`.
`VISUAL_APPROVAL: PENDING` para movimento e sistema completo; nenhum teste
automático converte esse estado em aprovado.
