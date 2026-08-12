# Changelog

Este projeto segue Semantic Versioning e recomenda Conventional Commits no fluxo de contribuição.

## [Unreleased]

## [0.2.8] - 2026-08-12

### Added

- Interface completa em English, Português do Brasil, Español, Français e Italiano, com idioma automático e seletor oficial por aplicativo.
- Novas ilustrações otimizadas da raposa para boas-vindas, processamento local e sucesso.
- Gate de CI que verifica paridade de chaves, placeholders e traduções vazias nos nove catálogos de recursos.

### Changed

- Novo design system claro/escuro em creme, laranja, azul-marinho e teal, inspirado no conceito visual da release.
- Home passou a destacar a raposa e uma única ação principal, com alternativas e recentes compactos.
- OCR abre em texto contínuo, usa estado de processamento dedicado e prioriza a cópia com poucos toques.
- Exportação separa configuração, processamento e sucesso; opções de PDF e imagem aparecem apenas quando relevantes.
- Datas, horários, modos, filtros e qualidades agora respeitam o locale, sem texto de domínio fixo no Kotlin.
- Orientação deixou de ser bloqueada e o conteúdo ganhou limites adaptativos, edge-to-edge e alvos de toque maiores.

### Accessibility

- Hierarquia tipográfica maior, componentes com mínimo de 48 dp, estados selecionados sem depender apenas de cor e movimento leve que segue a escala de animação do sistema.
- Plurais reais substituem padrões como `página(s)` nos fluxos de revisão, OCR e exportação.

### Fixed

- Resultado da exportação não aparece mais misturado às opções de configuração.
- Ações de exportação ficam indisponíveis para lotes sem páginas.

### Testing

- Compilação Kotlin e lint Android validados com os cinco idiomas e o novo LocaleConfig automático.
- Versão alinhada para `0.2.8` com `versionCode` 15.

## [0.2.7] - 2026-08-12

### Security

- Cloud backup e device transfer automáticos foram desativados com regras explícitas para arquivos, banco, preferências e armazenamento externo do app.
- FileProvider passou a expor somente subdiretórios de export e compartilhamento concede apenas leitura temporária com `ClipData`.
- Deleção de página/lote agora remove arquivos privados gerenciados sem apagar URIs externas nem exports do usuário.

### Fixed

- Room deixou de usar migration destrutiva; o schema 1 passou a ser exportado e versionado sem bump artificial.
- Importação parcial preserva ordem, informa contagens e faz rollback das cópias se Room falhar.
- Derivado ausente ou ilegível usa a fonte canônica e não mantém spinner infinito.
- Captura manual bloqueia taps concorrentes e trata falha de binding sem bloquear a main thread.
- PDF/JPG/PNG falham com página identificada em vez de reportar sucesso com conteúdo omitido.
- Worker deixou de limpar um diretório de export inexistente e passou a atuar apenas em fontes órfãs, derivados e temporários gerenciados.

### Changed

- `ScanFileStore` centraliza importação, ownership, rollback, deleção e orphan cleanup.
- Criação de lote saiu do filesystem do NavHost e usa resultado explícito de sucesso/falha.
- CI executa `check`, builds debug/release e compila os testes instrumentados; wrapper validation, CodeQL e Dependabot foram adicionados.
- Versão do app foi alinhada para `0.2.7` com `versionCode` 14.

### Testing

- Cobertura JVM para ownership, path traversal, importação parcial, rollback, orphan cleanup e gate de captura.
- Testes instrumentados para CRUD/cascade/ordem Room, deleção física e exportação de página ilegível.

### Documentation

- Estado atual, lifecycle de dados, threat model, checklist de release, política de privacidade e roadmap foram alinhados à base real.

## [0.2.6] - 2026-04-25

### Added

- Modelo puro de chave do pipeline em `core-common`, com versão de algoritmo, finalidade, fonte, crop, rotação, filtro e tamanho de saída.
- Testes unitários para normalização de rotação/crop, seleção de derivado, invalidação de imagem processada e chaves de preview/OCR/exportação.

### Changed

- Pipeline local agora aplica crop/perspectiva a partir do `sourceUri` canônico, depois rotação do usuário e só então filtro ou preparação de OCR.
- Exportação deixou de usar `displayUri` como fonte final e passa a rederivar PDF/JPG/PNG da página canônica quando existe crop, rotação, filtro ou cache processado.
- OCR continua usando a saída textual da `v0.2.5`, mas agora recebe a mesma geometria lógica da página aprovada.
- Revisão passa a preparar uma prévia canônica da página selecionada, sem confiar cegamente em `processedUri` antigo.
- Imagens de documento em componentes compartilhados usam `ContentScale.Fit` por padrão para evitar percepção de zoom/corte.
- Versão do app foi alinhada para `0.2.6` com `versionCode` 13.

### Fixed

- Risco de exportar arquivo diferente da revisão por uso de `processedUri` obsoleto.
- Risco de OCR/exportação aplicar crop implícito quando a página não tinha crop salvo, especialmente em resultados já tratados pelo scanner rápido.
- Race em que o usuário podia sair do crop antes de persistir o quadrilátero manual.
- Cache visual da UI sem carimbo de arquivo local, que podia manter bitmap antigo no mesmo URI.

## [0.2.5] - 2026-04-25

### Added

- Pós-processamento de OCR para ordenar linhas por posição visual, agrupar parágrafos e reduzir ruídos pequenos.
- Modelo de OCR mais rico com texto consolidado, parágrafos, qualidade simples da leitura e contagem de ruído descartado.
- Visualização alternável entre `Trechos` e `Texto contínuo` na tela de OCR.
- Testes unitários para ordenação, agrupamento, remoção de ruído e geração de texto consolidado.

### Changed

- `Copiar tudo` agora usa a saída consolidada pós-processada, não os blocos crus do ML Kit.
- Tela de OCR ficou mais minimalista, com preview menor, estados mais curtos e cópia por trecho.
- OCR local preserva bounding boxes do ML Kit para ordenar e agrupar a leitura de forma mais natural.
- Revisão mostra uma indicação discreta quando a página já tem OCR salvo.
- Documentação, site e versão do app foram alinhados para `0.2.5` e AGP `9.1.1`.

### Fixed

- Resultado de OCR manual/importado fragmentado em muitos blocos ou palavras isoladas.
- Saída de cópia com quebras artificiais, ordem visual ruim ou ruído claramente inútil.
- Referências públicas antigas à versão anterior e à versão do Android Gradle Plugin.

## [0.2.4] - 2026-04-25

### Added

- Assets da raposa mascote para onboarding e identidade visual do app.
- Novo ícone do launcher com a raposa escaneando documento.
- Onboarding visual em 3 telas: escanear rapidamente, revisar/ajustar e privacidade por padrão.

### Changed

- Home foi simplificada para colocar `Escanear` como CTA principal absoluto.
- A escolha antecipada entre documento, recibo e caderno saiu da Home; o scanner rápido agora usa um modo universal.
- Ajuste manual e importação direta ficaram como fallback secundário, preservando a preferência de modo manual nas configurações.
- Splash, onboarding e textos públicos foram alinhados à identidade da raposa e à versão `0.2.4`.

### Fixed

- Atrito visual causado por cards longos, textos explicativos e opções concorrendo com o scanner rápido na Home.
- Referências públicas defasadas à versão `0.2.3` e ao visual antigo.

## [0.2.3] - 2026-04-24

### Added

- Resultado de OCR estruturado em blocos e linhas, preservando o texto completo salvo por pagina.
- Acao `Copiar bloco` na tela de OCR alem do `Copiar tudo`.
- Pos-exportacao por arquivo com nome, tipo, tamanho, local salvo, caminho quando disponivel, abrir e compartilhar.

### Changed

- Tela de OCR foi refeita para leitura por blocos, com hierarquia mais limpa e acao principal fixa no rodape.
- Exportacao passou a escolher primeiro entre `PDF` e `Imagem`, mostrando qualidade apenas para PDF e JPG/PNG apenas no fluxo de imagem.
- Revisao deixou `Exportar lote` como CTA principal e reduziu textos/blocos secundarios.
- Documentacao publica e arquivos de versao foram alinhados para `0.2.3`.

### Fixed

- Sensacao de texto OCR jogado na tela mesmo quando a engine reconhecia estrutura util.
- Poluicao visual causada por opcoes simultaneas de exportacao.
- Falta de clareza no estado final sobre arquivo salvo, formato, tamanho, local e proximas acoes.

## [0.2.2] - 2026-04-24

### Added

- `escopo.md` na raiz como referência principal de produto para a fase atual.
- Cópia estável das imagens de entrada para armazenamento interno antes de criar lotes locais.
- Feedback curto ao concluir exportação e ao copiar texto do OCR.

### Changed

- Home passou a concentrar o topo em `Escanear rápido`, mantendo manual/importação direta em uma área secundária de ajuste manual.
- Fluxo manual deixou de oferecer atalho concorrente para o scanner guiado dentro da tela de CameraX.
- Lotes criados pela entrada rápida, câmera manual e importação direta agora recebem títulos com origem clara.
- OCR foi reorganizado com `Copiar tudo` no rodapé e blocos de texto separados para leitura.
- Documentação pública foi alinhada à decisão de manter o scanner do Google como fluxo principal.

### Fixed

- Dependência de URIs temporárias retornadas por scanner, galeria ou câmera antes de preview, filtros, OCR e exportação.
- Sensação de que o fluxo manual competia com o caminho rápido na Home.
- Falta de feedback curto após exportar arquivos ou copiar OCR.

## [0.2.1] - 2026-04-23

### Added

- Botão de reajuste automático no crop manual, mantendo o editor como fallback confortável quando o primeiro palpite não basta.
- Overlay de corte com área externa suavemente escurecida, guias durante o arraste e handles maiores para ajuste mais preciso.

### Changed

- Heurística de auto crop do fluxo manual/importado passou a reavaliar múltiplos candidatos, com score por contraste, borda, centralidade e geometria antes de escolher o quadrilátero inicial.
- Filtros locais foram recalibrados outra vez para documento real, com menos branco estourado, menos cinza metálico e melhor preservação de miolo manuscrito e pautado.
- Revisão do lote adotou disclosure progressivo para nome e tags, reduzindo peso visual e mantendo foco em páginas e ações principais.
- Exportação foi simplificada para mostrar primeiro o formato, depois as opções dependentes dele, com CTA mais direta e sucesso mais fácil de entender.
- Tela de OCR passou a priorizar leitura contínua e cópia rápida em vez de espalhar o texto em vários blocos pesados.

### Fixed

- Palpites iniciais excessivamente abertos ou pouco confiáveis em fotos inclinadas, importações da galeria e fundos mais poluídos.
- Sensação de editor carregado demais por texto, cards longos e metadados sempre expostos na revisão.
- Fluxo de exportação com opções demais ao mesmo tempo e pouca clareza entre gerar, abrir e compartilhar.
- Casos em que a prévia de filtro ficava mais fraca do que precisava no uso real, especialmente em documentos de estudo e caderno.

## [0.2.0] - 2026-04-23

### Added

- Saída dedicada para OCR, com imagem preparada especificamente para leitura antes do reconhecimento local.
- Metadados de pós-exportação com nome, tipo, tamanho, local salvo e ação direta para abrir o arquivo.
- Cache leve em memória para previews e thumbnails, reduzindo decodes repetidos nas telas principais.

### Changed

- Heurística inicial de crop do fluxo manual/importado foi refeita para estimar um quadrilátero mais útil a partir de bordas, brilho e amostras laterais da página.
- Pipeline dos filtros foi recalibrada para separar melhor correção geométrica, normalização de iluminação, contraste e binarização.
- Tela de revisão foi simplificada para dar foco na página selecionada, nas ações rápidas e na organização do lote.
- Tela de exportação passou a priorizar escolha de formato, geração clara do arquivo e leitura do resultado salvo no aparelho.
- Tela de OCR foi reorganizada com preview da entrada preparada, blocos mais legíveis e ação de cópia mais evidente.
- Home reforça o scanner rápido como caminho recomendado, mantendo modo manual e galeria como fallback.

### Fixed

- Build quebrado por desalinhamento entre contratos de exportação e implementação do `DocumentProcessingRepository`.
- Exportações em Android 10+ agora vão para `Downloads/Scanora`, deixando o local do arquivo mais previsível.
- Filtros que lavavam texto, estouravam o papel ou deixavam o cinza artificial em vários casos de documento real.
- Fluxo de OCR dependente demais do visual salvo da página, o que piorava leitura quando o filtro escolhido não era adequado.
- Ruído visual na revisão e no pós-export que deixava o fluxo mais pesado do que o necessário.

## [0.1.5] - 2026-04-22

### Added

- Scanner rápido com `ML Kit Document Scanner` promovido a CTA principal na Home.
- Pipeline de prévia em duas etapas no editor, com render inicial rápido e refinamento assíncrono em background.

### Changed

- Sugestão inicial de quadrilátero do fluxo manual foi fortalecida para imagens importadas e capturas mais tortas.
- Filtros locais foram recalibrados para reduzir página lavada, texto apagado e diferenças pouco úteis entre presets.
- Tela de revisão foi reorganizada para dar mais foco em lote, páginas e ações principais.
- Tela de exportação passou a usar hierarquia mais direta, com navegação clara, CTA fixa e compartilhamento melhor integrado.
- Tela de OCR ficou mais legível e o processamento local passou a usar uma imagem melhor preparada para reconhecimento.

### Fixed

- Risco de recalcular a mesma prévia de filtro em recomposições sucessivas ou trocas rápidas de opção.
- Sensação de fluxo quebrado entre scanner guiado, revisão e histórico local.
- Estado antigo de arquivos exportados permanecendo visível depois de trocar formato ou qualidade.

## [0.1.4] - 2026-04-22

### Added

- Prévia leve e assíncrona para filtros, usando bitmap reduzido antes da aplicação final.
- Estado dedicado de prévia no editor para separar seleção visual de processamento definitivo.

### Changed

- `CropScreen` agora mostra a imagem original inteira com `ContentScale.Fit` e overlay alinhado à área real do preview.
- `FilterScreen` foi reorganizada com `Scaffold`, `TopAppBar` com voltar, preview central mais limpo e CTA fixa no rodapé.
- Pipeline local de detecção inicial de cantos foi reforçada com suavização de luma, projeção de bordas e limites mais estáveis.
- Carregamento de imagem assíncrono passou a usar decode em background e amostragem menor para reduzir custo visual.

### Fixed

- Ajuste manual dos quatro pontos deixou de usar coordenadas desalinhadas com o preview e ficou mais responsivo ao toque.
- Handles do crop ganharam área de toque maior e perderam a sensação de arrasto preso durante o gesto.
- Risco de ANR na tela de filtros foi reduzido ao evitar decode pesado e processamento síncrono na main thread.
- Fluxo do editor ficou mais direto, com menos texto e com ações principais mais claras.

## [0.1.3] - 2026-04-22

### Added

- Novo conjunto de componentes visuais compartilhados em `core-ui` para seções, opções e hierarquia de conteúdo.
- Hero mais forte na Home, novas superfícies de detalhe e OCR mais legível com prévia da página.

### Changed

- Tema visual refinado com paleta, formas e tipografia mais coerentes com a proposta de scanner de documentos.
- Site público em `site/` redesenhado para ficar mais leve, mais escaneável e mais próximo de uma página de produto real.
- README reorganizado com topo mais enxuto, seção objetiva do que o app já faz e melhor apresentação do CI.

### Fixed

- Microcopy PT-BR revisada nas telas principais para reduzir tom genérico e melhorar clareza das chamadas para ação.
- Estados vazios, cards de histórico, seleção de opções e revisão de lote com melhor ritmo visual e consistência.

## [0.1.2] - 2026-04-22

### Added

- Estrutura `planning/` para documentos de escopo e planejamento.
- Diretório `site/` dedicado ao GitHub Pages, separado da documentação técnica.

### Changed

- Rename completo de package, namespace e `applicationId` para `com.soturine.scanora`.
- README, site e workflows foram revisados para deixar a apresentação pública mais direta e consistente.
- `docs/` passou a conter apenas documentação técnica em Markdown.

### Fixed

- Links e templates que ainda apontavam para placeholders antigos ou para caminhos legados do site.
- Workflow do GitHub Pages agora publica `site/` em vez de `docs/`.
- Referências internas do projeto foram alinhadas ao package real sem quebrar imports.

### Planned

- Substituir mockups e placeholders por capturas reais do app em emulador ou dispositivo.
- Refinar o material de release e o checklist para publicação 1.0.

## [0.1.1] - 2026-04-21

### Added

- Validação local do ambiente com Android SDK Platform 36, Build Tools 36.0.0 e build de linha de comando.
- Feedback visual mais claro na exportação com resumo do lote e indicador de progresso local.

### Changed

- Tela de revisão agora salva nome e tags sob ação explícita, em vez de persistir a cada tecla.
- Botões de seleção e reordenação de páginas passaram a refletir melhor o estado atual do lote.
- Documentação de setup e testes foi atualizada com o fluxo realmente validado nesta máquina.

### Fixed

- Redução de escritas redundantes no Room ao renomear documentos e atualizar tags sem alterações reais.
- Exibição de mensagens de erro na revisão, cobrindo validação de nome e falhas de persistência.
- Ajustes de build para a linha AGP 9.1.x, Kotlin 2.3.10 e dependências necessárias para compilar, lintar e testar localmente.

### Planned

- Rodar revisão visual em emulador ou dispositivos reais para validar espaçamento, responsividade e estados extremos.
- Ampliar cobertura de testes instrumentados para fluxos de revisão, OCR e exportação.

## [0.1.0] - 2026-04-21

### Added

- Base Android multimódulo com `app`, `core-*` e `feature-*`.
- Splash, onboarding, home, captura, editor, exportação, histórico, detalhe, OCR, configurações e sobre.
- Captura manual com `CameraX`.
- Scanner guiado com `ML Kit Document Scanner`.
- OCR local com `ML Kit Text Recognition`.
- Persistência local com `Room`.
- Preferências locais com `DataStore`.
- Exportação de PDF, JPG e PNG com compartilhamento por `FileProvider`.
- Histórico local com busca por título, tags e favoritos.
- Tema claro/escuro e componentes reutilizáveis em Compose.
- Site estático inicial para GitHub Pages.
- CI inicial para build, lint e testes unitários.
- Documentação inicial completa para setup, arquitetura, publicação e testes.

### Changed

- Escolha de pipeline híbrido: captura manual própria e scanner guiado complementar para acelerar o MVP.
- Coordenadas de crop normalizadas para simplificar preview, edição manual e processamento.

### Fixed

- Sanitização básica de nomes de documentos.
- Padronização inicial de datas exibidas em pt-BR.
- Geração previsível de nomes de arquivo para exportação.

### Planned

- Melhorar detecção de quadriláteros em imagens importadas.
- Refinar crop manual com snapping e validação visual.
- Adicionar mais testes instrumentados e cobertura de repositórios.
- Evoluir tratamento de erros e acessibilidade para release pública.
