# Identificadores públicos do LivePulse 2.4.4

Edite app/src/main/java/com/livepulse/app/PlatformConfig.java para configurar os Client IDs públicos de Twitch e Kick. Ambos já estão preenchidos. Credenciais existentes no dispositivo são preservadas.

Google Android: cadastro OAuth com pacote com.livepulse.app e certificado de assinatura do APK. GOOGLE_ANDROID_CLIENT_ID é referência administrativa; o fluxo Google Identity Android utiliza pacote e assinatura, sem Secret. O projeto Google está em testes e requer contas na lista de usuários de teste.

Não inclua Client Secrets no código ou APK. A Kick continua exigindo Secret configurado privadamente por instalação no fluxo local atual. Conexão simplificada sem compartilhar Secret exige serviço de autenticação. A API Key do YouTube continua no campo Integrações do app e não foi embutida.

Validação: compilação e assinatura do APK; teste de preservação das credenciais (5 verificações). Login não testado em dispositivo Android.
