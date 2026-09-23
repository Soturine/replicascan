# Estado atual do ReplicaScan

O ReplicaScan `0.4.0` (`versionCode 19`, `com.soturine.replicascan`) é um scanner Android local-first. **Escanear** abre o ML Kit Document Scanner; **Importar** usa o seletor de fotos do sistema; se o scanner não estiver disponível, a câmera do sistema e o crop manual servem de alternativa. Não há conta, backend, sincronização ou envio de documentos pelo app.

## Fluxo do produto

1. As páginas do scanner, da galeria ou da câmera do sistema são copiadas para `filesDir/scan-sources` antes de qualquer registro; falhas desfazem a cópia.
2. Room cria o documento; a Revisão mostra a página em destaque com Cortar, Girar, Ajustar e Texto.
3. O crop manual começa na página inteira e nunca tenta adivinhar bordas.
4. O texto é reconhecido no aparelho uma vez por página e reaproveitado; outro idioma pode ser escolhido nas opções avançadas.
5. O texto alimenta a busca FTS e a camada pesquisável do PDF.
6. PDF, JPG e PNG são gerados a partir da fonte privada, sem arquivos parciais em caso de falha.

## Experiência e idiomas

- tema claro/escuro em creme, navy, laranja e teal; raposa apenas em boas-vindas, vazio, processamento e sucesso;
- ferramentas com ícone e rótulo visível; ações destrutivas pedem confirmação;
- Revisão com painel lateral em janelas largas/paisagem;
- 12 idiomas com o mesmo glossário; árabe com RTL (o crop usa coordenadas físicas porque a imagem não é espelhada);
- as traduções foram reescritas nesta versão e ainda **não** passaram por revisão de falantes nativos.

## Limitações atuais

- o scanner depende do Google Play services e de ≥ 1,7 GB de RAM; sem ele, a alternativa é foto + crop manual;
- OCR depende de modelos baixados pelo Google Play services na primeira utilização; OCR árabe não é suportado;
- a camada pesquisável do PDF é texto invisível agrupado, sem posição por palavra, e precisa de validação em leitores externos;
- a automação de API 36 roda no GitHub em dispositivo gerenciado; QA físico (câmera, TalkBack, fonte 200%, RTL, corpus de documentos e OCR) está pendente;
- o APK publicado no GitHub é de avaliação (debug), não uma build de Play Store; R8 e assinatura de produção ficam para o endurecimento de Play.
