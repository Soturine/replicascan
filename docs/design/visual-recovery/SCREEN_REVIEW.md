# Revisão das telas — checkpoint de composição

`STATUS: REVISÃO PARCIAL, ACEITE VISUAL PENDENTE` (2026-10-12).

Fonte: [capturas atuais e comparação Android](evidence/review.html), com
documento sintético no emulador API 34. O proprietário aprovou o master e as
poses estáticas da raposa e autorizou continuar a revisão. Isso não aprova
automaticamente todas as telas, o movimento ou a release.

| Destino/estado | Observação e decisão | Estado |
|---|---|---|
| Home com recentes e vazia | O texto disputava largura com uma raposa pequena. A arte passou a 136 dp e o texto de apoio ocupa a largura completa; em fonte 200%, a arte sai para preservar conteúdo e ações. Espaço inferior com poucos itens continua natural ao conteúdo. | Implementado; capturas API 34 claras, escuras e 200%; aceite pendente |
| Onboarding (3 páginas) | Saudação, leitura e sucesso agora acompanham escanear, revisar e guardar; antes, as duas primeiras páginas repetiam a pose de trabalho. A sequência ainda precisa de inspeção física de swipe/RTL. | Implementado; cinco testes Android focados passaram com a matriz visual; aceite pendente |
| Revisão, crop e filtros | A página é protagonista, o recorte mantém quatro cantos e página inteira por padrão. Não mover controles nem geometria sem corpus real e teste de gesto. | Preservado; QA físico pendente |
| OCR carregando, pronto e vazio | Texto reconhecido é a peça principal; o grande espaço livre com texto curto não deve ser preenchido por decoração. Estados de modelo e seleção precisam inspeção com OCR real e TalkBack. | Capturado com estado injetado; fluxo real pendente |
| Exportação: PDF/JPG/PNG | Prévia, quantidade de páginas e formato atual agora formam uma superfície única; nome, qualidade e tamanho continuam editáveis e progressivos. Estado escuro com fonte 200% exige rolagem para opções inferiores. | Implementado; capturas API 34; aceite pendente |
| Exportação: progresso, sucesso, erro | Progresso mostra a página real e trabalho em curso, agora agrupados no centro da altura disponível; sucesso identifica o arquivo; erro mantém recuperação. O movimento continua por pose inteira. | Capturado com estado injetado; exportação real/aceite pendentes |
| Histórico, busca, detalhe | Uma biblioteca com só um item deixa espaço livre; não adicionar conteúdo artificial. Busca e ações documentais devem ser exercitadas em aparelho. | Capturado; interação pendente |
| Configurações, idioma, sobre | Grupos e alvos de toque estão claros, embora a linguagem visual ainda seja próxima do Material padrão. Validar todos os idiomas, RTL e fonte ampliada. | Capturado; revisão linguística/QA pendentes |
| Ícone adaptativo | Raposa + documento são legíveis nas máscaras capturadas, mas o traço vetorial é mais plano que a mascote aprovada. | Divergência visual aberta; escolha do proprietário pendente |
| Site | Capturas de Home e opções de exportação atualizadas. | Implementado; gate local do site passou, publicação remota pendente |

As POCs Pipa/Lume informam hierarquia e ritmo, sem fornecer código ou assets de
produção. O [mapa de referências](REFERENCE_MAP.md) lista as divergências. A
[matriz de validação](VALIDATION_MATRIX.md) distingue a inspeção das capturas,
testes instrumentais, QA físico e aprovação estética.
