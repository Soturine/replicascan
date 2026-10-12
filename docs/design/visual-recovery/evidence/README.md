# Capturas Android da recuperação visual

`RebrandVisualTest` renderizou os componentes Compose no emulador Pixel 7,
Android 14 / API 34, com APK debug da v0.4.1 em desenvolvimento. As capturas
medem 1080×2201 px, usam a localização inglesa do emulador, documento
sintético e estados injetados; não demonstram captura física, OCR real ou
escrita de exportação. `export-progress.png` foi recapturada após incluir a
prévia da página real na composição; as demais capturas vieram da mesma fatia
de runtime com as poses aprovadas.

O [clipe](mascot-motion-v1.mp4) reúne 64 frames renderizados no Android com o
clock de teste em passos de 125 ms e codificados em MP4 a 8 fps. Sequência:
repouso → trabalho → sucesso → atenção. Não é `adb screenrecord` em tempo real,
nem prova de transição da navegação; permite inspeção de continuidade visual.

Master e poses estáticas receberam aprovação do proprietário. Movimento,
composição das telas e release ainda precisam de aceite expresso. O antigo
`docs/design/evidence/` permanece como baseline rejeitado.
