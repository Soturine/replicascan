# Roadmap — Scanora

O roadmap segue: **integridade → imagem canônica → detecção → geometria → filtros → OCR → exportação → busca**. Concluídos ficam no `CHANGELOG.md`.

## v0.2.9 — Image Quality, Intelligent Crop & Product Polish

**Status:** entregue em 2026-08-13

- metadata via `UPDATE`, páginas preservadas e ordem estrita;
- decoder canônico com EXIF, mirror, sampling e budgets;
- OCR canônico sem preprocess duplicado;
- imagens exportadas uma página por vez;
- onboarding com swipe, safe insets e raposas PNG animadas;
- UI creme/laranja/navy/teal e Settings compacta;
- JPEG apenas no scanner rápido; `targetSdk 36`.

## v0.3.0 — OCR as Document Data

- `OcrImagePreparer`, `OcrEngine`, readiness e lifecycle explícitos;
- texto/blocos/linhas/bounds, engine/version e fingerprint persistidos;
- Room FTS, busca e searchable PDF; benchmark antes de outra engine.

## v0.3.1 — Product & Play Store Hardening

- QA físico/upgrade, Android 16, TalkBack, font 200%, landscape/tablet;
- localization QA, R8, AAB assinado, Data Safety e store listing.

## v0.3.2 — Measured Performance

- cold start, jank, latências e peak RAM;
- thumbnails/cache medidos; Macrobenchmark/Baseline Profile só com ganho.

## v0.3.3 — Better Local Detection

- dataset legal/sintético: A4, recibo, caderno, sombra, perspectiva e `NO_DOCUMENT`;
- IoU, corner error, falsos positivos, latência, RAM, APK e bateria;
- comparar heurística, OpenCV e LiteRT sem remover fallback manual.

## v0.4.0+ — Optional Document Intelligence

- OCR plugável e layout/tabelas somente após benchmark;
- remoto apenas opt-in e com consentimento antes de sair do aparelho.

## Fora do caminho crítico

Login, cloud, chat com PDF, LLM, editor PDF completo, dezenas de modos/filtros, DI por estética e rewrite não antecedem os fundamentos.
