# Matriz de qualificação da recuperação

| Requisito | Código/arte | Prova funcional | Captura/clipe | Aceite | Estado |
|---|---|---|---|---|---|
| Direção e master da raposa | original v0.4.0 e master editado, com seis poses derivadas | personagem consistente na prancha; master e poses aprovados em mensagens explícitas | `candidates/review.png` | proprietário | ARTE ESTÁTICA APROVADA |
| Movimento sem pops/cortes | poses inteiras Compose; atlas 3×3 removido | teste Android verifica movimento e redução; articulação independente não implementada | `evidence/mascot-motion-v1.mp4` e dois frames Android | proprietário | PARCIAL, ACEITE PENDENTE |
| Home/Onboarding/Export e demais telas | Home amplia a mascote e libera largura para texto; introdução integra três poses à folha e compacta a cena em fonte 200%; opções de exportação reúnem prévia/páginas/formato; progresso inclui página real | `MainActivityTest` (4 testes) e `renderScreenMatrix` passaram no emulador API 34 com fixture sintética; splash corrigida após o primeiro teste revelar falha de recurso | `evidence/review.html` compara capturas atuais e baseline; fluxo físico não foi exercitado | proprietário | PARCIAL, ACEITE PENDENTE |
| Ícone adaptativo opção 4 | Arte com duas patas, folha intermediária e cantos/linha de escaneamento; foreground adaptativo e vetor temático no APK debug | `IconCandidateTest` e `launcherMasksAndSizes` passaram no emulador API 34; recurso real conferido em 24–108 dp | `icon-candidates/candidate-4-android.png`, `launcher-installed.png` e `launcher-masks.png` | proprietário | CANDIDATO FINAL ESCOLHIDO, TESTE FÍSICO PENDENTE |
| Ícones alternativos 6/7 | Folha à esquerda ou folha lateral; PNG transparente sem camadas editáveis/monochrome instalados | `IconCandidateTest` passou no emulador API 34 em 24/36/48/72/108 dp e quatro máscaras; ambos perdem clareza de scanner em 24 dp | `icon-candidates/candidate-6-android.png` e `candidate-7-android.png` | proprietário | EXPERIMENTO; 4 PERMANECE INSTALADA |
| Histórico vazio e busca sem resultado | Pose vazia e mensagens no fundo, sem cartão; busca/lista/callbacks mantidos | `:feature-history:testDebugUnitTest`, lint e `renderScreenMatrix` passaram; consulta real não exercitada | `evidence/history-empty.png` e `search-empty.png` | proprietário | CAPTURADO, INTERAÇÃO PENDENTE |
| Crop sequencial e pixels exportados | `CropScreen`, `QuadEditorOverlay`, `CropExportIntegrationTest` | API 34 passou no ciclo anterior; revalidar após mudança | `../evidence/crop.png` baseline | teste/inspeção | PRESERVAR |
| Export parcial de histórico | `ExportViewModelTest` | 3 testes unitários passaram no ciclo anterior | `../evidence/export-library-warning.png` baseline | teste/inspeção | PRESERVAR |
| UI 200%, RTL, landscape, compacto | Home consulta `LocalDensity` efetivo e omite arte com fonte ampliada | API 34: matriz de tela passou; não cobre interações TalkBack | `evidence/home-dark-200.png`, `export-dark-200.png` e `review-dark-200-rtl.png`; largo ainda usa baseline | QA humano | PARCIAL |
| Instrumentação API 36, lint e Python | workflow e AGENTS.md | Gradle `testDebugUnitTest lint check assembleDebug assembleRelease assembleDebugAndroidTest` e cinco checks Python passaram após a revisão de telas; CI API 36 do HEAD final pendente | clipe e capturas locais | gates | LOCAL PASS; CI PENDING |
| Tag/release v0.4.1 | manifesto é intenção | aprovação visual expressa do sistema completo ainda não existe | pacote comparativo e clipe em `evidence/`; QA físico pendente | proprietário | BLOCKED |

Após cada fatia: usar fonte atual, teste de risco, captura Android e comparação
com a referência no mesmo estado; marcar **executado**, **injetado**, **não
executado** e **falhou** com precisão. Uma tela bonita isolada não valida um
fluxo e um teste verde não aprova estética.
