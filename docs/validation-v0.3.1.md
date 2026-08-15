# Scanora v0.3.1 — evidências de validação

Registro local executado em 15 de agosto de 2026, no Windows com Microsoft JDK 17.

## Gates concluídos

- `gradlew.bat assembleDebug testDebugUnitTest lint assembleRelease assembleDebugAndroidTest check --quiet` — código 0 após o ajuste responsivo final.
- `python tools/check_localization.py` — 9 módulos × 12 localidades completos.
- `git diff --check` — sem erros de whitespace antes do commit de fechamento.
- `:feature-export:compileDebugKotlin :feature-editor:compileDebugKotlin :app:assembleDebug` — código 0 após o ajuste responsivo final.

## Identidade visual aceita

- O ícone usa a mesma fonte oficial `scanora_mascot_welcome.png` exibida no produto.
- `ic_launcher_fox.png` e `scanora_icon_source.png` são PNG ARGB 768×768, com alfa 0 no canto e SHA-256 idêntico `EF9EECB5F98279FFD09FAC1616EF9E85812AB4F30C62BF64F88D20442DCF276F`.
- Duas gerações que apresentaram deformidade, inconsistência de personagem ou fundo falso foram rejeitadas e não fazem parte do repositório.

## Validação física ainda pendente

Os gates automatizados não equivalem à aprovação em aparelho real. Permanecem para validação do proprietário: Motorola intermediário, TalkBack, fonte 200%, RTL, latência da câmera e corpus físico de crop. Esses itens continuam marcados como parciais no roadmap e na matriz de implementação.
