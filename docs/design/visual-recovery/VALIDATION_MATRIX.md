# Matriz de qualificação da recuperação

| Requisito | Código/arte | Prova funcional | Captura/clipe | Aceite | Estado |
|---|---|---|---|---|---|
| Direção própria e master da raposa | proposta em `ART_DIRECTION.md`; arte nova pendente | inspeção de continuidade pendente | baseline rejeitado em `../evidence/` | proprietário | PENDING |
| Rig sem pops/cortes | atlas antigo rejeitado | teste de pixels antigo não mede qualidade | clipe Android novo pendente | proprietário | PENDING |
| Home/Export e demais telas | versão 42a494b como AS-IS | captura antiga de Composables | comparação nova pendente | proprietário | PENDING |
| Crop sequencial e pixels exportados | `CropScreen`, `QuadEditorOverlay`, `CropExportIntegrationTest` | API 34 passou no ciclo anterior; revalidar após mudança | `../evidence/crop.png` baseline | teste/inspeção | PRESERVAR |
| Export parcial de histórico | `ExportViewModelTest` | 3 testes unitários passaram no ciclo anterior | `../evidence/export-library-warning.png` baseline | teste/inspeção | PRESERVAR |
| UI 200%, RTL, landscape, compacto | injeção de density/RTL existente | API 34 parcial | baseline `../evidence/` | QA humano | PARTIAL |
| Instrumentação API 36, lint e Python | workflow e AGENTS.md | CI do HEAD final pendente | relatórios pendentes | gates | PENDING |
| Tag/release v0.4.1 | manifesto é intenção | aprovação visual expressa ainda não existe | pacote para proprietário pendente | proprietário | BLOCKED |

Após cada fatia: usar fonte atual, teste de risco, captura Android e comparação
com a referência no mesmo estado; marcar **executado**, **injetado**, **não
executado** e **falhou** com precisão. Uma tela bonita isolada não valida um
fluxo e um teste verde não aprova estética.
