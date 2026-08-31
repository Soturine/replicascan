# Engineering Constitution

## Objetivo

ReplicaScan otimiza para correção, regra de negócio, segurança, privacidade, integridade, manutenção, testabilidade, acessibilidade, desempenho medido, reprodutibilidade, supply chain, operação e UX. Sofisticação aparente, volume de código e quantidade de testes não são metas.

## Arquitetura e código

- Preserve o monólito modular, contratos claros, alta coesão e dependências previsíveis.
- O módulo `app` compõe; regras e o document engine ficam fora das telas.
- Prefira a menor solução correta. Evite abstrações estéticas, Managers genéricos, God Objects, duplicação, código morto, TODO permanente e features placebo.
- Todo código gerado é rascunho até revisão, teste proporcional e inspeção do diff.

## Dados, concorrência e recursos

- A fonte privada canônica nunca depende de cache; escrita crítica é transacional ou possui rollback.
- Não use migrations destrutivas. Versione schemas e teste upgrades.
- Jobs concorrentes precisam de ownership/idempotência explícitos; nunca omita páginas por corrida ou falha parcial.
- Decodifique com sampling e budgets. UI usa previews; full-res é processado sequencialmente fora da main thread.
- Otimize somente a partir de medição de latência, RAM, jank, tamanho e energia.

## Segurança e privacidade

- Menor privilégio em manifest, FileProvider, workflows e grants.
- Sem segredos no código, logs, artefatos ou histórico.
- Dados documentais não entram em backup/device transfer automático.
- Claims de segurança e processamento local devem refletir também o comportamento dos SDKs terceiros.
- Dependências novas exigem necessidade, licença, manutenção, superfície de ataque e custo de artefato avaliados.

## Produto, acessibilidade e internacionalização

- Ação principal clara, opções dependentes reveladas progressivamente e estados curtos.
- Crop manual e recuperação de erro nunca podem depender de heurística perfeita.
- Preserve alvos de toque, TalkBack, fonte ampliada, contraste, RTL e catálogos completos.
- OCR é ferramenta prática com incerteza, não promessa de transcrição perfeita.

## Verificação

- Testes cobrem riscos e invariantes, não o próprio mock.
- Execute tasks focadas durante a mudança e uma qualificação consolidada antes do push.
- Branding, localização, consistência, package/provider, Room, build e lint são gates.
- Verificação automatizada não substitui validação física. Registre claramente o que permanece pendente.
- Uma auditoria independente deve conseguir reconstruir versão, fonte, artefato, checksum e decisão de release.

## Git, CI e release

- Commits são lógicos, revisáveis e não misturam refactor cosmético sem benefício.
- Tags/releases públicas e histórico de licenças são imutáveis.
- Actions são pinadas por SHA, permissões são mínimas e o release falha fechado.
- O artefato é construído uma vez e reutilizado; a tag anotada nasce apenas após todos os gates obrigatórios.
- O agente não fica observando CI. GitHub Actions orquestra espera, gates e publicação; o handoff informa estado objetivo.

## Resiliência e operação

- Fluxos críticos têm fallback conservador, mensagens acionáveis e cleanup limitado a namespaces conhecidos.
- Pipelines de release são idempotentes: nunca movem tag nem substituem artefato silenciosamente.
- Documentação descreve o produto real, limites, recovery e decisões duradouras por ADR.
