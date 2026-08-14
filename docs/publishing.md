# Publicação

## GitHub Pages

O site estático fica em `site/` e o workflow `pages.yml` publica esse diretório no GitHub Pages.

Para o deploy funcionar no repositório publicado:

1. abra `Settings > Pages`;
2. selecione `Source: GitHub Actions`;
3. mantenha o workflow `pages.yml` habilitado na branch principal.

## GitHub Releases

Recomendações:

- use tags seguindo SemVer;
- gere notas de release com base no `CHANGELOG.md`;
- publique APK ou AAB apenas quando o pipeline de release estiver estabilizado.
- quando um APK debug for anexado apenas para avaliação, identifique-o claramente como não assinado para loja e publique seu SHA-256.

## Play Store

Checklist resumido:

1. Revisar nome final, ícone e screenshots.
2. Validar a política de privacidade publicada.
3. Revisar permissões solicitadas.
4. Confirmar `targetSdk` e políticas do Google Play.
5. Gerar `AAB` de release.
6. Testar fluxo em dispositivos reais.

## Assinatura e release

Itens ainda não automatizados no MVP:

- assinatura de release;
- pipeline de Play Console;
- screenshots reais finais;
- hardening completo de minificação e obfuscation.
