# Ativos visuais da v0.3.1

## Decisão de identidade

A v0.3.1 preserva a raposa oficial já usada no app. A mesma fonte PNG de `scanora_mascot_welcome.png` alimenta `ic_launcher_fox.png` e `scanora_icon_source.png`; isso evita uma segunda personagem e mantém rosto, proporções, cores e lenço idênticos entre Home e launcher.

Validação do ativo aceito:

- raster PNG 768×768, `Format32bppArgb`;
- alfa do pixel do canto igual a 0;
- SHA-256 da fonte e do launcher: `EF9EECB5F98279FFD09FAC1616EF9E85812AB4F30C62BF64F88D20442DCF276F`;
- sem texto incorporado, fundo, marca d'água ou SVG geométrico.

## Geração assistida rejeitada

Foram testadas duas variações raster por geração de imagem local à sessão: uma fonte de ícone e uma pose de revisão. Ambas foram rejeitadas antes da entrega por inconsistência de personagem/anatomia; uma tentativa referenciada também produziu xadrez opaco (`Format24bppRgb`, alfa 255) em vez de transparência real. Nenhum desses três resultados permanece no repositório.

Na revisão, o app reutiliza `scanora_mascot_working.png`. A biblioteca continua com sete poses úteis — welcome, processing, working, OCR, empty, attention e success — sem duplicação ornamental.
