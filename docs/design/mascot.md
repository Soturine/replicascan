# Raposa — arte e runtime

Arte original gerada em 2026-10-12 pela ferramenta integrada imagegen, a pedido
do proprietário; nenhuma imagem Pipa/Lume foi copiada. Primeira explora??o (rejeitada pelo propriet?rio como feminina; mantida apenas como registro):
`assets/fox-concept.png`. Fonte de produção editável como atlas RGBA:
`core-ui/src/main/res/drawable-nodpi/replicascan_fox_atlas.png` (1254×1254).
Não há arquivo Rive, Lottie, SVG ou rig de editor externo oculto. O código/arte
corrente segue a licença do projeto, respeitados os termos da ferramenta de
geração; isto não é garantia de exclusividade ou registrabilidade da marca.

## Especificação da geração

Primeiro prompt: original fox helper for an adult/older-adult document scanner;
welcoming eyes, organic silhouette, auburn/apricot and cream, dark small nose,
full body, visible empty paws, curved tail, no clothes/glasses/text/background,
transparent RGBA, editorial illustration, not origami or clipart.

Segundo prompt (edição da primeira arte): strict 3×3 isolated part atlas,
transparent, same identity and registration; row 1 open/closed/concerned heads;
row 2 torso with feet/no head or arms, tail, left arm; row 3 right arm, paper,
teal folder; no labels/grid/assembled character. Resultado inspecionado antes
de integrar. Cabeças têm pequenas diferenças de contorno: qualificar flicker
e costuras em runtime, sem afirmar registro perfeito.

## Registro

Células 418×418, índices row-major 0–8. O runtime desenha regiões do atlas
diretamente, sem nove decodes nem imagens recortadas com fundo artificial.
Stage 240×240; posições e pivôs ficam no único `ReplicaScanMascot.kt`.
Cauda fica atrás do corpo; cabeça cobre o pescoço; braços articulam nos ombros;
papel e pasta têm movimento próprio. Olhos fecham pela expressão registrada.
Alterar a arte exige manter grade/alpha ou atualizar o registro e provas visuais.

| Estado | Ação | Duração | Interrupção/fallback |
|---|---|---|---|
| Welcome | aceno, cabeça e cauda, piscar | 2,4 s e repouso | cancela ao sair |
| Idle / Empty | respirar, piscar, repouso | 2,4 s | pose neutra |
| Scanning / Processing | folha e linha de leitura | ciclo 3,2 s + pausa | apenas trabalho real |
| Ocr | inclinação da cabeça e leitura | ciclo 3,2 s + pausa | estado de OCR governa |
| Working | papel descendo para pasta, patas | ciclo 3,2 s + pausa | estado de export governa |
| Success | aceno/check e expressão tranquila | 2,4 s e repouso | arquivo já confirmado |
| Attention | expressão empática, inclinação | 2,4 s e repouso | erro em texto, retry externo |

Lifecycle menor que RESUMED interrompe animação. ContentObserver acompanha
ANIMATOR_DURATION_SCALE; zero/reducedMotion usa pose estática sem loop.
Animatable respeita o clock/duração de Compose. A mascote não tem descrição
falada; o texto de estado do proprietário da tela é a alternativa acessível.

## Decisão e substituição

Compose + camadas evita dependência adicional e permite autoria efetivamente
entregue. Rive é alternativa se houver .riv válido e processo de edição; Lottie
é alternativa para sequências. Para substituir, manter a API de estados e
contrato de lifecycle/redução, validar todos os assets localmente e remover o
runtime/atlas antigo apenas após migração dos callers. Não usar timer de arte
para emitir sucesso de domínio. Evidência visual ainda deve ser registrada na
matriz do redesign; compilação isolada não aprova arte.

## Corre??o do checkpoint

Ap?s feedback do propriet?rio, o atlas foi regenerado com olhos menores sem
c?lios destacados, sobrancelhas simples e focinho mais largo. Prompt de corre??o:
?Edit the exact 3x3 atlas; friendly gender-neutral sturdy fox; remove eyelashes
and eyeliner, smaller warm brown eyes, broader muzzle, no makeup; preserve cell
registration; thicker rounded paws; tail/paper/folder unchanged; transparent.?
Patas s?o desenhadas atr?s da cabe?a/corpo, com piv?s ajustados; aceno reduzido
para n?o atravessar o rosto. A aceita??o est?tica da nova vers?o continua em
revis?o. N?o apresentar o primeiro conceito como mascote final aprovada.
