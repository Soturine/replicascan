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

Capturas Android em [evidence/launcher.png](evidence/launcher.png) e
[evidence/launcher-masks.png](evidence/launcher-masks.png): launcher instalado,
círculo e quadrado arredondado em 48/72/108dp. Inspeção sem corte do símbolo.
Outros launchers OEM e a aparência de ícones temáticos permanecem pendentes.
