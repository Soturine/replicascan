# Testes

## Unitários (JVM)

- validação de nomes (typed errors, nomes curtos reais como “RG”);
- formatação de datas por locale e fuso;
- pipeline de imagem: rotação, chaves de cache, página inalterada, invalidação de derivados e leitura de chaves de visual aposentadas;
- `OcrScriptPolicy`: ja/ko/hi, latino como padrão e interface árabe sem reconhecedor inexistente;
- pós-processamento de OCR (ordem, parágrafos, ruído, qualidade EMPTY/WEAK/PARTIAL/GOOD);
- `ScanDraftCoordinator`: ordem, importação parcial, rollback, resultado vazio do scanner e entrada ilegível;
- `ScanFileStore`/`ManagedFilePolicy`: ownership, path traversal, rollback e limpeza de órfãos;
- nomes de exportação e tags;
- `AppLanguages`: 12 opções sem rótulo vazio, “Sistema” único e resolução de tags regionais.

## Instrumentados (`androidTest`)

- Room: CRUD, cascade, ordem, migration 1→2, FTS com OCR persistido e integridade de exclusão;
- exportação: PDF pesquisável (API 35+), falha identificando a página, PNG sem JPEG intermediário, JPG inalterado copiado byte a byte e remoção dos arquivos já gravados quando uma página falha;
- app: onboarding independente do idioma do aparelho, identidade do pacote e FileProvider restrito.

Rodam em Gradle Managed Devices:

```powershell
.\gradlew.bat api36DebugAndroidTest --no-parallel "-Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect"
```

`api36` é o gate de release no GitHub (com KVM habilitado); `api35` roda de forma agendada. Com um aparelho conectado, `connectedDebugAndroidTest --no-parallel` também funciona.

## Ferramentas do repositório

`python -m unittest discover -s tools/tests -p "test_*.py"` cobre os gates de branding e consistência e o `release_artifact.py` (versão, versionCode e pacote divergentes, checksum, SHA da tag, assets inesperados e verificação idempotente).

## Não coberto por automação

Os internals do ML Kit (detecção, limpeza, reconhecimento) não são testados pelo ReplicaScan. Continuam dependendo de QA físico:

- scanner em folha A4, recibo, caderno com espiral, perspectiva forte, fundo texturizado e baixa luz;
- fallback pela câmera do sistema em aparelho sem Google Play services;
- OCR em latino, devanágari, japonês e coreano, e PDF pesquisável em leitores externos;
- TalkBack, fonte 200%, RTL (árabe), paisagem, tablet e tema escuro;
- exportação de documentos grandes com pouco espaço livre.

## Qualificação local completa

```powershell
.\gradlew.bat testDebugUnitTest lint check assembleDebug assembleRelease assembleDebugAndroidTest
python tools/check_localization.py
python tools/check_branding.py
python tools/check_consistency.py
python tools/check_site.py
python -m unittest discover -s tools/tests -p "test_*.py"
```
