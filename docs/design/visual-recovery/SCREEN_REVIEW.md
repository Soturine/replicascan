# Revisão das telas — checkpoint de composição

`STATUS: REVISÃO PARCIAL, ACEITE VISUAL PENDENTE` (2026-10-13).

Fonte: [capturas atuais e comparação Android](evidence/review.html), com
documento sintético no emulador API 34. O proprietário aprovou o master e as
poses estáticas da raposa e autorizou continuar a revisão. Isso não aprova
automaticamente todas as telas, o movimento ou a release.

| Destino/estado | Observação e decisão | Estado |
|---|---|---|
| Home com recentes e vazia | Cena nativa de papel e raposa e CTA Escanear de 68 dp. Em fonte 200%, a cena sai para preservar conteúdo e ações. Espaço inferior com poucos itens continua natural ao conteúdo. | Implementado; capturas API 34 claras, escuras e 200%; aceite pendente |
| Onboarding (3 páginas) | Saudação, leitura e sucesso acompanham escanear, revisar e guardar em uma cena de folha e raposa sem cartão isolado; a composição compacta preserva título, corpo e botão com fonte 200%. A sequência ainda precisa de inspeção física de swipe/RTL. | Capturas Android das três páginas e 200%; aceite pendente |
| Revisão, crop e filtros | Review/Crop/filtros dão luz e profundidade à mesma página nativa. O recorte mantém quatro cantos e página inteira por padrão; controles, gesto e geometria não foram movidos. | Capturados em claro; Review também em escuro/RTL/fonte 200%; QA físico pendente |
| OCR carregando, pronto e vazio | Parágrafos ficam em uma folha selecionável, com cópia e idioma preservados. Carregamento e vazio mantêm a raposa; estados de modelo e seleção precisam inspeção com OCR real e TalkBack. | Capturado com estado injetado; fluxo real pendente |
| Exportação: PDF/JPG/PNG | Documento e raposa abrem a tarefa; nome editável, página, três formatos e qualidade contextual formam uma hierarquia sem título duplicado. Tamanho da página aparece ao expandir qualidade. Em fonte 200%, a área rola sob a ação fixa. | Refinado; capturas Android antes/depois, PDF/JPG/PNG e escuro 200%; aceite pendente |
| Exportação: progresso, sucesso, erro | A mesma folha real e a raposa acompanham preparo, gravação, sucesso e recuperação, com pose articulada conforme o estado. Sucesso identifica o arquivo e erro preserva opções de nova tentativa. | Capturado com estado injetado; exportação real/aceite pendentes |
| Histórico, busca, detalhe | Uma biblioteca com só um item deixa espaço livre. Biblioteca vazia e busca sem resultado mostram a pose vazia com texto direto no fundo, sem cartão pesado. Detalhe amplia a página única e mantém a grade em lotes; exportar, editar, favorito, excluir e abrir OCR preservados. Busca e ações documentais devem ser exercitadas em aparelho. | Vazio e busca recapturados; interação pendente |
| Configurações, idioma, sobre | Grupos em fundo contínuo e divisórias leves tiram o aspecto de cartões repetidos. Opções e alvos de toque preservados. Validar todos os idiomas, RTL e fonte ampliada. | Configurações recapturadas; revisão linguística/QA pendentes |
| Ícone adaptativo | A opção 4 equilibra rosto e folha com cantos e linha de escaneamento. Foreground adaptativo e vetor temático foram instalados no APK debug; capturas do launcher e de 24–108 dp estão em `icon-candidates/`. | Proprietário escolheu manter como candidato final; teste físico e aceite do conjunto pendentes |
| Site | Capturas de Home e opções de exportação atualizadas. | Implementado; gate local do site passou, publicação remota pendente |

As POCs Pipa/Lume informam hierarquia e ritmo, sem fornecer código ou assets de
produção. O [mapa de referências](REFERENCE_MAP.md) lista as divergências. A
[matriz de validação](VALIDATION_MATRIX.md) distingue a inspeção das capturas,
testes instrumentais, QA físico e aprovação estética.
