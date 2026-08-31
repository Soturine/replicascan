# AGENTS.md

## Produto e fonte de verdade

ReplicaScan é um scanner Android local-first. Antes de alterar, valide código, versão, changelog, roadmap e tags; o README pode atrasar o estado real. Preserve o monólito modular e as regras em [docs/engineering/ENGINEERING_CONSTITUTION.md](docs/engineering/ENGINEERING_CONSTITUTION.md).

## Módulos

- `app`: bootstrap, navegação e onboarding;
- `core-common`: modelos, contratos e regras centrais;
- `core-data`: Room, DataStore, OCR, imagem, exportação e lifecycle;
- `core-ui`: tema e componentes;
- `feature-*`: home, câmera, editor, exportação, histórico, OCR e configurações;
- `docs`, `site`, `tools`: documentação pública, Pages e gates.

## Não negociáveis

- não reescrever o app, adicionar backend ou mover trabalho pesado para a main thread;
- `sourceUri` é canônico; derivados são cache regenerável;
- Room sem fallback destrutivo, schemas exportados e migrations testadas;
- persistir importação somente após cópia privada, com rollback em falha;
- não omitir páginas silenciosamente nem apagar URIs externas/exports do usuário;
- backup/device transfer documental desativado;
- FileProvider somente nos subdiretórios necessários, com leitura mínima;
- preview intermediário; processamento/exportação full-res fora da UI;
- baixa confiança de crop usa fallback conservador e o ajuste manual permanece funcional;
- UI limpa, acessível, localizada e com progressive disclosure.

## Comandos

Durante a implementação, prefira tasks focadas. Antes do push final:

```powershell
.\gradlew.bat testDebugUnitTest lint check assembleDebug assembleRelease assembleDebugAndroidTest
python tools/check_localization.py
python tools/check_branding.py
python tools/check_consistency.py
python -m unittest discover -s tools/tests -p "test_*.py"
```

API 36 no GitHub é o gate instrumental de release. API 35 é compatibilidade agendada. Testes automatizados não substituem câmera, TalkBack, fonte 200%, RTL e corpus físico.

## Git, licença e release

- use commits lógicos; não force-push, mova tags ou reescreva histórico;
- código corrente é proprietário conforme `LICENSE`/`LICENSING.md`; terceiros mantêm suas licenças;
- o branding gate permite o nome histórico apenas em caminhos explicitamente autorizados;
- `release/manifest.json` expressa intenção de publicar;
- GitHub Actions constrói o artefato uma vez, valida, testa API 36, cria tag anotada e publica o mesmo APK;
- não faça polling de CI: após o push, consulte uma vez e deixe o GitHub concluir;
- recuperação e idempotência: [docs/release.md](docs/release.md).

## Documentação canônica

Mudanças perceptíveis exigem revisão de `README.md`, `CHANGELOG.md`, `ROADMAP.md`, `PRIVACY_POLICY.md` e `site/`. Registre limites como implementado, validado, pendente ou fora de escopo; nunca transforme CI enfileirada em sucesso declarado.
