# Auditoria visual de recuperação — 2026-10-12

## Autoridade e baseline

`main` e `origin/main`: `42a494b9d16489be28d9c07e72a31ed1f4c8241e` após fetch.
`versionName=0.4.1`, `versionCode=20`, manifesto `publish=true` expressa intenção,
mas a release pública mais recente é `v0.4.0`; não há tag `v0.4.1`.
Identidade Git/autenticação: Soturine. Worktree limpa no início desta auditoria.
O proprietário rejeitou explicitamente a qualidade visual do ciclo anterior.
O feedback posterior rejeitou explorações realistas/furry e chapadas; a
referência solicitada agora é a própria raposa fofinha da v0.4.0.
Portanto, as capturas e os testes anteriores são baseline de regressão e
correção funcional, não aceite estético.

Escopo inspecionado: código de tema/mascote, screenshots Android de `docs/design/evidence`,
quatro ZIPs Pipa/Lume com HTML/CSS/JS/SVG e README, renders iniciais dos quatro
documentos no Chromium, navegação declarada e documentação de release.
Fluxos físicos e conteúdo de OCR real permanecem fora dessa amostra.
Derivanta foi usado como método de cobertura e contraprova, não como autoridade
de produto. Nenhum HTML, asset conceitual ou ZIP foi incorporado ao app.

## Achados com contraprovas

| ID | Severidade | Evidência do baseline | Causa e próximo critério |
|---|---|---|---|
| A1 | Bloqueante | `evidence/mascot-working-frame.png`, onboarding e export: olhos/rosto mudam por troca de célula e poses parecem coladas | Atlas 3×3 com cabeça inteira intercambiável, peças com pivôs fixos e senoides. Exigir master aprovado e continuidade frame a frame antes de runtime novo. |
| A2 | Alta | `evidence/home-recent.png`: raposa pequena no canto de cartão branco, grande vazio inferior | Hierarquia genérica; propor uma cena breve com documento e CTA claro, densidade de recentes adaptada ao conteúdo. |
| A3 | Bloqueante visual | `evidence/export.png`, `export-progress.png`: formulário/chips isolados e personagem no centro de muito espaço vazio | Composição não usa a página/documento como narrativa. Reconstruir preview, contexto, etapa de trabalho e resultado mantendo o estado real. |
| A4 | Alta | `evidence/onboarding*.png`: uma grande ilustração isolada sem continuidade visual entre páginas | Reusar um cenário e guia de três cenas, sem nome inventado para a raposa; preservar pular/voltar/scroll. |
| A5 | Média | `evidence/review.png` e `crop.png`: documento é legível e o crop contrastante, mas ferramentas e superfícies são genéricas | Preservar geometria/callback corrigidos. Elevar acabamento com régua/feedback discreto, evitar competir com a foto. |
| A6 | Média | `evidence/ocr*.png`, `history.png`, `settings.png`: repetição de cards ou áreas vazias | Ritmo próprio para leitura, biblioteca e preferências. Medir densidade a 200% e RTL. |
| A7 | Média | `Type.kt`: Serif/SansSerif de sistema sem família de marca definida | Avaliar fonte distribuível ou combinação nativa mais coerente em scripts variados, sem sacrificar 12 idiomas. |
| A8 | Média | `launcher.png`: ícone é seguro no círculo; visual simplificado pode ler só “raposa com folha” | Revisar gesto de escaneamento e themed monochrome em máscaras reais antes de mudar. |
| A9 | Alta | `site-mobile.png` local: página lateralmente rolável em 390px | Site também é superfície perceptível. Corrigir largura, testar 390 e desktop depois da direção aprovada. |

## Correções funcionais preservadas

`c4ab06a`, `454a43b` e os testes garantem crop sequencial, letterbox e imagem
exportada real. `cb9a14e` mantém o acesso a arquivos salvos quando o histórico
falha e distingue essa falha no texto. Importação privada, Room, OCR, ML Kit e
PDF/JPG/PNG não dependem da nova direção de arte. Alterações artísticas não
justificam regressão nesses contratos.

## Critério de saída

`VISUAL_APPROVAL: PENDING`. Apresentar captura nativa comparável, vídeo de
movimento no Android e matriz funcional; somente o proprietário pode aprovar
a estética. Testes verdes ou CI verde não substituem o aceite. Sem tag/release
v0.4.1 enquanto esse gate estiver pendente.
