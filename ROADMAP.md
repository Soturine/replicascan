# Roadmap — ReplicaScan

ReplicaScan é o nome corrente a partir da v0.4.0. O produto usou outro nome até a v0.3.1; esse histórico permanece intacto no Git, nas tags, releases e no changelog.

## v0.4.0 — Identity, Simplification & Release Foundation

**Status:** publicada como release de avaliação no GitHub (APK debug); QA físico pendente.

- identidade ReplicaScan, pacote `com.soturine.replicascan`, licença corrente proprietária;
- ML Kit Document Scanner como motor principal; detector heurístico próprio, CameraX e modos de documento removidos;
- OCR com um reconhecedor por idioma de escrita, artefato estruturado e prontidão real do modelo;
- exportação sem intermediário com perdas; interface revisada e 12 catálogos reescritos;
- gate de API 36 em dispositivo gerenciado; release publicada pelo proprietário e verificada pelo CI (ADR 0003).

## v0.4.1 — Redesign e recuperação

**Laboratório de 23/10:** atlas criado a partir do master único ainda duplicou patas e não ofereceu encaixes limpos; uma sequência quadro a quadro também introduziu pata extra. Ambos foram isolados como estudos reprovados em [motion-lab-2026-10-23](docs/design/motion-lab-2026-10-23/README.md), sem substituir os assets instalados. O próximo gate é uma fonte editável desenhada com anatomia coerente, seguida de montagem neutra e extremos em quatro escalas antes de qualquer integração Compose.

**Checkpoint de 23/10:** a tentativa de rig com novas pinturas foi reprovada visualmente pelo proprietário (proporção das patas e encaixe do cachecol) e retirada do runtime. As poses aprovadas voltaram; o texto da ação principal cresceu para 17sp. Um rig articulado de qualidade, QA físico, matriz final e CI do SHA de release ainda são bloqueios. Não criar tag ou release v0.4.1.

**Checkpoint de 14/10:** Home/Configurações receberam calor e hierarquia do produto histórico, com laranja acessível, slogan localizado e cena documental; o ícone opção 4 agora usa fundo teal para separar a folha. A contenção da mascote elimina recortes anatômicos defeituosos, mas **não entrega articulação independente**. A opção de ícone desta rodada foi escolhida pelo agente com evidência nativa, sem novo aceite do proprietário. A publicação permanece bloqueada pelo rig definitivo e pelos gates finais.

**Status:** recuperação visual em andamento. A arte mestre e as poses da raposa foram aprovadas como arte estática; o rig articulado candidato foi desativado em 2026-10-14 por vazamento de pixels; o novo loop seguro e a pose de atenção corrigida seguem qualificação, e um rig de partes reais permanece pendente. A opção 4 do ícone, com símbolo de escaneamento, foi escolhida como candidata final e instalada no APK debug; teste físico e aceite do conjunto visual ainda faltam. Telas e release também permanecem pendentes. O pedido de 2026-10-14 dispensa aceite visual intermediário para uma release de avaliação, mas exige gates técnicos completos; a release permanece bloqueada pelo rig final e sua prova visual.

- tema papel/tinta e telas do ciclo anterior estão sob revisão; Home agora tem cena nativa de papel e raposa com ação Escanear dominante, indicador em Recentes e raposa compacta em fonte 200% verificada em 360/390 dp; a introdução integra três cenas de folha e raposa aprovadas como direção; exportação conecta a prévia real à raposa em preparo, gravação, sucesso e erro; Review/Crop/filtros compartilham a iluminação da página sem alterar o recorte, OCR apresenta uma folha de texto selecionável, Histórico vazio usa composição aberta, detalhe amplia uma página única e configurações usam grupos leves; demais telas ainda precisam de refinamento; o atlas Compose rejeitado foi substituído por poses aprovadas e uma animação de pose inteira sem recortes anatômicos defeituosos, ainda sem rig final;
- preparo da exportação refinado com arquivo editável, formatos legíveis e opções de PDF progressivas; capturas Android antes/depois existem, mas fluxo real e aceite visual ainda faltam;
- OCR distingue ausência de texto, falha e modelo indisponível com recuperação visível; capturas Android existem, mas modelo e documento físicos ainda precisam de QA;
- Histórico cheio passou a lista contínua, com comparação Android de seis documentos; busca persistida e ações de detalhe ainda precisam de QA em aparelho;
- crop sequencial corrigido e PNG real verificado; exportação com estados persistentes;
- capturas Android, fonte 200%, direção RTL e layout largo; limites em [validation.md](docs/design/validation.md).

## Próximo marco — Physical QA & Play Hardening

- validar scanner, fallback, crop, TalkBack, fonte 200%, RTL e upgrade em aparelhos físicos;
- revisão linguística nativa dos 12 idiomas;
- posicionar o texto do PDF pesquisável pelas caixas do OCR e validar em leitores externos;
- medir cold start, jank, latência de OCR/exportação, RAM e tamanho do artefato;
- AAB assinado com Play signing, R8 com regras justificadas, Data safety e store listing com evidências reais.

## v0.5.0+ — Optional Intelligence

- layout/tabelas e OCR alternativo somente após benchmark;
- processamento remoto apenas opt-in, com consentimento anterior à saída do aparelho;
- monetização somente depois de revisão de privacidade e UX.

## Fora do caminho crítico

Backend, login, cloud sync, anúncios, analytics, OpenCV/LiteRT, OCR remoto e detector próprio não fazem parte do caminho atual.
