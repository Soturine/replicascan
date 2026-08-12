# Escopo do Projeto

Este arquivo é histórico. O estado operacional vigente fica em [`../docs/current-state.md`](../docs/current-state.md), o lifecycle em [`../docs/data-lifecycle.md`](../docs/data-lifecycle.md) e as próximas fases em [`../ROADMAP.md`](../ROADMAP.md).

## Visão geral

`Scanora` é um aplicativo Android de escaneamento de documentos com foco em privacidade, processamento local, OCR no dispositivo, revisão rápida e exportação simples.

Identidade técnica atual:

- Nome do app: `Scanora`
- Package base: `com.soturine.scanora`
- Stack principal: Kotlin, Jetpack Compose, Material 3, CameraX, ML Kit Document Scanner, ML Kit Text Recognition, Room, DataStore, Navigation Compose e WorkManager

## Direção de produto

A partir da `v0.2.5`, o produto mantém uma decisão clara:

- `Escanear rápido` com `GmsDocumentScanner` é o fluxo principal;
- galeria/importação deve passar pelo scanner do Google quando houver suporte;
- captura manual e importação direta continuam como fallback editável;
- o valor do app está em lote local, revisão, ajuste fino, OCR consolidado por trechos, histórico e exportação sem atrito.

## Fluxo principal

1. Abrir a Home.
2. Tocar em `Escanear rápido`.
3. Capturar ou importar pelo scanner do Google.
4. Copiar as imagens retornadas para armazenamento interno estável.
5. Criar lote local no Room.
6. Abrir direto em revisão.
7. Ajustar corte/filtro apenas quando necessário.
8. Rodar OCR por trechos/texto contínuo ou exportar em PDF, JPG ou PNG.
9. Confirmar nome, tipo, tamanho, local salvo, abrir ou compartilhar.

## Fallback manual

O fallback manual existe para cenários em que o scanner rápido não basta:

- foto difícil;
- página de caderno;
- manuscrito;
- fundo poluído;
- necessidade de controlar os quatro cantos;
- importação direta de imagem para ajuste manual.

Esse caminho deve ser apresentado como `Ajuste manual` ou `Modo manual`, sem competir visualmente com o CTA principal.

## Princípios técnicos

- Não reescrever o app do zero.
- Preservar a modularização atual.
- Não adicionar backend.
- Não introduzir biblioteca pesada sem necessidade clara.
- Manter processamento de bitmap fora da main thread.
- Usar prévias em resolução intermediária na UI.
- Usar imagem estável em armazenamento interno para preview, filtros, OCR e exportação.

## Qualidade esperada

O projeto é considerado saudável quando:

- compila sem erro;
- passa em `assembleDebug`, `testDebugUnitTest` e `lint`;
- mantém README, roadmap, changelog, site e versão coerentes;
- deixa claro que o scanner rápido é o caminho principal;
- mantém o editor manual funcional como fallback real.
