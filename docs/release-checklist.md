# Checklist de release

## Fonte e identidade

- [ ] branch `main`, worktree limpo e remoto canônico;
- [ ] versão, código, pacote, manifest, changelog, README e site alinhados;
- [ ] branding/localização/consistência verdes;
- [ ] tag ausente ou já apontando exatamente ao SHA pretendido;
- [ ] nenhum segredo, documento real ou binário inesperado no diff.

## Integridade e segurança

- [ ] migrations não destrutivas e schemas versionados;
- [ ] import/delete/rollback controlam apenas arquivos privados gerenciados;
- [ ] backup/device transfer e FileProvider mantêm menor privilégio;
- [ ] CodeQL e testes package/provider/Room verdes;
- [ ] dependências e avisos de terceiros revisados.

## Qualificação local

```powershell
.\gradlew.bat testDebugUnitTest lint check assembleDebug assembleRelease assembleDebugAndroidTest
python tools/check_localization.py
python tools/check_branding.py
python tools/check_consistency.py
python -m unittest discover -s tools/tests -p "test_*.py"
```

## Publicação remota

- [ ] `release/manifest.json` usa `publish: true` apenas no SHA final;
- [ ] pre-rename/final push sem force push e HEAD remoto verificado;
- [ ] GitHub API 36 é o gate instrumental principal; API 35 é compatibilidade agendada;
- [ ] artefato construído uma vez, checksum produzido junto e mesmo arquivo reutilizado;
- [ ] tag anotada e release são responsabilidade do workflow após green;
- [ ] estado remoto consultado uma vez, sem polling agentic;
- [ ] validação física (câmera/crop/TalkBack/200%/RTL) reportada honestamente como concluída ou pendente.
