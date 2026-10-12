# Redesign: implementação e evidência

## Como ler esta evidência

Capturas em [evidence/](evidence/) feitas por `RebrandVisualTest` no Android 14
(API 34), emulador Pixel 7, 1080×2400. São componentes reais do APK, com documento
sintético e estados injetados. Não são mockups HTML nem prova de navegação completa,
OCR reconhecido ou exportação concluída. A captura `launcher.png` veio do launcher
do Android com o APK instalado. Catálogo usado: português do Brasil.

Os testes instrumentais de `core-data` verificam separadamente persistência,
migrations, rollback e pixels do pipeline real. A automação local não substitui
o gate de API 36 no GitHub nem QA físico. Nenhum documento pessoal foi publicado.

## Matriz de telas e estados

| Superfície | Antes → implementação | Evidência / limite |
|---|---|---|
| Splash/launcher | bitmap ampliado → símbolo vetorial com rosto e folha; monochrome | `launcher.png`, `launcher-masks.png`; círculo e quadrado arredondado, 48/72/108dp; demais OEMs pendentes |
| Onboarding 1–3 | grandes PNGs → cenas por partes, voltar/pular, conteúdo rolável | `onboarding*.png`; animação aqui é demonstração educativa |
| Home vazia/recentes | leitura direta de PNG → componente único, ação principal e papel | `home-empty.png`, `home-recent.png`, `home-dark.png` |
| Importação/falha/fallback | mesmos contratos, novo tema e mascote de trabalho | estado governa a cena; importação real/câmera física pendentes |
| Scanner | ML Kit permanece proprietário da UI de captura | não imitar, redesenhar ou prometer controle sobre a tela do Google |
| Revisão | documento destacado, ferramentas rotuladas, superfícies novas | `review.png`, `review-dark-200-rtl.png`; painel lateral preservado |
| Crop | seleção mais contrastante, fundo escuro distingue letterbox | `crop.png`; callback atualizado preserva cantos sequenciais; fonte canônica intacta |
| Filtros | comparar ao segurar, restaurar ao soltar, feedback no botão | `filters.png`; teste verifica troca e restauração; TalkBack alterna pela ação semântica |
| OCR carregando/texto/vazio | leitura em papel, rig durante reconhecimento, vazio em repouso | `ocr-loading.png`, `ocr-ready.png`, `ocr-empty.png`; texto da captura é fixture |
| Export PDF/JPG/PNG | formatos visíveis, nome validado, opções dependentes | `export.png`, `export-jpg.png`, `export-png.png`; opções de PDF continuam progressivas |
| Export em progresso | opções antes continuavam visíveis → cena de trabalho e indicador indeterminado | `export-progress.png`; sem percentual inventado; seleção e voltar bloqueados durante gravação |
| Export erro/sucesso | snackbar efêmero → erro persistente; resultado com nomes de arquivo | `export-error.png`, `export-success.png`; sucesso vem do domínio, não do relógio da animação |
| Export salvo / histórico indisponível | antes podia alegar rollback → arquivos continuam acessíveis e aviso específico | `export-library-warning.png`; regressão unitária com falha após escrita |
| Histórico/busca | linhas em papel, espaçamento e rótulo de busca | `history.png`, `search-empty.png`; FTS e contratos preservados |
| Detalhe | componentes/cores compartilhados e confirmações existentes | `detail.png`; operações destrutivas continuam confirmadas |
| Configurações | grupos de aparência/exportação/ajuda com superfícies próprias | `settings.png`; preferências e autônimos mantidos |
| Idioma | diálogo nativo Material com lista rolável e seleção | `language.png`; 12 catálogos completos, sem revisão linguística nativa |
| Sobre/privacidade | novo tema/tipografia; conteúdo legal e link preservados | `about.png`; página de privacidade é externa, não uma tela simulada |
| Diálogos/ausência de documento | preservados componentes Material e recuperação existentes | herdam tokens; nenhum novo fluxo de exclusão ou permissão |

## Motion e acessibilidade

- `motion-frame-0.png` e `motion-frame-1.png`: frames distintos do mesmo componente
  Android durante Working; teste compara pixels após avançar 750ms do relógio Compose.
- `reducedMotion=true`: pixels permanecem iguais após 1000ms. O código também observa
  a escala de animação do Android e pausa fora de RESUMED; essas duas integrações
  precisam de validação manual complementar. Sucesso/erro são finitos.
- Atlas e pivôs editáveis documentados em [mascot.md](mascot.md). Sem runtime novo.
  Rive/Lottie não foram adicionados sem composição válida. Revisão estética final
  do proprietário permanece pendente, especialmente após o feedback sobre a raposa.
- Contraste calculado: branco/terracota 5,68:1; tinta/marfim 11,04:1; corpo/papel
  6,15:1; texto do botão escuro 9,38:1. Não é certificação de todas as combinações.
- Export em tema escuro com fonte 200%: `export-dark-200.png`; revisão também usa
  direção RTL injetada. Isso não substitui teste em árabe nem TalkBack humano.
- Layout largo renderizado a 1920×1080, densidade 240 (1280dp de largura):
  `review-wide.png`, `home-recent-wide.png`, `export-dark-200-wide.png`.
  O painel lateral da revisão está visível; a matriz passou nessa configuração.
- Alças do crop têm ações semânticas e área de toque de 64dp; coordenadas físicas
  não espelham a imagem. Conteúdo decorativo da raposa não repete anúncios de estado.
- Pressão do botão principal escala para 98,5% por 120ms. Não governa persistência.

## Verificações executadas antes da qualificação final

- Build focado `:app:assembleDebug :app:assembleDebugAndroidTest`: passou.
- Instrumentação direta `RebrandVisualTest`: 5 testes passaram em API 34, incluindo
  dois cantos sequenciais, segurar comparação, frames/redução, telas e máscaras.
- Instrumentação `core-data`: 14 testes passaram em API 34. O teste de extração
  de texto de PDF depende de API 35+ e retorna cedo nesse aparelho; não foi exercido.
- `CropExportIntegrationTest`: render de metade de fixture bicolor, rotação 90°,
  dimensões/pixels do PNG exportado e preservação da fonte; reset para página inteira.
- Gates de localização e branding passaram durante os checkpoints. Resultado
  consolidado, SHA final e artefato pertencem ao relatório de release, não a este snapshot.
- Três regressões unitárias de ExportViewModel passaram: falha de escrita não gera
  sucesso, falha posterior do histórico preserva acesso aos arquivos e operação ativa
  congela opções/rejeita clique duplicado. Nenhuma fonte privada é renomeada.

## Limites e trabalho posterior

QA de câmera física, scanner do Google, TalkBack, línguas nativas, corpus real,
upgrade em aparelho e leitores externos de PDF continuam pendentes. Não há
medição de jank/RAM/cold start nem alegação de prontidão para Play Store.
Exportação permanece no destino aberto até terminar, sem cancelamento interativo
novo e sem trabalho durável garantido após encerramento do processo.

Foram removidos 12 PNGs estáticos de runtime sem callers. O conceito inicial
rejeitado fica apenas como registro em docs. Modelos, Room, origem privada,
ML Kit, políticas de backup e FileProvider foram preservados.
