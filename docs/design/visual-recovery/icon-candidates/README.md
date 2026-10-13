# Ícones candidatos — checkpoint de exploração

`STATUS: OPÇÃO 4 ESCOLHIDA COMO CANDIDATA FINAL, RELEASE PENDENTE` (2026-10-13).

[Compare as capturas nativas](index.html). Os PNGs originais foram gerados
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
| 6 | Composição nova com folha à esquerda e raposa à direita. Em 24 dp, o rosto perde presença e os cantos do scanner ficam finos. Estudo sem foreground/monochrome instalados. |
| 7 | Rosto dominante com folha pequena à direita. Mais expressão em 24 dp, mas a função de scanner fica pouco nítida. Estudo sem foreground/monochrome instalados. |

O teste [`IconCandidateTest`](../../../../app/src/androidTest/java/com/soturine/replicascan/IconCandidateTest.kt)
renderiza 24/36/48/72/108 dp com `AdaptiveIconDrawable` e a máscara real do
emulador. Também desenha prévias de círculo, squircle, arredondado e gota. Essas quatro
prévias são aproximações de máscaras de outros launchers, não prova em OEMs.
O ícone temático da opção 4 é o recurso `ic_launcher_monochrome.xml` do app;
as cores na prancha são simuladas. [Launcher instalado](launcher-installed.png)
e [máscaras do recurso](launcher-masks.png) foram capturados no emulador API 34.
Faltam teste físico, aceite do conjunto visual e qualificação do HEAD da release.

As opções 6 e 7 foram geradas em PNG transparente a partir do master aprovado.
Uma terceira geração, quase idêntica à opção 4, foi descartada antes da prancha.
Elas oferecem composições distintas para diagnóstico, mas ainda não têm fonte
editável em camadas nem versão monocromática correspondente; por isso não são
substitutas prontas para instalação. A opção 4 continua no launcher até um
novo aceite explícito após comparação.
