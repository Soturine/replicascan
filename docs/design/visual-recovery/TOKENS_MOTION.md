# Tokens e movimento — proposta de revisão

O tema atual usa `Color.kt`, `Theme.kt`, `Shape.kt`, `Type.kt` em `core-ui`.
Contrastes calculados do ciclo anterior não bastam para julgar composição.
Paleta final, fontes e texturas só serão congeladas após renders comparáveis.

Papéis: fundo de bancada, folha de leitura, superfície de controle, tinta forte,
texto secundário, terracota de ação, verde de conclusão e contorno de crop.
Claro/escuro preservam função e hierarquia; não dividir Pipa=dia/Lume=noite.
Validar pares de contraste usados de fato, incluindo erro, disabled e seleção.

Escala proposta: passos de espaço 4/8/12/16/24/32dp, bordas 8–24dp conforme
papel (documento é mais reto; botão mais macio), alvos mínimos 48dp, crop 64dp.
Headline deve ser curta e com quebra intencional; corpo base 16–18sp com
line-height generoso, sem esconder ação a 200%. Áreas roláveis e rodapés
respeitam IME e barras do sistema. Tokens só sobem a `core-ui` se usados por
mais de um fluxo.

Movimento: toque 90–140ms, revelar controle 180–240ms, transição de estado
220–320ms, settle da raposa finito. Trabalho pode ciclar com pausa discreta;
nenhum percentual é derivado de frames. `Animator duration scale=0` e tela
fora de RESUMED interrompem loops. Registrar medidas de jank/RAM/APK antes
de alegar desempenho. Uso de transformação deve evitar relayout em massa.

Qualificar as transições navegação→Review, filtro→preview, OCR→texto,
exportação→trabalho→resultado/erro, abertura de opção avançada, dialog e
pressão. A animação nunca marca documento salvo.
