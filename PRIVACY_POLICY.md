# Política de Privacidade do Scanora

Última atualização: 2026-08-12

## Resumo

O Scanora funciona localmente e não exige conta, login, sincronização ou backend. A versão `0.2.7` desativa cloud backup e transferência automática dos dados do app, além de restringir o compartilhamento aos arquivos que o usuário escolheu exportar.

## Dados processados

O app pode manter no aparelho imagens capturadas ou importadas, crop/rotação/filtro, texto OCR, títulos, tags, favoritos, preferências e exports em PDF/JPG/PNG.

## Permissões e componentes

- `CAMERA` é usada apenas na captura manual;
- seleção de mídia usa os seletores da plataforma;
- ML Kit Document Scanner, via Google Play services, oferece o scanner guiado;
- ML Kit Text Recognition executa OCR no dispositivo;
- não há analytics, tracking nem envio automático de documentos para servidor próprio ou OCR remoto.

## Armazenamento e backup

A fonte canônica de cada página fica na área privada do Scanora. Metadados e OCR ficam em Room; derivados ficam em cache regenerável. Arquivos, banco, preferências e armazenamento externo do app são excluídos das regras de cloud backup e device transfer, com `allowBackup` também desativado.

O Scanora ainda não oferece backup próprio. Desinstalar ou limpar os dados do app pode remover o histórico privado.

## Compartilhamento e exports

Compartilhamento só começa após ação do usuário e concede leitura temporária por `content://`. O FileProvider não expõe fontes privadas, banco, preferências ou a raiz do cache.

Em Android 10+, exports finais são salvos em `Downloads/Scanora`. Esses arquivos passam a pertencer ao usuário: excluir o scan privado não apaga automaticamente um PDF/JPG/PNG já exportado.

## Retenção e exclusão

Excluir página ou lote remove os registros e os arquivos privados gerenciados correspondentes. Fotos originais da galeria, conteúdo de outros providers e exports finais não são apagados. Derivados e temporários antigos podem ser removidos automaticamente; fontes órfãs só são removidas dentro do namespace gerenciado e após período de segurança.

Detalhes técnicos estão em [docs/data-lifecycle.md](docs/data-lifecycle.md) e [docs/threat-model.md](docs/threat-model.md).

## Contato

Para vulnerabilidades, siga `SECURITY.md` e não publique documentos ou dados pessoais em issues.
