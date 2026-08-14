# Checklist de release

## Repositório e versão

- [ ] `git status --short` limpo e branch correta;
- [ ] `versionName`, `versionCode`, README, site e changelog alinhados;
- [ ] tag ainda não existe e apontará para o HEAD publicado.

## Integridade e privacidade

- [ ] não existe destructive migration em produção;
- [ ] schema Room foi exportado e mudanças possuem migration/teste;
- [ ] deleção e rollback cobrem arquivos privados sem tocar em externos;
- [ ] cache expirado possui fallback;
- [ ] backup e device transfer seguem a política documentada;
- [ ] FileProvider expõe apenas diretórios necessários;
- [ ] diff não contém segredos, documentos reais ou binários inesperados.

## Gates

```bash
./gradlew clean
./gradlew testDebugUnitTest
./gradlew lint
./gradlew check
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew assembleDebugAndroidTest
```

- [ ] `python tools/check_localization.py` confirma chaves, plurais e placeholders nos 12 idiomas;
- [ ] light/dark, landscape, fonte 200% e TalkBack foram revisados em aparelho ou emulador;
- [ ] `:core-data:connectedDebugAndroidTest` executado em API 35 para migration, FTS e PDF pesquisável;
- [ ] smoke manual cobre captura, importação parcial, fallback, deleção, OCR, export e compartilhamento;
- [ ] CI final concluiu sem findings ignorados.

## Publicação

- [ ] commits são pequenos e revisáveis;
- [ ] `main` foi enviada sem force push;
- [ ] HEAD remoto coincide com o local;
- [ ] tag anotada foi enviada;
- [ ] GitHub Release usa apenas garantias validadas;
- [ ] APK/AAB só é anexado quando assinatura e validação do artefato estiverem definidas.
