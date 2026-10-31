# LivePulse 2.1 — suas seis melhorias

1. **Conectar contas por botão.** Integrações inclui Conectar Twitch, Conectar Kick e Conectar YouTube, configuração guiada e Desconectar. Twitch usa Device Code Flow; Kick usa autorização com PKCE e retorno ao aplicativo; Google usa AuthorizationClient oficial do Google Play Services. Como você ainda não cadastrou aplicativos, os guias explicam os cadastros necessários. O app não inventa credenciais e não pede sua senha de conta. A autorização real depende de você concluir o cadastro nos portais e aceitar a tela de consentimento da plataforma.
2. **Lembretes de um minuto até encerrar.** Novos alertas entram como não vistos. Use Já vi, Assistir ou Já vi todos para encerrar. Deslizar a notificação não encerra a repetição. A opção Lembretes ativos desliga a repetição geral. O mesmo alerta é atualizado em vez de criar várias notificações iguais. Alertas antigos da versão 2.0 não começam a repetir após atualizar. O Android pode atrasar os alarmes em repouso ou por economia de bateria: a repetição exata de 60 segundos não é garantida com a tela bloqueada. Não solicitamos permissão especial de alarmes exatos nem ignoramos Não perturbe.
3. **Som por canal e evento.** No Estúdio de som, escolha canal e tipo antes de importar. Ordem de escolha: canal + evento, canal, evento, geral, Pulse original. O botão de teste usa essa mesma ordem. Máximo de 5 MB por áudio e 64 regras. Sons simultâneos entram em fila. Marcar um alerta como visto cancela seu áudio pendente.
4. **Backup e restauração.** A nova aba Backup exporta canais, preferências, histórico e áudios para um arquivo escolhido por você. Tokens, Client Secrets e chaves de API não são exportados. A restauração valida o arquivo e pede confirmação; o radar e os lembretes restaurados ficam pausados. Contas e credenciais já existentes no aparelho são mantidas. Para outro aparelho, conecte as contas novamente. O arquivo de backup não é criptografado: guarde-o em local de confiança. Limite de 25 MB de áudios no backup, 200 canais, 100 integrações e 300 eventos.
5. **Notificações mais completas.** Título, canal, plataforma, tipo, categoria, audiência, duração e agendamento aparecem quando disponíveis na API. Imagens são carregadas em segundo plano com limites de tamanho e tempo; se a imagem falhar, o alerta de texto continua. Botões Assistir e Já vi encerram os lembretes. Testes de som não entram na fila de lembretes nem no histórico.
6. **YouTube com confirmação de formato.** A API identifica transmissões e seus estados; as abas públicas Shorts e Vídeos confirmam o formato dos uploads. Quando a confirmação não está disponível, aparece FORMATO A CONFIRMAR. O app não afirma que um vídeo é Short apenas por ser curto. A classificação pode ser corrigida no histórico e uma confirmação posterior atualiza um item incerto sem gerar um novo alerta duplicado. Premières e lives agendadas podem compartilhar metadados: a descrição deixa essa limitação explícita.

## Atualizar sem perder dados

Instale `LivePulse-2.1.0.apk` por cima da versão 2.0, sem desinstalar. O pacote (`com.livepulse.app`) e a chave de assinatura foram mantidos. A atualização preserva o arquivo criptografado, canais e som geral existentes. Os lembretes só são criados para novos alertas.

## Cadastro guiado das contas

- Twitch: abra Configuração guiada, registre um aplicativo público para Device Code Flow no console Twitch, salve Client ID e toque Conectar. Autorize no navegador. O app pode mostrar um código de ativação e aguarda a autorização enquanto permanece em execução.
- Kick: registre um aplicativo, configure exatamente `livepulse://oauth/kick` como retorno, salve Client ID e Client Secret e toque Conectar. Se o portal não aceitar o retorno ou não liberar sua aplicação, a configuração por API de dados públicos continua disponível. O Client Secret usado é o seu, salvo de forma criptografada no seu aparelho, e não é embutido no APK compartilhado.
- YouTube: no Google Cloud ative YouTube Data API v3, configure a tela de consentimento e crie um OAuth Client Android. O guia dentro do aplicativo mostra pacote e SHA-1 reais da instalação e permite copiar as instruções. Adicione sua conta como testadora enquanto o projeto estiver em teste. Depois, toque Conectar e escolha a conta pelo diálogo oficial Google. Precisa de Google Play Services. Uma chave de API continua sendo alternativa para monitorar dados públicos sem conectar a conta.

Nenhum desses cadastros foi criado automaticamente na sua conta. Não envie senhas ou Client Secrets pelo chat. A autorização acontece no navegador da plataforma ou na tela oficial do Google.

## Validação e limites

Compilação Java/DEX, alinhamento e assinatura v2/v3 concluídos. Passaram 53 verificações Java: 26 de regras anteriores e 27 de lembretes, prioridade de sons, backup e classificação de YouTube. Fluxos visuais de confirmação de leitura, correção de tipo, seleção de canal/evento e navegação de Backup foram verificados com dados simulados usados apenas nos testes; esses dados não estão no APK.

Não há aparelho Android conectado nem cadastro OAuth de produção fornecido. Reprodução nativa, consentimento real das contas, lembretes em repouso e importação/exportação pelo seletor do Android precisam de teste no aparelho. A assinatura válida comprova a integridade do pacote; não substitui esse teste. A versão continua local e independente de servidor LivePulse, mas consultas e conexões de conta precisam de internet.

## Compilar

JDK 21, Android SDK Platform 35 e Build Tools 36.1.0. As bibliotecas Google/AndroidX ficam empacotadas no APK. Para preparar o código-fonte do ZIP, execute `python prepare_google.py` (Python 3 e internet); as bibliotecas vêm dos repositórios oficiais Google Maven e Maven Central e são conferidas com `dependencies.lock.json`. Em seguida, use `Compilar APK.cmd`. Na cópia da Área de Trabalho, as dependências já estão preparadas.

Preserve `signing/` na pasta do projeto da Área de Trabalho para futuras atualizações. Essa pasta privada não é incluída no ZIP de código. As licenças e versões das dependências estão em THIRD-PARTY-NOTICES.txt.

Documentação: [Twitch OAuth](https://dev.twitch.tv/docs/authentication/getting-tokens-oauth/), [Kick OAuth](https://docs.kick.com/getting-started/generating-tokens-oauth2-flow), [Google AuthorizationClient](https://developers.google.com/android/reference/com/google/android/gms/auth/api/identity/AuthorizationClient), [restrições de serviços Android](https://developer.android.com/develop/background-work/services/fgs/timeout).
