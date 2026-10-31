# LivePulse 2.3 — atualizações do APK

O aplicativo continua funcionando localmente, sem servidor próprio. As consultas às plataformas e o serviço opcional de atualização precisam de internet. Nenhum endereço de publicação vem configurado nesta versão.

## Usar agora

Instale LivePulse-2.3.0.apk por cima da versão anterior, sem desinstalar: o pacote e a assinatura foram preservados. Seus canais, sons e histórico permanecem no aparelho. Os alertas individuais usam o símbolo de Twitch, Kick, YouTube ou uma tela para integração personalizada. A notificação persistente do radar mantém o símbolo do LivePulse. O Android controla o fundo, a cor e o formato final dos ícones; alguns aparelhos mostram símbolos monocromáticos.

Na aba **Atualizações**, deixe o endereço vazio enquanto não tiver uma publicação. Nenhuma consulta remota é feita sem URL. O radar de canais funciona independentemente disso.

## Publicar a próxima versão

1. Aumente `versionCode` e `versionName` no AndroidManifest.xml e o nome de saída no build.ps1. Compile e assine usando **a mesma chave** da versão instalada. Preserve a pasta privada `signing` e sua senha; nunca publique esses arquivos.
2. Hospede o APK assinado em uma URL HTTPS pública que permita download direto. Pode usar um site ou um arquivo anexado a um GitHub Release.
3. Gere `updates.json` com os dados reais do arquivo. Exemplo de comando, substituindo os endereços abaixo pelos seus:

```powershell
python make_update.py --apk build/LivePulse-2.4.0.apk --apk-url https://SEU-DOMINIO/LivePulse-2.4.0.apk --output publicacao/updates.json --notes "Novidades desta versão."
```

O comando lê o pacote, a versão e o Android mínimo diretamente do APK via aapt2 do Android SDK. Calcula o SHA-256 e o tamanho real, e não publica nada. `--aapt2` permite indicar outro caminho para essa ferramenta. O nome e a versão mostrados são exemplos de uma publicação futura; não há uma versão 2.4 entregue neste pacote.

4. Hospede `updates.json` em HTTPS, retornando JSON diretamente, sem login ou página HTML. No GitHub, use a URL **Raw** de um arquivo de repositório público, ou o download direto de um anexo com nome estável em Releases; não use a página `/releases` ou `/blob/...` como endereço do feed. Atualize o mesmo endereço em cada nova publicação. Recomenda-se cache curto no servidor.
5. Em **Atualizações**, salve a URL pública do JSON e toque em **Verificar agora**. Os usuários precisam configurar essa URL uma vez. Para APKs futuros já configurados, preencha `feedUrl` em `app/src/main/assets/update-config.json` antes de compilar; isso serve como padrão para instalações novas, sem substituir a escolha salva de usuários existentes.

Com endereço e verificações automáticas ativos, o aplicativo consulta ao abrir quando a última verificação tem pelo menos 15 minutos e agenda consultas periódicas de 15 minutos. O Android pode adiá-las por bateria, falta de rede, restrições do fabricante ou encerramento forçado. O botão Verificar agora faz uma consulta manual mesmo com as verificações automáticas desligadas. Não é atualização por push nem uma promessa de aviso instantâneo.

Uma versão com código maior e Android mínimo compatível mostra **“Existe uma atualização disponível”** no app e uma notificação do sistema, se as notificações estiverem permitidas. A mesma versão não é anunciada repetidamente. Falhas de conexão são exibidas e não interrompem o radar.

O botão Baixar recebe o APK no cache privado com progresso. Antes de oferecer a instalação, verifica tamanho, SHA-256, pacote, versão, Android mínimo e a assinatura comparada ao app instalado. Alteração de chave de assinatura não é aceita. O Android confirma a instalação; caso necessário, pede a permissão para o LivePulse abrir o instalador. Nenhuma instalação é silenciosa. Arquivos de cache podem ser removidos pelo Android: nesse caso, baixe novamente.

Esta distribuição é um APK instalado diretamente. Uma eventual distribuição pela Google Play exigirá adequar o mecanismo de atualização às regras da loja.

## Validação desta entrega

APK compilado e assinatura verificada com apksigner. Testes Java cobrem política de versão, feed, compatibilidade, checksum e seleção de ícones. Não houve teste de instalação em aparelho conectado nem publicação remota real, pois ainda não existe um endereço de hospedagem.

Referências: [notificações Android](https://developer.android.com/design/ui/mobile/guides/home-screen/notifications), [permissão do instalador](https://developer.android.com/reference/android/content/pm/PackageManager#canRequestPackageInstalls()), [FileProvider](https://developer.android.com/reference/androidx/core/content/FileProvider).
