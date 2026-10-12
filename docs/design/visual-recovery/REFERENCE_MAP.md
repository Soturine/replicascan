# Mapa de referências e anexos

Os quatro ZIPs estão em `Downloads`; SHA-256 e cobertura abaixo. Cada arquivo
contém HTML, `support.js`, React local e README. CSS/JS/SVG foram lidos; primeira
tela do artboard e do protótipo foi renderizada em Chromium. As interações ainda
precisam de navegação sistemática; as telas dos POCs não provam serviço real.

| Arquivo | SHA-256 | Papel / observação |
|---|---|---|
| Lume, marca e sistema | `87F97F82EE60F5305030D1111E8EAC4D85E4781753B8C96687E07EF6F26D4349` | foco noturno, luz sobre documento, contraste; rejeitar personagem geométrico e noite como única identidade |
| Lume, protótipo | `3ACD7D6029F752EF6F942D072F3730933C90BADDD9C278CDE08D7995AEB8CFCC` | ritmo de onboarding e estados; viewport fixa 390×844, timers de OCR/export são simulações |
| Pipa, marca e sistema | `70DCFDE921F80D8D8C5EC6E4EBB2B567B0A093F264A4B43A689E27B48FDDEA44` | papel quente, texto claro, detalhes de mesa; rejeitar fita/lápis em toda tela e cartoon SVG como arte final |
| Pipa, protótipo | `8A833720204C0DF2E196A694ED91489DE43728BE0E695006417395908F5828A0` | progressão de tarefa e affordance de arquivo; viewport fixa, capture/OCR/export simulados |

O README interno dos ZIPs sugere replicação fiel de valores. Essa orientação é
do artefato de referência; o pedido do proprietário proíbe cópia literal.
As fontes Google Young Serif/Nunito Sans/Lexend aparecem por URL remota no HTML.
Não foram licenciadas, baixadas ou distribuídas no app; o render pode usar
fallback conforme rede/cache. Os SVGs estão inline, sem master editável útil
para o personagem novo.

Outras referências: screenshot Android anterior em `docs/design/evidence/`
(captura real com fixture; visual rejeitado), `docs/design/assets/fox-concept.png`
(primeira exploração rejeitada), e histórico `v0.4.0` em Git. Screenshots antigos
separados e auditorias externas citados pelo pedido não estavam anexados aqui.
`docs/design.zip` é um pacote versionado do ciclo anterior, não um POC do Claude.

Princípios aproveitados: calor do papel e palavras de Pipa; legibilidade noturna
e luz dirigida ao documento de Lume; simplicidade do fluxo real do ReplicaScan.
Novidade necessária: evolução cuidadosa da raposa histórica e cenas ligadas à tarefa real,
com cor/luz coerentes entre temas e sem prop decorativo repetido.
