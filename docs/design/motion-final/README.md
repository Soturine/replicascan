# Estudo de movimento rejeitado — 23/10/2026

O rig experimental em camadas criado em 15/10 chegou ao Android, mas o proprietário reprovou o resultado visual: patas dianteiras pequenas e rígidas, cachecol deslocado e composição geral inferior ao [master aprovado](../visual-recovery/candidates/master-v1.png). A tentativa de corrigir apenas escala e posição também foi reprovada. O runtime voltou às poses pintadas aprovadas em `replicascan_fox_*.png`; os PNGs `fox_rig_*.png` foram retirados do APK.

A [comparação nativa em 112 dp, telas de exportação/Home e vídeo do rollback](review.html) mostra o APK atual ao lado de um quadro do rig rejeitado. O [manifesto](rig/manifest.json) e a [prévia](rig/preview.html) permanecem como registro **rejeitado**, não como referência de implementação. Os demais clipes e capturas dessa tentativa foram preservados fora do repositório em `C:\Users\rafael\.codex\scratch\replicascan-motion-20261015\rejected-evidence`, para diagnóstico.

As novas separações automáticas testadas em 23/10 também mudaram a escala e a anatomia dos membros. Não foram integradas. Para retomar a animação articulada, o rig precisa manter exatamente as proporções do master, com torso, cabeça/cachecol, cauda, cada pata e documento em fontes coerentes e pivôs verificáveis. Cada estado e extremo de movimento deve ser inspecionado em 72, 112, 168 e 240 dp, temas claro/escuro e no APK antes de voltar ao runtime.

O texto do botão primário em 17sp foi mantido. A cena de OCR/exportação mantém os olhos abertos enquanto a pintura de pálpebras não alinhar em tamanhos pequenos. Isso é contenção visual, **não** entrega do motion definitivo. A tag e a release v0.4.1 seguem bloqueadas pelo rig, pelos vídeos Android finais, pelo QA físico e pelos demais critérios do prompt.
