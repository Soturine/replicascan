# Capturas Android da recuperação visual

`RebrandVisualTest` renderizou os componentes Compose no emulador Pixel 7,
Android 14 / API 34, com APK debug da v0.4.1 em desenvolvimento. As capturas
medem 1080×2201 px, usam a localização inglesa do emulador, documento
sintético e estados injetados; não demonstram captura física, OCR real ou
escrita de exportação. `export-progress.png` foi recapturada após incluir a
prévia da página real na composição. Home e opções de exportação foram
recapturadas após a revisão de composição; as demais capturas vêm da fatia
anterior do mesmo runtime com as poses aprovadas.

O [clipe](mascot-motion-v1.mp4) reúne 64 frames renderizados no Android com o
clock de teste em passos de 125 ms e codificados em MP4 a 8 fps. Sequência:
repouso → trabalho → sucesso → atenção. Não é `adb screenrecord` em tempo real,
nem prova de transição da navegação; permite inspeção de continuidade visual.

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
quando a biblioteca é curta; as opções de exportação conservam controles de
formulário e o progresso ainda tem espaço inferior pouco usado;
o ícone vetorial usa linguagem mais plana que a mascote; o movimento atual é
discreto e não articula membros. A inspeção manual de transições, recorte com
câmera real, tema escuro, fonte 200%, RTL e TalkBack também permanece pendente.
As capturas de tema escuro/fonte 200% são amostras, não QA humano completo.
As diferenças voluntárias em relação às POCs Pipa/Lume estão registradas no
[mapa de referências](../REFERENCE_MAP.md); nenhum dos HTMLs ou assets delas
foi incorporado ao produto.
