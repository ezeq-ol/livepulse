# LivePulse 2.2 — Android local

Leia primeiro **NOVIDADES-2.2.md** para sons por canal, fotos reais e o novo visual. As melhorias anteriores e os guias de conexão estão em NOVIDADES-2.1.md.

## Instalar e usar

1. Transfira `LivePulse-2.2.0.apk` para o aparelho Android e abra o arquivo. Autorize a instalação por esse aplicativo de arquivos, se o Android solicitar.
2. Abra **LivePulse**, entre em **Integrações** e configure as credenciais pessoais de Kick, Twitch e/ou YouTube. Não há chaves de terceiros embutidas no APK.
3. Adicione o link do canal em **Visão geral**. Para uma plataforma personalizada, salve a API em **Outra plataforma**, selecione-a no radar e informe o identificador do canal.
4. Toque em **Ativar radar e som** e permita notificações.
5. No **Estúdio de som**, importe seu áudio (até 5 MB), escolha o volume e toque em **Testar notificação**.

Requisitos: Android 8.0 ou posterior, componente Android System WebView atualizado e acesso à internet para consultar as plataformas. Um único APK atende aparelhos ARM e x86 porque não há bibliotecas nativas específicas por arquitetura. APKs são para Android; não são executáveis de Windows ou iPhone.

## Sem servidor externo ou computador

A interface completa está dentro do APK. As consultas às APIs são feitas por código Java no próprio aparelho, com HTTPS. Não há servidor Node.js, localhost, site hospedado, login no LivePulse ou computador necessário. A interface, os ajustes, o histórico e o som de teste funcionam sem uma conexão com um servidor do LivePulse.

Internet continua necessária para buscar novas lives, vídeos e Shorts nas plataformas. Sem rede, os dados já salvos continuam disponíveis e as consultas exibem um erro de conexão. O aplicativo não consegue detectar conteúdo novo sem internet.

## Monitoramento em segundo plano

O botão **Ativar radar e som** inicia uma sessão de monitoramento local com notificação persistente. O intervalo padrão é de dois minutos e pode ser ajustado no app. **Pausar radar**, na interface ou na notificação persistente, para as consultas automáticas.

O Android pode interromper ou adiar a sessão por bateria, economia de energia, falta de rede ou limites de execução. Em Android 15 ou posterior, serviços de sincronização em primeiro plano de apps que miram API 35 têm limite de duração em segundo plano. Quando a sessão termina, o app usa o agendador do Android como modo econômico: intervalo mínimo de 15 minutos, sem horário exato garantido. Abrir o aplicativo e ativar o radar novamente inicia uma nova sessão permitida pelo sistema.

O agendamento é persistente entre reinicializações quando o radar está habilitado. Forçar a parada do aplicativo nas configurações do Android impede monitoramento até abri-lo novamente. Não há promessa de entrega instantânea ou funcionamento ininterrupto em qualquer fabricante. A tela do radar mostra se a sessão está ativa ou no modo econômico.

Modo silencioso, Não perturbe, volume de notificações e permissões do Android são respeitados. O volume no app ajusta o som dentro dos limites definidos no aparelho. Um som muito baixo no sistema não pode ser aumentado além desse limite pelo app.

## Integrações e YouTube

- Twitch: Client ID e Client Secret de aplicativo criado em https://dev.twitch.tv/console/apps. Usa OAuth client credentials e API Helix.
- Kick: Client ID e Client Secret do portal https://kick.com/settings/developer. Usa o endpoint oficial de canais com OAuth client credentials.
- YouTube: conexão da conta Google com autorização oficial ou chave de API com YouTube Data API v3 ativada no Google Cloud. Links individuais de vídeo/live/Short são convertidos para monitoramento do canal do criador.
- Outra plataforma: API HTTPS que responde JSON por GET, sem autenticação ou com Bearer. Configure URL com `{channel}`, caminho do status, valor ao vivo, ID único por transmissão, título e link opcional. Caminhos aceitam pontos e índices de arrays, como `data.0.id`.

As credenciais precisam ser válidas e as cotas das APIs precisam estar disponíveis. Não basta colocar qualquer link de página HTML em Outra plataforma; serviços com APIs diferentes exigem um adaptador próprio.

Lives do YouTube, agendamentos e replays são identificados pelos metadados oficiais. Shorts são confirmados quando seu ID aparece na aba pública Shorts do canal. A API não oferece um campo universal `isShort`; bloqueios, consentimento, paginação e mudanças na página podem impedir a confirmação. Nesses casos, o item fica como FORMATO A CONFIRMAR e pode ser corrigido no histórico. Não há garantia de classificação perfeita de Shorts.

São consultados até 25 uploads recentes e até 50 IDs incluindo transmissões pendentes. Conteúdo privado, não listado ou ausente da playlist pode não aparecer. A primeira consulta registra uploads antigos sem alertá-los e alerta lives já ativas. Eventos são deduplicados por ID e tipo e o histórico mantém até 300 alertas.

## Armazenamento e privacidade

Canais, configurações, histórico e credenciais são gravados em arquivo privado criptografado com AES-GCM. A chave fica no Android Keystore. Os áudios importados ficam em arquivos privados do aplicativo. O backup manual exclui contas e segredos; consulte NOVIDADES-2.1.md. Backups automáticos do app estão desativados para não separar dados criptografados de sua chave.

Nenhuma credencial real vem no código ou APK. O app envia as credenciais configuradas somente aos endpoints das integrações escolhidas. Ao configurar uma API personalizada, use uma URL confiável: o token Bearer será enviado para esse endpoint. A aplicação não oferece um serviço de nuvem próprio. Desinstalar ou limpar os dados do Android apaga as configurações locais.

## Código e compilação

Projeto Java/Android com bibliotecas oficiais Google/AndroidX empacotadas no APK. Somente a conexão Google precisa do Google Play Services no aparelho. Os arquivos HTML/CSS/JS em `app/src/main/assets` são empacotados no APK, e a ponte local chama `LocalApi.java`; não existe um servidor HTTP dentro do aplicativo.

Arquivos principais:

- `MainActivity.java`: interface local em WebView, seletor de áudio e permissões.
- `LocalApi.java`: operações de cadastro e preferências no dispositivo.
- `Monitor.java`, `MonitorService.java`, `MonitorJob.java`: consultas e agendamento local.
- `Rules.java`: validação de links, classificação e adaptação de APIs personalizadas.
- `Alerts.java`: notificações Android e áudio.
- `Store.java`: armazenamento privado e criptografia.

No Windows, tenha JDK 21, Android SDK Platform 35 e Build Tools 36.1.0. Abra `Compilar APK.cmd`, ou use `build.ps1 -SdkPath CAMINHO_DO_SDK -JdkPath CAMINHO_DO_JDK`. O resultado fica em `build/LivePulse-2.2.0.apk`. Nenhuma conta de nuvem é necessária para compilar. No ZIP de código, prepare as bibliotecas com python prepare_google.py antes de compilar.

O script gera uma chave privada de assinatura em `signing/` se ela ainda não existir. A cópia do projeto na Área de Trabalho preserva a chave usada nesta entrega. Guarde essa pasta com segurança e não a publique: ela é necessária para instalar futuras atualizações mantendo os dados. O ZIP público de código fornecido nesta entrega exclui a chave. Se compilar o ZIP com uma nova chave, será necessário desinstalar a versão anterior antes de instalar o novo APK.

## Validação desta entrega

- Compilação Java e geração DEX concluídas.
- Alinhamento do APK e assinaturas v2/v3 verificados com ferramentas oficiais do Android SDK.
- 70 verificações Java de links, classificação, lembretes, sons, backup, fotos de perfil e cache passaram.
- Layout conferido em navegador com tamanhos de celular e computador, sem erros no console.
- Não havia dispositivo Android ou imagem de emulador conectados/disponíveis para executar o APK. Instalação, permissões, reprodução nativa e comportamento de bateria precisam de validação no aparelho. Consultas reais também dependem das suas chaves de API.

Documentação: https://developer.android.com/develop/background-work/services/fgs/timeout e https://developer.android.com/develop/ui/compose/notifications/notification-permission.
