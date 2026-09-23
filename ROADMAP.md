# Roadmap — ReplicaScan

ReplicaScan é o nome corrente a partir da v0.4.0. O produto usou outro nome até a v0.3.1; esse histórico permanece intacto no Git, nas tags, releases e no changelog.

## v0.4.0 — Identity, Simplification & Release Foundation

**Status:** publicada como release de avaliação no GitHub (APK debug); QA físico pendente.

- identidade ReplicaScan, pacote `com.soturine.replicascan`, licença corrente proprietária;
- ML Kit Document Scanner como motor principal; detector heurístico próprio, CameraX e modos de documento removidos;
- OCR com um reconhecedor por idioma de escrita, artefato estruturado e prontidão real do modelo;
- exportação sem intermediário com perdas; interface revisada e 12 catálogos reescritos;
- gate de API 36 em dispositivo gerenciado; release publicada pelo proprietário e verificada pelo CI (ADR 0003).

## v0.4.1 — Physical QA & Play Hardening

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
