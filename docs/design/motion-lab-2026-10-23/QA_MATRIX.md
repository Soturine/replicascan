# Matriz de validação · experimento de 23/10/2026

`PASS` é uma propriedade observada dentro do escopo declarado; `FAIL` impede integração; `NOT_RUN` não equivale a aprovação. As poses atuais foram exibidas no laboratório HTML em quatro tamanhos, mas isso não é uma captura Android dos novos candidatos. Os atlas 3/4 feitos após receber as duas referências também falharam em proporções, encaixes ou alfa.

| Situação | Pose atual no laboratório 72/112/168/240 dp | Fonte em camadas candidata | Gestos/extremos candidato | Android candidato | Inspeção artística |
|---|---|---|---|---|---|
| Welcome | PASS · greeting estática | FAIL · atlas duplica patas | NOT_RUN | NOT_RUN | Pendente |
| Idle | PASS · idle estática | FAIL · atlas duplica patas | FAIL · cels mudam proporção | NOT_RUN | Pendente |
| Scanning | PASS · working estática | FAIL | NOT_RUN | NOT_RUN | Pendente |
| Processing | PASS · working estática | FAIL | NOT_RUN | NOT_RUN | Pendente |
| Working | PASS · working estática | FAIL | NOT_RUN | NOT_RUN | Pendente |
| Ocr / exportação | PASS · reading estática | FAIL | NOT_RUN | NOT_RUN | Pendente |
| Empty | PASS · empty estática | FAIL | NOT_RUN | NOT_RUN | Pendente |
| Attention / erro | PASS · attention estática | FAIL | NOT_RUN | NOT_RUN | Pendente |
| Success | PASS · success estática | FAIL | NOT_RUN | NOT_RUN | Pendente |

Além das escalas: temas claro/escuro, RTL, fonte 200%, reduced motion, 90 s por estado, 20+ trocas, lifecycle, medição de jank, QA físico, integração Compose e vídeo Android **da animação candidata**: `NOT_RUN`, porque os assets falharam antes da integração. As [capturas e o vídeo do Android atual](../motion-final/review.html) verificam a contenção estática anterior; não são evidência do candidato. Os testes de imagem decodificada em Export e Review cobrem somente o conteúdo carregado e visível nas cenas instrumentadas.
