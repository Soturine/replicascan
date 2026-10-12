# Rebrand — auditoria e plano de implementação

## Baseline congelado

2026-10-12: `main`/`origin/main` em `473789785509cf9c73d3ccbefdac37a565b05fab`.
Release pública `v0.4.0`, tag histórica em `a78001b`, versão 0.4.0/code 19.
Identidade autenticada e autoria existente: Soturine. Alterações locais da IDE em
`gradle.properties` e `gradle/gradle-daemon-jvm.properties` precedem este trabalho.
Não fazem parte do redesign. Build inicial `:app:assembleDebug`: passou.
Emulador Pixel_7_recor disponível; nenhum dispositivo físico conectado.

## Evidência recebida

Lidos os dois pedidos textuais e os quatro ZIPs Pipa/Lume (marca e protótipo).
HTML/CSS e lógica dos protótipos inspecionados como referência, sem transpilação.
Os dois protótipos fixam 390×844 com overflow hidden; captura, OCR e export usam
timers. Essas simulações não demonstram bugs equivalentes no aplicativo nativo.
Pipa oferece hierarquia de papel e leitura acolhedora; Lume oferece continuidade
entre estados e contraste noturno. Cenários decorativos, medidas fixas e serviços
inexistentes não serão incorporados. Screenshots privados, auditorias anteriores
nomeadas no pedido e conceitos A/B/C não foram anexados nesta execução.
Os ZIPs ficam fora do versionamento público.

Derivanta consultado em `05a60d8b2b2087eb38905eae0c959b3c1c7ac87b`:
skill, AUDIT, FRONTEND_PRODUCT_UX, MOBILE_DESKTOP, DELIVERY_GIT_CI_RELEASES,
ACCESSIBILITY_I18N e RESEARCH_POLICY. Cobertura proporcional, sem alterações upstream.

## Findings e contraprovas

| ID | Evidência AS-IS | Ação / estado inicial |
|---|---|---|
| V01 | Mascote troca PNGs inteiros; busy apenas oscila o bitmap | PARTIAL: substituir por rig com peças independentes e lifecycle |
| V02 | ExportOptions continua durante isExporting; raposa aparece apenas em sucesso | PARTIAL: cena de exportação e recuperação persistentes |
| V03 | Foreground launcher é bitmap gravity=fill | PARTIAL: símbolo novo em safe area, monochrome correspondente |
| V04 | Botão branco sobre laranja #E95F0C precisa contraste maior | PARTIAL: terracota mais escura e pares medidos |
| V05 | ReplicaScanContent aplica fillMaxWidth antes de widthIn | PARTIAL: limitar largura antes de preencher |
| V06 | Crop possui quad canônico, lupa, snap e ações TalkBack | Preservar; melhorar contraste, seleção e verificar render real |
| V07 | Onboarding depende de grandes imagens estáticas e coluna sem scroll | PARTIAL: cenas do rig e conteúdo adaptativo rolável |
| V08 | Home lê drawable diretamente, duplicando autoridade da mascote | PARTIAL: migrar para componente compartilhado |
| V09 | Erro de export some após snackbar; seleção permanece ativa enquanto grava | PARTIAL: erro inline recuperável; congelar seleção durante operação |

Inspeção inicial profunda: tema/componentes/mascote, Home, crop/overlay, export
UI/ViewModel e onboarding. Review, OCR, histórico, settings, pipeline e testes
recebem inspeção adicional antes de suas mudanças. Este inventário não equivale
a execução dos fluxos nem a conformidade de acessibilidade.

## Direção: papel, tinta e luz

Uma marca: papel marfim, tinta petróleo, terracota de contraste e verde mineral.
Claro/escuro compartilham papéis semânticos. Títulos editoriais discretos, corpo
do sistema com fallback nativo para todos os scripts; sem download de fontes.
Documento domina edição; mascote acolhe, acompanha trabalho e confirma resultado.
Sem personagem em cada toolbar, sem percentuais inventados, sem loops de sucesso.
Componentes compartilhados governam espaço, formas, foco e feedback.

## Motion e autoria

Exploração original gerada com ferramenta imagegen; atlas com cabeça, expressões,
corpo, cauda, patas, papel e pasta. Arte é candidata até montagem e inspeção real.
Compose + atlas é a opção executável: não exige novo runtime, autoria externa ou
arquivo binário não produzido. Rive oferece state machines, mas requer asset .riv
válido; Lottie atende sequências predefinidas e também requer composição válida.
Não há .riv/.lottie entregue. Estado do domínio governa cenas, nunca o inverso.
Frames decorativos não são anunciados pelo TalkBack. Motion off e background
precisam interromper atualização; sucesso/erro assentam em pose estática.

Fontes oficiais consultadas em 2026-10-12:
- https://github.com/rive-app/rive-android
- https://rive.app/docs/auth
- https://github.com/airbnb/lottie/blob/master/android-compose.md
- https://developer.android.com/develop/ui/compose/system/icon_design_adaptive

## Ordem e aceitação

1. Fundamentos e limites adaptativos, contraste e ícone.
2. Arte/rig, redução de motion, lifecycle e cenas verificáveis.
3. Onboarding/Home; crop/review/filtros; OCR; export; biblioteca/settings.
4. Evidência de Compose/APK com fixtures sintéticas, estados e variações.
5. Gates locais consolidados; commits temáticos; push main; consulta única de CI.
6. Release 0.4.1/code 20 somente com gates materiais satisfeitos, artefato e SHA
   coincidentes. Não publicar se falta evidência visual essencial.

Matriz final e resultados serão atualizados com fatos executados. QA físico,
TalkBack humano e revisão linguística nativa permanecem pendentes até execução.
