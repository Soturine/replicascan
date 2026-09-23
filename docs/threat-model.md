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
| FileProvider amplo | somente subdiretórios de export e `cache/captures`; grants temporários por intent | app receptor passa a controlar a cópia recebida; o app de câmera escreve apenas no arquivo de captura indicado |
| Fonte privada órfã | deleção integrada ao repositório, rollback de importação e cleanup conservador | falha de I/O pode exigir nova execução do worker |
| Cache expirado quebrar documento | fonte canônica separada e fallback visual | fonte corrompida ainda produz estado de erro |
| URI ou nome malicioso | leitura por stream, nome UUID, extensão limitada e ownership por caminho canônico | provider externo pode ficar indisponível durante a importação |
| Perda do histórico em upgrade | sem migration destrutiva; schema exportado | uma migration ausente impede abrir até correção, sem apagar dados |
| Perda silenciosa de página | importação parcial reportada; export fail-fast identifica página; imagens parciais são removidas | validar lotes grandes e interrupção por falta de espaço em aparelho real |
| Conteúdo sensível em logs | código não registra OCR, bytes nem URIs completas | relatórios de crash de terceiros não fazem parte do app atual |

## Permissões e componentes Google

O app não declara `CAMERA`: o ML Kit Document Scanner usa a permissão do Google Play services e o fallback usa o app de câmera do sistema. O manifesto final contém `INTERNET` e `ACCESS_NETWORK_STATE` trazidas pelas bibliotecas do ML Kit/Google Play services, e `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED` e `FOREGROUND_SERVICE` trazidas pelo WorkManager. O código do ReplicaScan não faz chamadas de rede. Componentes exportados além da `MainActivity` são do WorkManager/ProfileInstaller e exigem permissões de sistema.

Scanner e OCR processam o conteúdo no aparelho; modelos e a UI do scanner são baixados pelo Google Play services, que também pode enviar métricas técnicas conforme os termos do Google. O ReplicaScan não adiciona analytics, backend nem OCR remoto. Esta é uma revisão proporcional orientada ao OWASP MASVS, não uma certificação.

## Fora do escopo atual

Não há cofre criptografado, secure delete garantido em flash, sync, conta ou proteção biométrica. Essas capacidades exigem threat model próprio antes de implementação.
