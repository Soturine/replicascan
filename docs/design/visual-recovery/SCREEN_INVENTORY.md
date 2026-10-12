# Inventário de rotas, estados e evidência esperada

Fonte: `ReplicaScanNavHost.kt`, Composables `feature-*` e `RebrandVisualTest`.
O arquivo anterior `docs/design/validation.md` contém capturas do baseline
rejeitado; estas não equivalem a aprovação da recuperação.

| Destino/estado | Prioridade | Evidência exigida após alteração |
|---|---|---|
| splash, launcher, themed/monochrome | alta | ícone instalado, máscaras, 24/48/72/108dp |
| onboarding 1, 2, 3; pular/voltar | alta | três capturas e continuidade do personagem |
| Home vazia, recente, ocupada, dark | alta | 360/390dp, dark, fontes grandes |
| handoff ML Kit, cancelamento, fallback e importação | alta funcional | execução real/provider quando possível; nenhum POC como prova |
| Review 1/múltiplas páginas, rotação e ações | alta | telefone e painel largo, página antes/depois |
| Crop inicial, arraste, reset, undo, inválido | bloqueante funcional | captura durante gesto e pixels do export |
| Filtros, comparação, seleção e Apply | média | gesto, TalkBack/foco, preview original |
| OCR loading/ready/empty/error/idioma | alta | capturas e teste de cópia/readiness real |
| Export PDF/JPG/PNG opções | bloqueante visual | preview, nome, qualidade e tamanho, teclado/200% |
| Export running/success/write error/library warning | bloqueante visual/funcional | clipe Android, nomes reais, falhas reais |
| History cheio/vazio/busca; Detail e exclusão | média | densidade, busca, confirmações |
| Settings, idioma, About, privacidade, diálogos | média | 12 locales/RTL, 200%, navegação |

Cada linha deve registrar estado, origem dos dados (real ou injetado), tamanho,
tema, captura, teste funcional e aceite. Render de um Composable com fixture não
verifica navegação/processo/serviço. A UI do scanner é do Google Play services.
