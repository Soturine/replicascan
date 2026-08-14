# Avaliacao de deteccao de documentos

O harness de v0.3.0 mede o detector local sem transformar exemplos favoraveis em promessa de produto.

## Conjunto de dados

- `synthetic`: imagens geradas no teste instrumentado, sem dados pessoais.
- `private-device`: fotos reais mantidas fora do Git; o manifesto guarda apenas categoria e identificador anonimo.
- categorias obrigatorias: folha simples, caderno/espiral, recibo, papel colorido, fundo poluido e negativo sem documento.

## Metricas

- recall de documento e taxa de falso positivo;
- IoU da caixa delimitadora e erro normalizado dos quatro cantos;
- latencia p95, registrada separadamente por classe de aparelho;
- pico de memoria observado durante a rodada instrumentada.

Os utilitarios deterministas ficam em `DocumentDetectionMetrics`. O detector retorna `NO_DOCUMENT` quando nao ha evidencia suficiente; full-page e uma escolha explicita do usuario, nao um acerto artificial.

## Execucao

```powershell
gradlew.bat :core-common:testDebugUnitTest :core-data:connectedDebugAndroidTest
```

O teste conectado depende de aparelho/emulador. Resultados de dispositivo devem registrar modelo, Android, resolucao, quantidade de amostras e commit.
