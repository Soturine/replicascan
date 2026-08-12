# Modelo de ameaças

Este documento descreve proteções atuais, não uma certificação de segurança.

## Ativos

- imagens canônicas e derivadas;
- texto OCR;
- títulos, tags, favoritos e ordem de páginas;
- exports em PDF/JPG/PNG.

## Fronteiras e mitigação

| Risco | Mitigação atual | Risco residual |
| --- | --- | --- |
| Backup ou transferência involuntária | `allowBackup=false` mais regras de exclusão para Android antigo e Android 12+ | comportamento de fabricante deve continuar sendo acompanhado |
| FileProvider amplo | somente subdiretórios de export; grant apenas de leitura e `ClipData` | app receptor passa a controlar a cópia recebida |
| Fonte privada órfã | deleção integrada ao repositório, rollback de importação e cleanup conservador | falha de I/O pode exigir nova execução do worker |
| Cache expirado quebrar documento | fonte canônica separada e fallback visual | fonte corrompida ainda produz estado de erro |
| URI ou nome malicioso | leitura por stream, nome UUID, extensão limitada e ownership por caminho canônico | provider externo pode ficar indisponível durante a importação |
| Perda do histórico em upgrade | sem migration destrutiva; schema exportado | uma migration ausente impede abrir até correção, sem apagar dados |
| Perda silenciosa de página | importação parcial reportada; export fail-fast identifica página | reforma de memória/streaming da exportação fica para `0.2.8` |
| Conteúdo sensível em logs | código não registra OCR, bytes nem URIs completas | relatórios de crash de terceiros não fazem parte do app atual |

## Componentes Google

O scanner guiado usa Google Play services/ML Kit Document Scanner e pode depender da disponibilidade desse componente no aparelho. OCR usa ML Kit Text Recognition no dispositivo. O Scanora não adiciona analytics, backend nem OCR remoto nesta versão.

## Fora do escopo atual

Não há cofre criptografado, secure delete garantido em flash, sync, conta ou proteção biométrica. Essas capacidades exigem threat model próprio antes de implementação.
