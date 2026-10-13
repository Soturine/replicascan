# Ícones candidatos — checkpoint de exploração

`STATUS: OPÇÃO 4 ESCOLHIDA COMO CANDIDATA FINAL, RELEASE PENDENTE` (2026-10-13).

[Compare as quatro capturas nativas](index.html). Os PNGs originais foram gerados
tomando o [master aprovado](../candidates/master-v1.png) como identidade da
raposa, sem copiar os conceitos A/B rejeitados. Cada candidato tem apenas duas
patas dianteiras segurando uma folha. A opção 4 foi escolhida para refinamento;
seu primeiro APK debug foi instalado no emulador. O app agora usa sua arte como
foreground adaptativo e um vetor temático correspondente, ainda em revisão.

| Opção | Leitura observada no Pixel 7 / API 34 |
|---|---|
| 1 | Rosto e sorriso claros em 24 dp; linha terracota associa a folha ao scanner. Melhor ponto de partida. |
| 2 | Rosto claro, folha e traço petróleo mais presentes; compete um pouco mais com o lenço. |
| 3 | Folha forte, mas rosto menor demais em 24 dp. Revisar proporção antes de considerar. |
| 4 | Rosto da 2 com folha intermediária e sinais de escaneamento; instalada no APK debug e capturada em 24–108 dp, máscaras e rascunho temático. O proprietário escolheu mantê-la como candidata final. |

O teste [`IconCandidateTest`](../../../../app/src/androidTest/java/com/soturine/replicascan/IconCandidateTest.kt)
renderiza 24/48/72/108 dp com `AdaptiveIconDrawable` e a máscara real do
emulador. Também desenha prévias de círculo, squircle e gota. Essas três
prévias são aproximações de máscaras de outros launchers, não prova em OEMs.
O ícone temático da opção 4 é o recurso `ic_launcher_monochrome.xml` do app;
as cores na prancha são simuladas. [Launcher instalado](launcher-installed.png)
e [máscaras do recurso](launcher-masks.png) foram capturados no emulador API 34.
Faltam teste físico, aceite do conjunto visual e qualificação do HEAD da release.
