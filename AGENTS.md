# AGENTS.md

## Projeto
Scanora é um app Android de escaneamento de documentos com foco em:
- privacidade
- processamento local
- OCR no dispositivo
- revisão e exportação rápidas
- arquitetura modular pronta para evolução

## Verdadeiro contexto do repositório
Ao trabalhar neste repo:
- leia o repositório inteiro antes de alterar qualquer coisa
- não assuma que o README público está totalmente atualizado
- confie primeiro no código, changelog, roadmap, arquivos de versão e tags
- trate a documentação pública como potencialmente defasada
- preserve a arquitetura existente

## Stack esperada
- Kotlin
- Android Gradle Plugin 9.x
- Gradle Wrapper
- Jetpack Compose + Material 3
- Navigation Compose
- ViewModel + Coroutines + Flow
- Room
- DataStore
- WorkManager
- CameraX
- ML Kit Document Scanner
- ML Kit Text Recognition

## Estrutura do projeto
- `app`: entrypoint, navegação, onboarding, composição dos módulos
- `core-common`: modelos, contratos, regras e interfaces centrais
- `core-data`: Room, DataStore, OCR, exportação, pipeline de imagem
- `core-ui`: tema e componentes reutilizáveis
- `feature-home`: tela inicial
- `feature-camera`: captura
- `feature-editor`: crop, filtros, revisão de página/lote
- `feature-export`: exportação e compartilhamento
- `feature-history`: histórico
- `feature-ocr`: OCR e leitura do texto
- `feature-settings`: preferências
- `docs`: documentação complementar
- `site`: página pública do projeto

## Missão do agente
Ao editar este repositório, priorize:
1. qualidade real do produto
2. estabilidade
3. usabilidade
4. desempenho em aparelho real
5. consistência entre preview, edição e exportação
6. documentação alinhada com o estado real do projeto

## Prioridades atuais de produto
O foco do Scanora não é “ter muitas features”.
O foco é fazer bem o fluxo principal:
- capturar ou importar
- detectar e ajustar documento
- corrigir perspectiva
- aplicar visual útil
- revisar com clareza
- exportar sem atrito
- copiar OCR de forma usável

## Problemas conhecidos que devem orientar decisões
- auto crop manual/importado ainda é mais fraco que o scanner do Google
- páginas de caderno com espiral e fundos poluídos ainda quebram a heurística
- filtros locais podem parecer inferiores ao scanner do Google
- algumas telas ainda ficam poluídas visualmente
- exportação tende a mostrar opções demais ao mesmo tempo
- OCR precisa ser útil e legível, não apenas “funcionar tecnicamente”

## Regras de implementação
- não reescreva o app do zero
- não quebre a modularização
- não adicione backend
- não introduza bibliotecas pesadas sem necessidade clara
- não mova processamento pesado para a main thread
- não aplique bitmap full-res em tempo real na UI
- preview pode usar resolução intermediária
- aplicação final e exportação devem usar imagem full-res fora da UI
- preserve comportamento estável em aparelho mediano
- prefira melhorias locais, incrementais e verificáveis

## Regras permanentes de integridade e privacidade
- migrations Room de produção nunca podem usar fallback destrutivo
- todo schema Room deve ser exportado e toda mudança de versão deve possuir migration testada
- `sourceUri` é a fonte canônica persistente; derivados são cache regenerável
- importação só persiste Room depois da cópia privada e deve fazer rollback se a persistência falhar
- temporários ficam apenas em namespaces conhecidos e possuem cleanup conservador com grace period
- excluir página ou scan deve controlar também os arquivos privados gerenciados, sem apagar URIs externas ou exports do usuário
- dados documentais, banco, OCR e preferências devem permanecer excluídos de backup e device transfer automáticos
- FileProvider deve expor somente subdiretórios de export necessários, com grant mínimo de leitura
- importação e exportação nunca podem omitir páginas silenciosamente
- release exige schema versionado, testes de integridade, `check`, lint e builds debug/release

## Regras para crop / imagem
- priorize robustez sobre “mágica”
- quando a confiança do auto crop for baixa, use fallback conservador
- ajuste manual dos 4 cantos deve sempre continuar funcional
- preview e resultado final devem ser coerentes
- correção de perspectiva precisa ser estável
- rotação automática deve evitar decisões absurdas
- considere cenários reais:
  - folha simples
  - caderno pautado
  - manuscrito
  - gráfico
  - recibo
  - papel colorido
  - fundo com mesa/cadeira/objetos

## Regras para filtros
Cada filtro precisa ter intenção clara.
Evite filtros placebo ou quase iguais.

Presets preferidos:
- Original corrigido
- Documento P&B
- Documento cinza
- Colorido aprimorado
- Recibo / Alto contraste

Filtros devem:
- preservar legibilidade
- evitar branco estourado
- evitar texto apagado
- reduzir sujeira e sombra
- manter coerência com o preview

## Regras de UX
Direção visual desejada:
- mais clean
- mais direta
- menos texto
- menos cards verbosos
- menos sensação de formulário
- mais progressive disclosure
- mais foco em ação

Ao simplificar telas:
- mostre primeiro o essencial
- esconda detalhes avançados até o usuário precisar
- reduza opções simultâneas
- prefira agrupamento dependente da escolha atual
- preserve clareza do CTA principal

## Regras para exportação
- não mostrar configurações irrelevantes ao formato escolhido
- PDF pode expandir qualidades logo abaixo da seleção
- JPG/PNG não devem carregar opções de PDF na cara do usuário
- o caminho até gerar/salvar/compartilhar deve ser óbvio
- estados de sucesso e erro precisam ser curtos e claros

## Regras para OCR
- OCR deve ser tratado como recurso prático, não como promessa perfeita
- melhorar pré-processamento quando fizer sentido
- a tela de OCR deve ser legível e limpa
- não despejar texto cru de forma caótica
- priorizar copiar texto com poucos toques

## Documentação
Sempre que a mudança alterar o produto de forma perceptível, revise:
- `README.md`
- `ROADMAP.md`
- `CHANGELOG.md`
- `site/`

Se houver inconsistência de versão entre código, changelog, roadmap, README e release:
- alinhe tudo
- explique o que estava defasado
- não deixe documentação pública contraditória

## Processo de trabalho esperado
1. Ler o repo inteiro
2. Identificar arquivos-alvo
3. Fazer alterações pequenas e consistentes
4. Validar build e qualidade
5. Atualizar documentação necessária
6. Gerar resumo final objetivo

## Validação obrigatória
Antes de concluir, executar:
- `./gradlew assembleDebug`
- `./gradlew testDebugUnitTest`
- `./gradlew lint`

No Windows, usar:
- `gradlew.bat assembleDebug`
- `gradlew.bat testDebugUnitTest`
- `gradlew.bat lint`

## Entrega final esperada do agente
A resposta final deve incluir:
- o que mudou
- por que mudou
- arquivos alterados
- impacto em UX e produto
- resultado dos comandos de validação
- commit/push/tag/release, se aplicável

## O que evitar
- refactor cosmético desnecessário
- sycophancy
- mudanças grandes sem necessidade
- duplicação de lógica
- telas com excesso de texto explicativo
- opções demais ao mesmo tempo
- filtros agressivos sem ganho real
- heurísticas frágeis sem fallback
- documentação desatualizada após alteração de produto

## Regra final
Aja como maintainer sênior de um app Android real:
- pragmático
- cuidadoso com regressão
- obcecado por UX
- focado em desempenho
- focado em qualidade visível ao usuário
