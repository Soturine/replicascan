# Ícones candidatos — checkpoint de exploração

`STATUS: CANDIDATOS, NENHUM ÍCONE APROVADO` (2026-10-13).

[Compare as quatro capturas nativas](index.html). Os PNGs originais foram gerados
tomando o [master aprovado](../candidates/master-v1.png) como identidade da
raposa, sem copiar os conceitos A/B rejeitados. Cada candidato tem apenas duas
patas dianteiras segurando uma folha. As artes permanecem em `docs/` e nos
assets de `androidTest`; o launcher de produção não foi alterado.

| Opção | Leitura observada no Pixel 7 / API 34 |
|---|---|
| 1 | Rosto e sorriso claros em 24 dp; linha terracota associa a folha ao scanner. Melhor ponto de partida. |
| 2 | Rosto claro, folha e traço petróleo mais presentes; compete um pouco mais com o lenço. |
| 3 | Folha forte, mas rosto menor demais em 24 dp. Revisar proporção antes de considerar. |
| 4 | Rosto da 2 com folha intermediária e sinais de escaneamento; capturado em 24–108 dp, máscaras e rascunho temático. Aguarda revisão do proprietário. |

O teste [`IconCandidateTest`](../../../../app/src/androidTest/java/com/soturine/replicascan/IconCandidateTest.kt)
renderiza 24/48/72/108 dp com `AdaptiveIconDrawable` e a máscara real do
emulador. Também desenha prévias de círculo, squircle e gota. Essas três
prévias são aproximações de máscaras de outros launchers, não prova em OEMs.
O ícone temático mostrado nas pranchas é um vetor candidato separado, ainda
sem escolha artística; por isso não substitui `ic_launcher_monochrome.xml`.
Faltam instalação no launcher, teste físico, escolha do proprietário e
ajuste final das camadas adaptativas.
