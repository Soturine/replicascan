# Capturas Android da recuperação visual

`RebrandVisualTest` renderizou os componentes Compose no emulador Pixel 7,
Android 14 / API 34, com APK debug da v0.4.1 em desenvolvimento. As capturas
medem 1080×2201 px, usam a localização inglesa do emulador, documento
sintético e estados injetados; não demonstram captura física, OCR real ou
escrita de exportação. Preparo, gravação, sucesso, erro e aviso da biblioteca
na exportação foram recapturados em 2026-10-13 com a mesma cena nativa da
folha real e da raposa, variando a pose conforme o estado.

Em 2026-10-13 o preparo foi recapturado após trocar o campo Material e os chips
sempre expostos por uma edição de nome integrada, três escolhas de formato e
qualidade/tamanho de página sob disclosure. `export-setup-before.png` preserva
o estado Android imediatamente anterior; `export.png`, `export-jpg.png`,
`export-png.png`, `export-error.png` e `export-dark-200.png` mostram o novo
estado com fixture sintético. `export-progress.png`, sucesso e warning
pós-salvamento não mudaram nesta fatia. O teste instrumental também abre o
disclosure e troca PDF/JPG, mas não grava arquivos reais.

`export-eye-before.png` registra o contorno duplicado que aparecia sobre os olhos
fechados na pose de leitura. A máscara foi ajustada e `export-blink.png` mostra a
piscada limpa no Android; `export.png`, `export-jpg.png` e `export-png.png` foram
recapturados com os olhos abertos. Esta comparação verifica o defeito visual
relatado, mas a composição final continua sujeita à revisão do proprietário.

As três páginas de introdução foram recapturadas após receber poses distintas. Agora a folha e a raposa formam
uma cena aberta nas três páginas, sem cartão de fundo; `onboarding-200.png`
mostra a versão compacta em fonte 200%. A Home foi recapturada em
2026-10-13 com cena de papel e raposa, botão principal ampliado, estado vazio
sem cartão e folha iluminada no tema escuro; `home-dark-200.png` documenta a
composição sem ilustração em fonte 200%. As demais capturas vêm da
fatia anterior do mesmo runtime com as poses aprovadas.

`review.png`, `crop.png` e `review-dark-200-rtl.png` foram recapturados em
2026-10-13 após a iluminação e a profundidade da página. O teste confirma
renderização desses estados; o gesto de recorte ainda requer QA físico.
`filters.png` e `ocr-ready.png` foram recapturados após a folha compartilhada
dos filtros e o agrupamento selecionável dos parágrafos do OCR.
`detail.png` e `settings.png` mostram a página única ampliada e grupos de
preferências sem cartões repetidos, também recapturados em 2026-10-13.
`history-empty.png` e `search-empty.png` mostram os dois vazios do Histórico
sem cartões; as capturas usam estados injetados e não validam busca real.
`launcher-masks.png` foi atualizado após instalar o ícone candidato 4 no APK
debug; a comparação das quatro opções e o launcher real estão em
[`icon-candidates/`](../icon-candidates/index.html).

O [clipe v2](mascot-motion-v2.mp4) reúne 64 frames renderizados no Android com o
clock de teste em passos de 125 ms e codificados em MP4 a 8 fps. Sequência:
repouso → trabalho → sucesso → atenção. Não é `adb screenrecord` em tempo real,
nem prova de transição da navegação; permite inspeção de continuidade visual.
O [clipe contínuo Android](mascot-motion-live-android.mp4) é uma gravação real
via `adb screenrecord` no emulador API 34 (720×1600, 25,72 s, 255 quadros úteis,
taxa variável média de 9,86 quadros/s). Mostra Home → opções de exportação →
gravação → sucesso → erro, sem quadros interpolados. O vídeo foi cortado apenas
para remover o launcher e a inicialização branca do teste instrumental.
`MascotJourneyCaptureTest` injeta esses estados e um documento sintético: o
arquivo de sucesso não foi gravado, e a transição não é a navegação de produção.
A cadência baixa deste emulador impede declarar a fluidez final aprovada; o
clipe serve para verificar as composições, a continuidade e as costuras da arte.
As poses de 1254 px agora são decodificadas a metade da largura no runtime;
o teste instrumental de movimento/redução passou após essa mudança. Isso
reduz a carga de desenho sem alterar os PNGs-fonte ou prometer ganho de fps em
dispositivos físicos.
Os [frames 21](mascot-working-frame-21.png) e
[22](mascot-working-frame-22.png) mostram estados consecutivos da pose de
trabalho. A [captura de atenção corrigida](mascot-attention-corrected-android.png)
mostra quatro patas após a revisão anatômica solicitada em 2026-10-13.

A [comparação lado a lado](review.html) mostra Home, revisão, recorte, opções e
estados de exportação, introdução, OCR, histórico, configurações e ícone contra
o baseline rejeitado em `docs/design/evidence/`. Este diretório também contém
as demais variantes capturadas pelo teste: formato JPG/PNG, erro, biblioteca,
tema escuro, texto ampliado e RTL. A captura usa o mesmo fixture e viewport de
1080×2201 px, mas não equivale à inspeção humana em dispositivo físico.

Master e poses estáticas receberam aprovação do proprietário. Movimento,
composição das telas e release ainda precisam de aceite expresso. O antigo
`docs/design/evidence/` permanece como baseline rejeitado.

Pontos abertos para esta revisão: a Home ainda deixa espaço inferior vazio
quando a biblioteca é curta; a exportação ainda precisa de validação visual
em documentos físicos e prova de gravação completa fora do fixture sintético;
o ícone temático vetorial usa linguagem mais plana que a mascote; o rig candidato
articula regiões das poses, mas ainda depende de máscaras sobre imagens
completas. A inspeção manual de transições, recorte com
câmera real, tema escuro, fonte 200%, RTL e TalkBack também permanece pendente.
As capturas de tema escuro/fonte 200% são amostras, não QA humano completo.
As diferenças voluntárias em relação às POCs Pipa/Lume estão registradas no
[mapa de referências](../REFERENCE_MAP.md); nenhum dos HTMLs ou assets delas
foi incorporado ao produto.
