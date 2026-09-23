# Política de Privacidade do ReplicaScan

Última atualização: 2026-09-23 — versão 0.4.0

## Resumo

ReplicaScan funciona localmente e não exige conta, login, sincronização ou backend. O app não tem analytics próprio, rastreamento, anúncios nem OCR remoto, e o código do ReplicaScan não envia o conteúdo dos seus documentos para servidores.

## Dados processados

Páginas escaneadas ou importadas, recortes, rotações, visuais, texto reconhecido, títulos, etiquetas, favoritos e preferências ficam no aparelho. O Room guarda metadados e texto; imagens derivadas são cache regenerável.

## Permissões e componentes

- O ReplicaScan **não** solicita a permissão de câmera. O **ML Kit Document Scanner** do Google Play services abre a câmera com a permissão do próprio Google Play services; a alternativa usa o app de câmera do sistema, que grava a foto num arquivo temporário do ReplicaScan.
- A seleção de imagens usa o seletor de fotos do sistema.
- O **ML Kit Text Recognition** reconhece texto no aparelho.
- O manifesto final contém `INTERNET` e `ACCESS_NETWORK_STATE`, adicionadas pelas bibliotecas do ML Kit/Google Play services. O código do ReplicaScan não faz chamadas de rede.

Segundo a documentação do Google, o fluxo do Document Scanner e o reconhecimento de texto acontecem no aparelho. Mesmo assim, o Google Play services pode baixar e atualizar a interface e os modelos, verificar compatibilidade e coletar métricas técnicas de uso da API. Esse comportamento é regido pelos [termos do ML Kit](https://developers.google.com/ml-kit/terms) e pelos termos do Google, não pela Soturine.

## Armazenamento, backup e migração

A fonte canônica de cada página fica na área privada do ReplicaScan. Backup em nuvem e transferência automática de arquivos, banco, texto e preferências estão desativados. Desinstalar ou limpar os dados remove o histórico privado.

A v0.4.0 usa o pacote `com.soturine.replicascan` e instala separadamente das builds antigas. O app não lê nem apaga dados da instalação anterior. Exporte documentos importantes antes de remover a instalação antiga.

## Compartilhamento e exportação

Compartilhar sempre começa por uma ação sua e concede leitura temporária por `content://`. O FileProvider expõe apenas os diretórios de exportação e o arquivo temporário de captura; fontes privadas, banco, preferências e o restante do cache não são compartilhados.

No Android 10+, os arquivos exportados ficam em `Downloads/ReplicaScan`. Excluir um documento no app não remove PDFs, JPGs ou PNGs que você já exportou.

## Retenção e exclusão

Excluir uma página ou um documento remove os registros e os arquivos privados gerenciados. Fotos externas e arquivos exportados não são apagados. Temporários são limpos apenas em diretórios conhecidos e após um período de segurança.

Detalhes: [ciclo de vida dos dados](docs/data-lifecycle.md) e [modelo de ameaças](docs/threat-model.md). Para vulnerabilidades, use o processo privado de [SECURITY.md](SECURITY.md) e nunca publique documentos pessoais em issues.
