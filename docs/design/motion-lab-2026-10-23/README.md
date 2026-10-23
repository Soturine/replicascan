# Laboratório isolado da raposa — 23/10/2026

**Status: estudo artístico reprovado; zero assets novos integrados ao APK.** A [prancha interativa](index.html) mostra as nove situações previstas com as poses instaladas em 72, 112, 168 e 240 dp e permite examinar os quatro quadros de uma tentativa de saudação. Os arquivos com `rejected` no nome são diagnóstico, não fonte final. Os PNGs usados pelo app continuam em `core-ui/src/main/res/drawable-nodpi/`.

O master aprovado é [`master-v1.png`](../visual-recovery/candidates/master-v1.png). Duas tentativas de um **único atlas** editado a partir dele falharam: a pintura do torso inclui patas dianteiras e o atlas também contém patas avulsas. Montá-las daria membros extras; retirá-las por máscara voltaria ao recorte de pintura achatada já reprovado. A área entre peças tem cor semitransparente, logo nem fornece transparência operacional limpa. A alternativa de quatro cels desenhados a partir do mesmo master também falhou: a saudação introduziu uma pata incompatível, com mudanças de silhueta e registro entre quadros. Nenhum dos estudos representa fonte em camadas editável coerente. Foram produzidos com a ferramenta de geração de imagens a partir do master do projeto; a licença de uso do master continua sendo a do repositório. Não há arquivo `.riv`, Lottie, SVG anatômico ou rig final.

Duas imagens adicionais fornecidas pelo usuário em 23/10 foram examinadas como referências, não como aprovação para substituir o master. `user-atlas-reference.png` sugere uma organização de peças/expressões, mas as poses mudam as proporções e o alfa contém resíduos coloridos. `user-concept-reference.png` comunica scan, leitura, sucesso e expressão, mas não traz separação anatômica editável. Um atlas único gerado a partir **do master e da primeira referência juntos** (`layer-atlas-v3-rejected.png`) ainda duplicou os membros dianteiros. Uma edição do mesmo atlas (`layer-atlas-v4-rejected.png`) pintou o torso sem braços, mas as duas peças dianteiras ficaram compridas e com cortes abertos nos ombros; os encaixes e o alfa não passaram. Essas duas referências e os derivados estão no laboratório apenas para comparação e não entram no APK.

Para uma nova fonte ser admissível, deve existir pintura editável de cauda, torso/pernas traseiras, cada pata dianteira curta, cabeça, olhos/pálpebras, cachecol e objetos **no mesmo arquivo/projeto artístico**, com versões de oclusão e zonas escondidas realmente pintadas. Não basta transformar partes da pintura completa. As coordenadas abaixo são **marcas de planejamento no espaço de 1254 × 1254 px**, não pivôs validados de camadas existentes:

| Peça pretendida | Articulação estimada | Encaixe a pintar e inspecionar |
|---|---:|---|
| Cauda atrás do corpo | (535, 1005) | Raiz contínua atrás do quadril, sem segunda cauda aparente |
| Cabeça | (628, 710) | Pescoço/queixo sobre torso; o cachecol acompanha o pescoço sem flutuar |
| Pata dianteira esquerda | (525, 765) | Ombro escondido sob a bochecha e o cachecol; pata curta como no master |
| Pata dianteira direita | (745, 765) | Mesmo comprimento e volume da esquerda; quatro patas no total |
| Cachecol | (632, 744) | Nó e triângulo pintados sobre pescoço e peito, sem folga no giro |

**Critério de saída do laboratório:** remontagem neutra que corresponda ao master em expressão, tamanho de cabeça, comprimento das patas, cachecol e silhueta; inspeção de todos os estados e extremos em 72/112/168/240 dp; depois vídeo Android de movimento independente e natural. Revisão artística permanece distinta dos testes automatizados. Enquanto isso não existir, a animação Compose nova, os vídeos Android finais e a release v0.4.1 estão bloqueados.

O audit da Assertiva (`C:\Users\rafael\.codex\scratch\assertiva-reports\replicascan\audit.html`) apontou que capturas e mudanças de pixels não qualificam anatomia. Os testes instrumentados agora verificam que capa de exportação e página de Review foram **decodificadas e exibidas**, além de presença da mascote; isso ainda não aprova desenho. O teste de matriz foi renomeado para deixar claro que produz capturas para revisão manual.
