# Relatório de gate · 23/10/2026

## Resultado

**Não há fonte artística editável aprovada nem novo motion integrado.** O aplicativo mantém os PNGs de poses aprovados, o botão primário de 17 sp e o comportamento funcional anterior. A criação da mascote está bloqueada **antes** de Kotlin/Compose porque as tentativas de fonte não reconstruíram a anatomia do master. Os atlas e cels deste laboratório não são assets finais e não foram copiados para `res/drawable-nodpi`.

## O que foi observado

| Evidência | Resultado | Limite da conclusão |
|---|---|---|
| Master e poses atuais em 72, 112, 168 e 240 dp no [laboratório](index.html) | Referência estática legível em HTML; [capturas Android anteriores](../motion-final/review.html) disponíveis | Não é motion articulado nem inspeção física |
| Atlas 1/2 de geração conjunta a partir do master | FAIL: torso com patas e peças dianteiras duplicadas; transparência residual | Inapto para separar ou animar sem arte nova |
| Quatro cels derivados do master | FAIL: saudação com pata adicional e mudança de personagem | Inapto para animação quadro a quadro |
| Duas referências recebidas do usuário | Lidas como direção de expressão, scan e organização | Nenhuma é aceite do novo rig ou fonte editável |
| Atlas 3/4 gerados com master + referência recebida | FAIL: duplicação no 3; no 4, braços avulsos longos, ombros abertos e resíduos alfa | Inapto para montagem neutra ou extremos |
| `exportLargeFontDisplaysDecodedCoverAndMascot` no emulador API 34 | PASS: capa decodificada e exibida; mascote presente em 200% escuro | Não verifica beleza/anatomia da raposa |
| `reviewDisplaysDecodedPageInCurrentViewport` no emulador API 34 | PASS: página decodificada e exibida no viewport da cena injetada | Não cobre crop real nem qualidade estética |
| `captureScreenMatrixForManualReview` | Gera arquivos PNG não vazios | Screenshot produzida não é aprovação visual |
| Testes instrumentados completos no emulador API 34 | 23 métodos passaram | Não incluem novo motion, aparelho físico ou fluxo de câmera real |
| Gates locais Gradle e Python | `testDebugUnitTest lint check assembleDebug assembleRelease assembleDebugAndroidTest` exit 0; localização, branding, consistência, site e 15 testes Python exit 0 | Não qualificam anatomia nem substituem CI remota |

O [audit da Assertiva](README.md) motivou a separação entre prova de carregamento/visibilidade e aprovação artística. A execução da Assertiva anterior tinha 7 achados avaliados e não executou Gradle no ambiente isolado dela; os comandos locais acima foram executados fora da ferramenta. Não há uma nova auditoria da Assertiva nesta etapa.

## Saída exigida para continuar

1. Criar no mesmo projeto artístico uma cabeça, torso, cauda, cachecol, olhos, duas patas dianteiras curtas, patas traseiras e objetos com pintura escondida e sobreposições reais. Pivôs de planejamento estão no [README](README.md), mas só devem ser fixados após existir arte compatível.
2. Reconstruir a pose neutra sem diferenças anatômicas; mostrar todos os gestos/extremos em quatro escalas, temas claro/escuro e estados de tela. Se escolher cels, cada quadro deve conservar as quatro patas e a mesma identidade.
3. Só então integrar Compose, testar movimento independente em regiões fora dos olhos, lifecycle/reduced motion e gravar vídeos Android novos. Testes verdes e diferença de pixels não dispensam inspeção artística.

**Release v0.4.1:** bloqueada. `release/manifest.json` mantém `publish: false`; nenhuma tag ou release é produzida por este experimento.
