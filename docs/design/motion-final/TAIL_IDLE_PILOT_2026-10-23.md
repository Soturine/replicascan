# Piloto Idle/cauda — gate artístico reprovado (23/10/2026)

**Estado:** interrompido antes da integração. A raposa instalada permanece intacta. Este piloto não aprova motion, rig, APK ou release.

O PNG instalado `core-ui/src/main/res/drawable-nodpi/replicascan_fox_idle.png` e o master `docs/design/visual-recovery/candidates/master-v1.png` têm SHA-256 idêntico: `a5451933552a703d00df4457f962bf3f3b92a59497dba27930b09bc2c3075508`. A partir dessa pintura foi extraída somente a cauda visível, em canvas 1254 × 1254, com pivô **estimado**, não aprovado, em `(486,1040)`. A ordem experimental é cauda atrás, corpo/cabeça/cachecol/patas à frente. A máscara ocupa x=140–500, y=664–1166. Nenhuma cauda nova foi gerada.

O laboratório **local e reprovado** está em `C:\Users\rafael\.codex\scratch\replicascan-tail-pilot-20261023\index.html`. Ali há fonte editável `idle-tail-pilot-rejected.ora` (SHA-256 `cda1dd5350b33f7b049bf45be413545f9bb94a3bae818121e02fa3e82d0ed9ba`), PNGs das duas camadas, máscara, diff e overlay 50%, cinco fases de −4° a +4°, zoom da junção, quatro tamanhos (72/112/168/240 px CSS), quatro fundos e prévia GIF. `REPORT.md` e `manifest.json` explicam o método e os limites; `pilot_tail.py` e `render_pilot.py` reproduzem os resultados no mesmo diretório, a partir da raiz do repositório, com Python, Pillow, NumPy e OpenCV 4.10. A fonte ORA passou na verificação de integridade ZIP e contém duas camadas RGBA, mas **não foi aberta em Krita/GIMP**. O laboratório HTML teve suas referências verificadas; o CSS em px é uma aproximação para inspeção, não uma captura Android em dp.

Uma cópia transportável para revisão local está em `C:\Users\rafael\.codex\scratch\replicascan-tail-pilot-20261023\tail-idle-pilot-REJECTED-review.zip` (SHA-256 `34688eebffd665153369e5500a62f04aa75d4b04648f4afc27ac157a5ec01afd`). Ela inclui os arquivos referenciados pelo HTML e o roteiro `REPRODUCE.md`; não foi enviada à `main` por conter arte reprovada.

| Verificação | Resultado | O que comprova |
|---|---:|---|
| Remontagem neutra vs PNG instalado | 0 pixels visíveis diferentes; alfa idêntico; 1146 pixels de alfa zero com RGB diferente, máximo 7 por canal | Registro neutro, apenas |
| Contaminação do cachecol na cauda | 437 pixels teal na primeira máscara; 0 na segunda | O erro detectado não persiste na máscara candidata |
| Movimento na ROI da cauda | 131–135 mil pixels alterados em ±2°/±4°; controle de piscada: 0 | Há movimento fora dos olhos, não qualidade artística |
| Franja/contorno estático | Visível nos extremos, sobretudo no zoom `seam-plus4.png` | **FAIL**: cauda fantasma e matte sujo |
| Raiz/oclusão | Abertura junto ao quadril em +4°; ROI perde 2325 pixels opacos vs neutro | **FAIL**: falta pintura oculta |
| Animação candidata no Android >90 s | Não executada | Integração bloqueada pelo gate artístico |

A máscara preserva os pixels originais extraídos, porém deixa parte da borda branca/laranja antiga imóvel na camada do corpo. A raiz interna da cauda não existe sob o quadril; girá-la abre um buraco. **Não corrigir com escala, pivô ou deslocamento.** O handoff ao ilustrador é limpar a franja remanescente sem tocar rosto/cachecol/patas, pintar a continuação oculta da raiz e do quadril no mesmo arquivo, remontar o neutro e inspecionar todos os extremos em quatro tamanhos e fundos. Só uma fonte coerente e aprovada pode seguir para Compose. Cabeça/cachecol, patas e documento continuam fases separadas.

Os testes instrumentados de Export/Review/matriz de telas existentes não foram alterados. Seus oráculos verificam conteúdo decodificado/visível ou produzem capturas para revisão, não aprovam anatomia. Não há mudança de runtime, Room, OCR, exportação, localização, botão 17 sp ou `release/manifest.json`; `publish` continua `false`. Não foi criado APK ou vídeo Android deste candidato, nem tag/release v0.4.1.
