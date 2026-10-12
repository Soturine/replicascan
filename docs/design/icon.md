# Símbolo do launcher

Explorações avaliadas: rosto sozinho (rejeitado por não comunicar documento),
raposa de corpo inteiro (detalhes perdem leitura em 48dp), rosto + folha dobrada
(selecionado). Símbolo vetorial original, distinto da ilustração detalhada do rig.
Orelhas arredondadas, focinho creme, tinta mineral e terracota mantêm a família.

Foreground 108dp; conteúdo identificável contido no círculo central de 66dp,
conforme https://developer.android.com/develop/ui/compose/system/icon_design_adaptive.
Fundo marfim separado. Monochrome usa silhueta vazada, sem bitmap colorido.
Fallback vetorial em mipmap-anydpi para API 24/25. Splash usa o mesmo foreground.
Não há tipografia pequena. Os XMLs são a fonte editável dos assets.

Máscaras e launcher devem ser registrados por captura Android na matriz visual;
os limites geométricos por si só não comprovam legibilidade de todas as máscaras.
