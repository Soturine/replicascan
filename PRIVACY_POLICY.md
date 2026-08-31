# Política de Privacidade do ReplicaScan

Última atualização: 2026-08-31 — versão 0.4.0

## Resumo

ReplicaScan funciona localmente e não exige conta, login, sincronização ou backend. O app não possui analytics próprio, tracking ou OCR remoto e não envia automaticamente o conteúdo dos seus documentos para servidores da Soturine.

## Dados processados

Imagens capturadas/importadas, crop, rotação, filtros, OCR, títulos, tags, favoritos e preferências permanecem no aparelho. Room guarda metadados e OCR; derivados visuais são cache regenerável.

## Permissões e componentes

- `CAMERA` é usada somente quando você abre a captura;
- a seleção de mídia usa os seletores da plataforma;
- Google Play services ML Kit Document Scanner oferece o scanner guiado;
- ML Kit Text Recognition processa OCR no dispositivo.

Embora entradas e resultados do ML Kit sejam processados no aparelho, componentes do Google podem contatar seus serviços para baixar/atualizar modelos e componentes, verificar compatibilidade e coletar métricas de desempenho/utilização da API. Esse comportamento é regido pelos [termos do ML Kit](https://developers.google.com/ml-kit/terms) e pelos termos do Google, não pela Soturine.

## Armazenamento, backup e migração

A fonte canônica fica na área privada do ReplicaScan. Backup em nuvem e transferência automática de arquivos, banco, OCR e preferências permanecem desativados. Desinstalar ou limpar os dados pode remover o histórico privado.

A v0.4.0 usa o novo pacote `com.soturine.replicascan` e instala separadamente das builds antigas. O app não lê nem exclui dados privados da instalação anterior. Exporte documentos importantes antes de remover a instalação antiga.

## Compartilhamento e exports

Compartilhamento só começa após uma ação do usuário e concede leitura temporária por `content://`. O FileProvider expõe apenas subdiretórios de exportação; fontes privadas, banco, preferências e a raiz do cache não são compartilhados.

Em Android 10+, os arquivos finais são salvos em `Downloads/ReplicaScan`. O app não apaga a pasta histórica usada por versões anteriores. Excluir um lote privado não remove PDFs/JPGs/PNGs que você já exportou.

## Retenção e exclusão

Excluir página ou lote remove registros e arquivos privados gerenciados correspondentes. Fotos originais externas e exports finais não são apagados. Temporários só são limpos em namespaces conhecidos e com período de segurança.

Detalhes: [data lifecycle](docs/data-lifecycle.md) e [threat model](docs/threat-model.md). Para vulnerabilidades, use o processo privado descrito em [SECURITY.md](SECURITY.md) e nunca publique documentos pessoais em issues.
