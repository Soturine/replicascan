# Roadmap — Scanora

O roadmap começa pela confiabilidade dos documentos locais. Itens concluídos pertencem ao `CHANGELOG.md`; este arquivo mantém as próximas decisões e seus critérios.

## Princípios

- scanner rápido do Google como fluxo principal e CameraX/manual como fallback editável;
- `sourceUri` canônico, derivados descartáveis e processamento local por padrão;
- evolução incremental, medida em aparelho mediano, sem backend ou biblioteca pesada por estética;
- nenhuma promessa de IA/OCR substitui validação humana do documento.

## v0.2.7 — Integridade e Privacidade

**Status:** entregue em 2026-08-12

- Room sem migration destrutiva e schema versionado;
- lifecycle de fontes privadas, derivados, temporários e exports;
- deleção física com ownership e orphan cleanup conservador;
- cache expirável com fallback para a fonte;
- backup/device transfer desativados e FileProvider restrito;
- importação parcial explícita com rollback;
- captura manual serializada;
- exportação fail-fast quando uma página não pode ser renderizada;
- testes de integridade, CI, CodeQL, Dependabot e documentação normativa.

## v0.2.8 — Simples por Design

**Status:** entregue em 2026-08-12

- design system claro/escuro e fluxo visual inspirado no conceito Scanora;
- Home, onboarding, editor, OCR, exportação, histórico e configurações mais diretos;
- mascote dinâmica para boas-vindas, processamento e sucesso;
- acessibilidade estrutural, edge-to-edge e layouts adaptativos;
- English como fallback e cobertura completa de pt-BR, es, fr e it;
- idioma automático e seleção por aplicativo com APIs oficiais;
- gate de localização, plurais e formatação locale-aware.

## v0.2.9 — Exportação robusta e pipeline de imagem

**Objetivo:** reduzir picos de memória e tornar exports grandes previsíveis.

- extrair incrementalmente decode, transformação, filtros e armazenamento derivado da God Class;
- decodificação amostrada por destino, budgets de bitmap e reciclagem de intermediários;
- escrita de PDF/ZIP/imagens por streaming, sem documento inteiro em `ByteArrayOutputStream`;
- page size e escala de PDF coerentes, cleanup de saída parcial e relatório por página;
- golden tests sintéticos por histograma/geometria, evitando pixel-perfect frágil;
- avaliar searchable PDF somente após pipeline e memória estabilizarem.

## v0.3.0 — OCR confiável e document experience

**Objetivo:** tornar OCR local previsível, pesquisável e reutilizável.

- unificar preparação de OCR e lifecycle do `TextRecognizer`;
- estados claros para modelo indisponível/primeiro uso;
- persistir estrutura útil, engine e versão sem transformar erro de IA em verdade;
- busca OCR local e avaliação de text layer para PDF;
- benchmark de PP-OCR local apenas com dataset próprio/licenciado e ganho mensurável.

## v0.3.1 — QA e publicação pública

- QA físico do fluxo principal, importação, deleção, OCR, export e compartilhamento;
- acessibilidade, TalkBack, tamanhos de fonte e contraste;
- screenshots reais e store listing final;
- assinatura/release artifact, R8/minify e validação de atualização instalada;
- smoke test de release e política de suporte.

## v0.3.1 — Performance percebida

- medir cold start, jank, tempo de preview e consumo de memória;
- thumbnails dedicadas e cache com limites;
- Baseline Profiles/Macrobenchmark somente com benefício medido;
- cancelamento e backpressure em trocas rápidas de página/filtro.

## v0.3.2 — Detecção local aprendida

- comparar heurística atual com modelo LiteRT pequeno em folha, recibo, espiral, fundo poluído e perspectiva extrema;
- medir precisão, falso positivo, latência, RAM, APK, bateria e aparelhos antigos;
- manter ajuste manual e fallback conservador; adotar modelo só com ganho real.

## v0.4.0 — Document intelligence opcional

- `MlKitOcrEngine` continua padrão local;
- PaddleOCR pode ser experimento local após benchmark/licença;
- DeepSeek/Docling/servidor entram apenas como engine remota opt-in, com aviso explícito antes de enviar qualquer página;
- preservar imagem, engine, versão, saída bruta/estruturada e indicação de incerteza.

## v0.5.0+ — Candidatos, não compromisso

- cofre privado com BiometricPrompt e threat model específico, sem prometer secure delete em flash;
- sync opcional somente com conta, criptografia, conflitos, versionamento, delete propagation e nova política de privacidade.

## Referências a acompanhar

FairScan, OSS Document Scanner, OpenScan, PaddleOCR e OCRmyPDF servem como referências de comportamento e benchmark, não como templates para cópia automática.
