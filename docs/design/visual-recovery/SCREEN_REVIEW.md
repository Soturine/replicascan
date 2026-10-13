# Revisão das telas — checkpoint de composição

`STATUS: REVISÃO PARCIAL, ACEITE VISUAL PENDENTE` (2026-10-13).

Fonte: [capturas atuais e comparação Android](evidence/review.html), com
documento sintético no emulador API 34. O proprietário aprovou o master e as
poses estáticas da raposa e autorizou continuar a revisão. Isso não aprova
automaticamente todas as telas, o movimento ou a release.

| Destino/estado | Observação e decisão | Estado |
|---|---|---|
| Home vazia, recentes, muitos itens e carregando | Cena nativa de papel e raposa, CTA Escanear de 68 dp e indicador em Recentes enquanto carrega. Em fonte 200%, a cena grande cede lugar à raposa compacta junto ao título. Seis itens demonstram a lista rolável; espaço inferior com poucos itens acompanha o conteúdo. | REFINED; capturas API 34 em claro, escuro, RTL e 360/390 dp com fonte 200%; aceite pendente |
| Onboarding (3 páginas) | Saudação, leitura e sucesso acompanham escanear, revisar e guardar em uma cena de folha e raposa sem cartão isolado; a composição compacta preserva título, corpo e botão com fonte 200%. A sequência ainda precisa de inspeção física de swipe/RTL. | Capturas Android das três páginas e 200%; aceite pendente |
| Revisão, crop e filtros | Review/Crop/filtros dão luz e profundidade à mesma página nativa. O recorte mantém quatro cantos e página inteira por padrão; controles, gesto e geometria não foram movidos. | Capturados em claro; Review também em escuro/RTL/fonte 200%; QA físico pendente |
| OCR carregando, pronto, vazio, erro e modelo indisponível | Parágrafos ficam em uma folha selecionável, com cópia e idioma preservados. Falha sem texto permanece visível com repetir; modelo indisponível tem mensagem própria nos 12 catálogos. A raposa acompanha estados sem cobrir o texto reconhecido. | REFINED; cinco estados capturados com injeção; OCR real e TalkBack pendentes |
| Exportação: PDF/JPG/PNG | Documento e raposa abrem a tarefa; nome editável, página, três formatos e qualidade contextual formam uma hierarquia sem título duplicado. Tamanho da página aparece ao expandir qualidade. Em fonte 200%, a área rola sob a ação fixa. | Refinado; capturas Android antes/depois, PDF/JPG/PNG e escuro 200%; aceite pendente |
| Exportação: progresso, sucesso, erro | A mesma folha real e a raposa acompanham preparo, gravação, sucesso e recuperação, com pose articulada conforme o estado. Sucesso identifica o arquivo e erro preserva opções de nova tentativa. | Capturado com estado injetado; exportação real/aceite pendentes |
| Histórico, busca, detalhe | Seis documentos expuseram cartões repetidos; Histórico agora usa linhas contínuas e divisórias leves, com toque preservado. Biblioteca vazia e busca sem resultado mostram a pose vazia no fundo. Detalhe amplia a página única e mantém a grade multipágina; ações documentais permanecem. | REFINED no Histórico; capturas antes/depois, vazio, busca e detalhe; ações em aparelho pendentes |
| Configurações, idioma, sobre | Grupos em fundo contínuo e divisórias leves tiram o aspecto de cartões repetidos. Opções e alvos de toque preservados. Validar todos os idiomas, RTL e fonte ampliada. | Configurações recapturadas; revisão linguística/QA pendentes |
| Ícone adaptativo | A opção 4 equilibra rosto, folha e sinal de escaneamento. Duas composições novas, folha à esquerda (6) e folha lateral (7), foram capturadas no Android em 24–108 dp e máscaras; ambas perdem clareza funcional a 24 dp. Só a 4 tem foreground/temático instalados. | 4 mantida como candidata final; 6/7 estudos; fonte editável das novas, teste físico e aceite pendentes |
| Site | Capturas de Home e opções de exportação atualizadas. | Implementado; gate local do site passou, publicação remota pendente |

As POCs Pipa/Lume informam hierarquia e ritmo, sem fornecer código ou assets de
produção. O [mapa de referências](REFERENCE_MAP.md) lista as divergências. A
[matriz de validação](VALIDATION_MATRIX.md) distingue a inspeção das capturas,
testes instrumentais, QA físico e aprovação estética.
