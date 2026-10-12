# Rig da raposa — gate artístico anterior ao código

`STATUS: MASTER E POSES APROVADOS; MOVIMENTO PENDENTE DE ACEITE`. O atlas
rejeitado saiu do runtime. O proprietário aprovou `candidates/master-v1.png`
como estilo e a prancha de sete poses como conjunto em 2026-10-12. O rosto e a
silhueta permanecem reconhecíveis entre poses; isso ainda não aprova o clipe
Android nem o acabamento das telas.

Referência visual indicada e aprovada: a própria raposa
v0.4.0. Manter rosto sorridente, volume macio, olhos castanhos, patinhas e
bandana turquesa reconhecíveis. O proprietário rejeitou explorações furry e
geométricas/chapadas; simplificar pelo e detalhes sem descaracterizar a
personagem original. A prancha `candidates/review.html` mostra a arte histórica,
o master aprovado e seis poses derivadas aprovadas como arte estática.

Para um rig articulado definitivo, camadas exigidas: cauda atrás, corpo/pernas, braço atrás, cabeça base com
orelhas/olhos/pálpebras/focinho, braço à frente apenas quando a ação pedir,
papel/pasta/efeitos mínimos. Pivôs no ombro, pescoço e base da cauda; bleed
suficiente para girar sem costura aparente. A folha não pode cruzar o nariz/olho
por acidente. Três cabeças incompatíveis causam pop; preferir uma cabeça com
expressões sobrepostas ou poses desenhadas com guia proporcional comum.

Gestos pretendidos: antecipação→ação→settle. Idle respira e pisca com pausa; scan segue a
folha; OCR acompanha linhas; export arquiva uma página e só celebra após arquivo
confirmado; erro mostra recuperação sem culpa. Pose estática de cada estado deve
continuar legível com movimento desativado. O candidato atual anima poses
inteiras e um traço na folha; não contém partes separadas nem piscar. Essa
limitação é explícita, não uma afirmação de rig concluído.

Comparar Rive, Lottie e Compose **depois** de existir arte mestre. Rive exige
`.riv` editável válido e runtime/licença avaliados; Lottie exige JSON e fonte
de autoria; Compose usa o stack atual, mas atlas sem registro impecável não
passa. Escolher por qualidade, manutenção, APK/RAM e lifecycle medidos, não por
nome de tecnologia. Sem dependência nova sem asset verdadeiro.

Gate de implementação: spritesheet de contato, dois frames consecutivos e
clipe Android idle→trabalho→sucesso/erro, inspecionados em tamanhos/contextos
claros/escuros e com `Animator scale=0`. Fonte editável e termos de uso da arte
registrados. A prancha e o clipe inicial existem em `candidates/` e `evidence/`;
os frames individuais ficam no diretório temporário do teste Android. Avaliação
de movimento, contextos e fonte em camadas permanece
pendente antes de chamar o rig de definitivo.
