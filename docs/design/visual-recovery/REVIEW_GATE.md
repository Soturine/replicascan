# Revisão visual e funcional para aceite da v0.4.1

`STATUS: EM REVISÃO; SEM ACEITE FINAL` — 2026-10-13. Esta é uma revisão do APK
Android nativo em emulador API 34, não uma proposta de HTML. A
[prancha comparativa](evidence/review.html), o
[vídeo contínuo do Android](evidence/mascot-motion-live-android.mp4) e a
[prancha do ícone](icon-candidates/index.html) são os pontos de inspeção.
`evidence.zip` e os protótipos Pipa/Lume foram usados como diagnóstico e
inspiração; nenhum layout ou asset deles foi copiado. A direção A foi aprovada
apenas como direção. O ícone 4 é candidato final, não aceite do ícone. Master e
poses da raposa tiveram aceite estático; animação e telas ainda não.

| Área | Estado | Evidência Android e decisão | Limite para aceite |
|---|---|---|---|
| Home vazia, recente, cheia e carregando | REFINED | Cena de folha e raposa, Escanear dominante, indicador de Recentes; capturas em 360/390 dp com fonte 200%, claro, escuro e RTL; lista de seis itens | Fluxo real do ML Kit, importação e TalkBack não exercitados nesta revisão |
| Introdução | REFINED | Três páginas nativas e primeira em fonte 200%; raposa integrada à folha | Swipe, RTL e leitura assistiva em aparelho físico pendentes |
| Review/Crop/filtros | REFINED | Página iluminada, revisão de uma/duas páginas e viewport largo 1920×1200; filtro Comparar e pixels exportados cobertos por testes instrumentais | Gesto físico de quatro cantos, câmera e diferentes documentos reais pendentes |
| OCR | REFINED | Carregando, texto selecionável, vazio, erro persistente e modelo indisponível capturados; repetir testado | Modelo/idiomas reais e TalkBack pendentes |
| Exportação | REFINED | Preparo PDF/JPG/PNG, duas páginas, tema escuro/fonte 200%, progresso, sucesso, erro e aviso de biblioteca capturados; nome/qualidade com disclosure; correção da piscada comparada | Screenshots são estados injetados. Arquivos foram validados por testes do repositório, mas a jornada real ponta a ponta no dispositivo e o aceite visual estão pendentes |
| Histórico, busca e detalhe | REFINED | Lista de seis itens passou de cartões repetidos a linhas; toque na segunda linha abre o ID certo; vazios, busca sem resultado e detalhe multipágina capturados | Consulta Room real e ações de editar/excluir/favoritar em aparelho pendentes |
| Configurações | REFINED | Grupos contínuos e divisórias; captura contra baseline | Percorrer preferências reais, fonte 200%, RTL e TalkBack pendentes |
| Idioma e Sobre | KEPT_WITH_REASON | Hierarquia textual simples, capturas atuais contra baseline | Revisão humana dos 12 idiomas e navegação real pendentes |
| Ícone 4 | REFINED | Candidato instalado, 24–108 dp, máscaras e variante temática comparados no Android; alternativas 6/7 testadas | Aprovação estética final e launcher/OEM físico pendentes |
| Movimento da raposa | REFINED | Cabeça, cauda, patas, olhos e papel articulados por rig; testes de movimento, redução e escala 0; vídeo real via adb de Home e exportação | Rig usa máscaras de PNGs completos; clipe do emulador teve média de 9,86 fps. Fluidez e costuras em dispositivo físico não foram aprovadas |
| Diálogos e avisos | KEPT_WITH_REASON | Estados de erro/aviso de exportação visíveis na prancha; ações funcionais preservadas | Inventário visual de todos os diálogos e interação física pendentes |
| Câmera e captura física | NOT_TESTED | ML Kit Document Scanner preservado no código | Requer documento e câmera reais; não inferir aprovação das capturas sintéticas |
| Publicação v0.4.1 | DEFERRED | `main` recebe apenas checkpoints de código e evidência | Tag e release só após aprovação expressa do proprietário |

As melhorias de composição aproveitam o papel acolhedor e a hierarquia da Pipa,
e a iluminação/contexto do documento da Lume, preservando o mesmo sistema nos
temas claro e escuro. A comparação com os HTMLs é direcional: os protótipos
simulam captura, OCR e exportação, enquanto o app mantém os fluxos nativos.

O gate funcional local passou nesta rodada: Gradle `testDebugUnitTest lint
check assembleDebug assembleRelease assembleDebugAndroidTest`, os cinco gates
Python exigidos por `AGENTS.md`, 17/17 testes instrumentais de `app` e 14/14
de `core-data` no API 34. O resultado de CI do último commit ainda não existe
neste relatório. A suíte de dados inclui
Room/migração, crop aplicado aos pixels exportados, PDF pesquisável, JPG sem
recompressão quando cabível, PNG sem derivado JPEG e falha multipágina sem
omissão silenciosa. Isso não substitui a prova visual e física dos fluxos.
Consultar a [matriz de validação](VALIDATION_MATRIX.md) para o estado dos gates.
