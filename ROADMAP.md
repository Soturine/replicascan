# Roadmap — ReplicaScan

ReplicaScan é o nome corrente a partir da v0.4.0. O produto usou outro nome até a v0.3.1; esse histórico permanece intacto no Git, nas tags, releases e no changelog.

## v0.4.0 — Identity & Engineering Foundation

**Status:** implementação preparada; publicação depende dos gates assíncronos do GitHub.

- rebrand completo de produto, pacote Android, repositório, recursos e artefatos;
- licença corrente proprietária com fronteira histórica e avisos de terceiros;
- gates negativos de branding, localização e consistência de release;
- API 36 como gate instrumental principal e API 35 como compatibilidade agendada;
- release fail-closed com APK construído uma vez, checksum, attestation e tag anotada após sucesso;
- constituição de engenharia, ADRs e runbook de recuperação.

## v0.4.1 — Physical QA & Play Hardening

- validar câmera, crop, TalkBack, fonte 200%, RTL e upgrade em aparelhos físicos;
- medir cold start, jank, latência, pico de RAM e tamanho do artefato;
- preparar AAB e assinatura de produção sem armazenar segredo no repositório;
- concluir Data Safety e store listing com evidências reais.

## v0.4.2 — Measured Document Quality

- ampliar corpus legal/sintético de folha, recibo, caderno, sombra e fundos poluídos;
- comparar IoU, erro de cantos, falsos positivos, legibilidade, latência e memória;
- melhorar heurística/filtros apenas quando métricas superarem a base atual;
- preservar crop manual e fallback conservador.

## v0.5.0+ — Optional Intelligence

- layout/tabelas e OCR alternativo somente após benchmark;
- processamento remoto apenas opt-in, com consentimento anterior à saída do aparelho;
- monetização somente depois de revisão de privacidade e UX.

## Fora do caminho crítico

Backend, login, cloud sync, AdMob, assinatura/Play Console, OpenCV/LiteRT, novo OCR, detector ML, domínio e redesign amplo não fazem parte da v0.4.0.
