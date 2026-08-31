# Publicação

## GitHub Release

A publicação é automatizada e fail-closed. `release/manifest.json` define a intenção; o workflow de release valida versão/identidade, constrói o APK de avaliação uma vez, gera SHA-256, executa API 36, CodeQL e site, e publica o mesmo artefato somente após todos os gates.

Não crie/mova a tag manualmente e não reconstrua o APK durante a publicação. O runbook idempotente está em [release.md](release.md).

## GitHub Pages

O workflow independente `pages.yml` valida e publica `site/` em <https://soturine.github.io/replicascan/>. Falha de Pages é recuperada separadamente e não altera tags/releases.

## Play Store

Assinatura de produção, AAB, Play Console, Data Safety final e screenshots de loja estão fora da v0.4.0. O APK anexado à GitHub Release é explicitamente uma build debug para avaliação, não um artefato production-signed.
