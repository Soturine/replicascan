# Arquitetura

Monólito modular em Kotlin, Jetpack Compose e Material 3. Tecnologia externa fica atrás de fronteiras do próprio ReplicaScan.

```text
ReplicaScan UI
   │
   ├── Escanear ──► ML Kit Document Scanner (Google Play services)
   │                   └── páginas JPEG já recortadas/limpas
   ├── Importar ──► seletor de fotos do sistema
   └── fallback ──► câmera do sistema (TakePicture) → crop manual
                │
                ▼
     cópia privada atômica (filesDir/scan-sources)
                │
                ▼
        sourceUri canônico ──► Room (scans, pages, OCR, FTS)
                │
        ┌───────┴────────┐
     Revisão            Texto
  crop/giro/visual   ML Kit Text Recognition v2
        │            artefato estruturado → FTS
        └───────┬────────┘
             Exportação
      PDF (pesquisável) / JPG / PNG
```

## Módulos

- `app`: `Application`, container manual de dependências, onboarding, `NavHost` e `ScanDraftCoordinator` (cópia privada + criação do documento com rollback).
- `core-common`: modelos, contratos de repositório, `ImagePipelineSpec`, `OcrScriptPolicy`, validação de nomes e decodificação canônica (`CanonicalImageDecoder`, EXIF).
- `core-data`: Room, DataStore, `ScanFileStore`, `DefaultDocumentProcessingRepository` + `DocumentFilters`, `DefaultOcrRepository`, `DefaultExportRepository` e o worker de limpeza.
- `core-ui`: tema, tokens, `DocumentListItem`, `ReplicaScanToolButton`, carregamento de imagem com fallback e mascote.
- `feature-home`: ação principal Escanear (`rememberDocumentScanner`, única entrada para o ML Kit), importação e fallback pela câmera do sistema.
- `feature-editor`: Revisão (documento em destaque), Cortar (crop manual) e Ajustar (visual da página).
- `feature-ocr`, `feature-export`, `feature-history`, `feature-settings`.

Não há injeção de dependência por framework; `AppContainer` cria as implementações de forma explícita.

## Captura

O ML Kit Document Scanner (modo `FULL`) é o motor principal: detecção de bordas, perspectiva, rotação, filtros e limpeza acontecem na UI do provedor, no aparelho. O app recebe apenas as páginas JPEG. Cancelar ou voltar sem páginas não cria nada. Se o scanner não puder abrir (sem Google Play services, RAM abaixo do mínimo do provedor), a Home oferece a câmera do sistema ou a importação, seguidas de crop manual. O ReplicaScan não tem detector automático próprio nem CameraX.

## Pipeline de imagem

- `sourceUri` é a verdade; `processedUri` é apenas cache de miniatura.
- Transformações aprovadas: quad manual (ausente ou página inteira = sem warp), rotação em passos de 90° e um visual opcional: Original (sem alteração), Realçado, Cinza ou Preto e branco.
- `ImagePipelineSpec.VERSION = 4` versiona as chaves de cache.
- Previews são reduzidos (≤ 1800 px); exportação renderiza em memória (`renderBitmap`), uma página por vez, sem JPEG intermediário. Página sem alterações exportada como JPG é copiada byte a byte.
- OCR recebe só a geometria (crop + rotação), sem binarização.

## OCR

Um único reconhecedor por execução, escolhido por `OcrScriptPolicy` a partir do idioma do app (ja → japonês, ko → coreano, hi/mr/ne → devanágari, demais → latino). A pessoa pode trocar em “Tentar outro idioma”. A prontidão do modelo vem do `ModuleInstallClient` do Google Play services (`READY`, `DOWNLOAD_PENDING`, `UNAVAILABLE`, `ERROR`). O resultado estruturado (blocos, linhas, caixas, confiança, engine, versão, pipeline, fingerprint) é salvo em `page_ocr_artifacts` junto com o índice FTS na mesma transação. OCR árabe não é oferecido: o ML Kit Text Recognition v2 não o suporta.

## Persistência

- Room (schema 2, exportado, migration 1→2 testada). A coluna `scans.mode` foi aposentada sem migration: é gravada como `document` e não influencia o comportamento.
- DataStore: onboarding, tema e qualidade padrão do PDF. A chave antiga `default_scan_mode` é ignorada.

## Navegação

Rotas string em `ReplicaScanDestinations`. Após escanear ou importar, o app abre a Revisão; fotos do fallback abrem o Cortar por cima da Revisão. Cada tela secundária tem ação de voltar.
