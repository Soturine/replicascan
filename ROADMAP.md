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

**Status:** recuperação visual em andamento. A arte mestre e as poses da raposa foram aprovadas como arte estática; o rig articulado candidato e a pose de atenção corrigida aguardam inspeção e aceite. Telas, ícone e release também permanecem pendentes. Sem tag/release v0.4.1 antes do aceite visual expresso e da qualificação do HEAD final.

- tema papel/tinta e telas do ciclo anterior estão sob revisão; Home agora tem cena nativa de papel e raposa com ação Escanear dominante e composição reduzida em fonte 200%; opções de exportação e sequência de introdução continuam em refinamento; o atlas Compose rejeitado foi substituído por poses aprovadas e um rig de máscaras em avaliação de movimento;
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
