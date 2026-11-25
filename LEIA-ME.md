# Saborey Garçom — app Android (maquininha Stone)

O app abre o app do garçom (https://garcom.ecodados.app.br/garcom) e cobra no app de pagamento da Stone
pelo Deeplink (`payment-app://pay`). A resposta volta por `saboreygarcom://pay-response`.

## Gerar o APK de teste (Android Studio)
1. Abra o Android Studio → **Open** → escolha esta pasta (`SaboreyGarcom`).
2. Espere o **Gradle Sync** terminar (na primeira vez baixa as ferramentas; precisa de internet).
3. Menu **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
4. O APK fica em `app/build/outputs/apk/debug/app-debug.apk`.
5. Para testar num celular Android comum: copie o APK para o celular e abra (permita "instalar apps desconhecidos").
   Num celular comum, a cobrança avisa que o app da Stone não está no aparelho — o resto (mesas, pedidos) funciona.

## APK assinado (para mandar para a Stone)
- **Build → Generate Signed App Bundle / APK → APK → Create new...** e crie a chave (`.jks`).
- **Guarde a chave e as senhas em lugar seguro.** Toda atualização precisa ser assinada com a MESMA chave.
- A cada versão nova: aumente `versionCode` (1, 2, 3...) e `versionName` (1.0.0, 1.0.1...) em `app/build.gradle.kts`.

## Regras da Stone já atendidas
- Pacote `br.com.saborey.garcom.app`; Android 6.0+ (minSdk 23); versionName só números e pontos.
- Só HTTPS (tráfego sem criptografia bloqueado); tela "Fale conosco" (botão Ajuda no app).
- Endereço externo para a lista liberada (whitelist) da Stone: `https://garcom.ecodados.app.br`.

## Onde mexer
- Endereço do sistema: `URL_APP` em `app/build.gradle.kts`.
- Cobrança / resposta da Stone: `MainActivity.kt` (`iniciarCobranca` e `onNewIntent`).
