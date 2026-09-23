# Lifecycle dos dados

```text
captura/importação
        |
        v
fonte privada canônica -----> metadados Room
        |                         |
        +--> derivados/cache     +--> OCR salvo
        |
        +--> export final do usuário
```

## Fonte canônica

Cada imagem aceita é primeiro copiada, por arquivo temporário e rename, para `filesDir/scan-sources`. O nome é gerado pelo ReplicaScan e não reutiliza nomes não confiáveis do provider. A página só é persistida depois da cópia; se Room falhar, as cópias daquela operação sofrem rollback.

A fonte existe enquanto a página existir. Ela não é limpa como cache, não é compartilhada por FileProvider e não entra em backup ou transferência automática.

## Metadados e OCR

Room guarda documento, páginas, ordem, `sourceUri`, crop, rotação, visual, `processedUri` opcional e o artefato OCR estruturado. Chaves de visual aposentadas continuam legíveis: `auto` vira Original e `receipt_high_contrast` vira Preto e branco, sem migration. `sourceUri` mais as transformações lógicas são a verdade recuperável. Uma versão de banco sem migration conhecida falha ao abrir, em vez de apagar o histórico.

## Derivados e temporários

`cacheDir/processed` contém previews, filtros aplicados e imagens preparadas para OCR. Esses arquivos podem expirar. A UI tenta o derivado e, se ele estiver ausente ou ilegível, usa a fonte; se ambos falharem, encerra o loading com um estado de erro discreto.

Fotos do fallback pela câmera do sistema ficam em `cacheDir/captures` somente até a cópia privada; são removidas após a importação ou quando a captura é cancelada, e o worker limpa sobras após o grace period. Páginas do ML Kit Document Scanner chegam por URIs temporárias do Google Play services e são copiadas imediatamente. `cacheDir/shared-exports` é o único namespace interno de cache autorizado para compartilhamento futuro. O worker remove derivados, temporários antigos e fontes órfãs somente após grace period.

## Deleção

- excluir página: remove a linha, reindexa as restantes e remove fonte/derivado gerenciados;
- excluir a última página: também remove o documento (com confirmação na interface);
- excluir documento: remove todas as páginas por cascade e seus arquivos privados;
- URI externa, foto da galeria e export final nunca são apagados como efeito da exclusão do scan;
- falha física é reportada como cleanup parcial e o orphan cleanup pode tentar novamente.

## Export e ownership

Em Android 10+, a saída final fica em `Downloads/ReplicaScan` via MediaStore. Em versões anteriores, fica no diretório de export do app. Depois da exportação, o arquivo é um produto separado: excluir o scan privado não exclui automaticamente PDFs/JPGs/PNGs já salvos.

O FileProvider expõe apenas os subdiretórios de export e `cache/captures` (alvo de escrita da câmera do sistema); os intents concedem acesso temporário.

## Backup

Cloud backup e device transfer automáticos estão desativados e possuem regras explícitas de exclusão para arquivos, banco, preferências e armazenamento externo do app. O ReplicaScan ainda não oferece backup próprio ou sincronização.
