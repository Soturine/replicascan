# Matriz de qualificação da recuperação

| Requisito | Código/arte | Prova funcional | Captura/clipe | Aceite | Estado |
|---|---|---|---|---|---|
| Direção e master da raposa | original v0.4.0 e master editado, com seis poses derivadas | personagem consistente na prancha; master e poses aprovados em mensagens explícitas | `candidates/review.png` | proprietário | ARTE ESTÁTICA APROVADA |
| Movimento sem pops/cortes | poses inteiras Compose; atlas 3×3 removido | teste Android verifica movimento e redução; articulação independente não implementada | `evidence/mascot-motion-v1.mp4` e dois frames Android | proprietário | PARCIAL, ACEITE PENDENTE |
| Home/Export e demais telas | nova raposa em todos os callers; progresso inclui página real | cinco testes de UI Android passaram com fixture sintética | capturas atuais em `evidence/`; Review/Crop e demais estados ainda usam baseline anterior | proprietário | PARCIAL, ACEITE PENDENTE |
| Crop sequencial e pixels exportados | `CropScreen`, `QuadEditorOverlay`, `CropExportIntegrationTest` | API 34 passou no ciclo anterior; revalidar após mudança | `../evidence/crop.png` baseline | teste/inspeção | PRESERVAR |
| Export parcial de histórico | `ExportViewModelTest` | 3 testes unitários passaram no ciclo anterior | `../evidence/export-library-warning.png` baseline | teste/inspeção | PRESERVAR |
| UI 200%, RTL, landscape, compacto | injeção de density/RTL existente | API 34 parcial | baseline `../evidence/` | QA humano | PARTIAL |
| Instrumentação API 36, lint e Python | workflow e AGENTS.md | Gradle `testDebugUnitTest lint check assembleDebug assembleRelease assembleDebugAndroidTest` e cinco checks Python passaram após integração; CI API 36 do HEAD final pendente | clipe e capturas locais | gates | LOCAL PASS; CI PENDING |
| Tag/release v0.4.1 | manifesto é intenção | aprovação visual expressa do sistema completo ainda não existe | pacote parcial em `evidence/` | proprietário | BLOCKED |

Após cada fatia: usar fonte atual, teste de risco, captura Android e comparação
com a referência no mesmo estado; marcar **executado**, **injetado**, **não
executado** e **falhou** com precisão. Uma tela bonita isolada não valida um
fluxo e um teste verde não aprova estética.
