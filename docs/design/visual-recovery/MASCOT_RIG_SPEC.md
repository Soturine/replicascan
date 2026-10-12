# Rig da raposa — gate artístico anterior ao código

`STATUS: MASTER APROVADO COMO ESTILO; POSES E RUNTIME PENDENTES`. Atlas atual é
baseline rejeitado. O proprietário aprovou `candidates/master-v1.png` como
direção em 2026-10-12. Antes de substituir runtime, obter aceite visual
expresso das poses idle, greeting, reading, working, success, error e empty.
O rosto e a silhueta devem continuar reconhecíveis entre poses.

Referência visual indicada, ainda sem master aprovado: a própria raposa
v0.4.0. Manter rosto sorridente, volume macio, olhos castanhos, patinhas e
bandana turquesa reconhecíveis. O proprietário rejeitou explorações furry e
geométricas/chapadas; simplificar pelo e detalhes sem descaracterizar a
personagem original. A prancha `candidates/review.html` mostra a arte histórica,
o master aprovado e seis poses derivadas ainda pendentes de aceite.

Camadas exigidas: cauda atrás, corpo/pernas, braço atrás, cabeça base com
orelhas/olhos/pálpebras/focinho, braço à frente apenas quando a ação pedir,
papel/pasta/efeitos mínimos. Pivôs no ombro, pescoço e base da cauda; bleed
suficiente para girar sem costura aparente. A folha não pode cruzar o nariz/olho
por acidente. Três cabeças incompatíveis causam pop; preferir uma cabeça com
expressões sobrepostas ou poses desenhadas com guia proporcional comum.

Gestos: antecipação→ação→settle. Idle respira e pisca com pausa; scan segue a
folha; OCR acompanha linhas; export arquiva uma página e só celebra após arquivo
confirmado; erro mostra recuperação sem culpa. Pose estática de cada estado deve
continuar legível com movimento desativado.

Comparar Rive, Lottie e Compose **depois** de existir arte mestre. Rive exige
`.riv` editável válido e runtime/licença avaliados; Lottie exige JSON e fonte
de autoria; Compose usa o stack atual, mas atlas sem registro impecável não
passa. Escolher por qualidade, manutenção, APK/RAM e lifecycle medidos, não por
nome de tecnologia. Sem dependência nova sem asset verdadeiro.

Gate de implementação: spritesheet de contato, dois frames consecutivos e
clipe Android idle→trabalho→sucesso/erro, inspecionados em tamanhos/contextos
claros/escuros e com `Animator scale=0`. Fonte editável e termos de uso da arte
registrados. Arte final e runtime são pendentes até inspeção e aceite do dono.
