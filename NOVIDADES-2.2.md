# LivePulse 2.2

## Som de cada canal na Visão geral

Cada cartão agora mostra **Som da notificação**, o nome do áudio ou a indicação de sons por evento, um botão de teste e **Som do canal**. Esse botão abre o Estúdio de som já com o canal selecionado. Deixe **Todos os eventos** para que o som seja usado nas notificações desse canal e importe seu arquivo.

A ordem continua sendo canal + evento → canal → evento → som geral → Pulse original. O teste do cartão simula uma notificação de live; o teste do Estúdio usa a combinação selecionada. Os sons continuam no backup. Arquivos aceitos dependem do decodificador do aparelho/navegador; máximo de 5 MB por áudio e 64 regras.

## Fotos reais

Twitch usa Get Users e o campo profile_image_url. YouTube usa a foto de snippet.thumbnails do próprio canal, e não a miniatura de um vídeo. Kick consulta o ID do dono do canal e busca profile_picture no endpoint de usuários, validando que a resposta corresponde ao usuário solicitado. As imagens são buscadas em segundo plano e mantidas localmente para uso sem rede. Fotos são atualizadas após 24 horas; falhas são tentadas novamente após 15 minutos. O botão **Foto ↻** solicita uma atualização na próxima consulta.

É necessário que a integração esteja configurada e que a plataforma forneça uma URL de foto acessível. Na Kick, contas conectadas pela versão anterior podem precisar ser reconectadas para autorizar o escopo user:read. A configuração manual por credenciais de aplicativo também pode consultar fotos públicas conforme as permissões da API.

Integrações personalizadas podem preencher **Caminho da foto de perfil**, como data.user.profile_picture, se a resposta JSON da API contiver esse campo. Sem foto disponível, aparecem as iniciais do nome e uma indicação de identificação provisória. Não são usados avatares aleatórios nem banners como se fossem a foto do criador. Uma falha de foto não impede detectar lives.

No Android, o arquivo da foto fica no armazenamento privado e é apresentado à interface por uma rota interna de assets. Não existe servidor HTTP ou Node.js no APK. A versão para computador usa seu servidor local existente. Imagens públicas externas são baixadas sem enviar o token de autenticação para o servidor de imagens. HTTPS, limites de tamanho, tempo e formato são aplicados.

## Visual e carregamento

Cartões com foto, identificação da plataforma, status e controles separados. Filtros do YouTube ficam em uma seção expansível. O estado dos filtros abertos é preservado nas atualizações do cartão.

Tela inicial de carregamento até a primeira leitura dos dados; cartões provisórios durante essa leitura; spinner e faixa animada enquanto a consulta está pendente; feedback ao salvar; transições curtas de navegação e destaque ao abrir o som do canal. São estados de operação, sem porcentagens de progresso inventadas. A preferência de reduzir movimento é respeitada. No Android, as operações da interface passaram para uma ponte assíncrona para evitar bloquear a tela durante leituras e gravações locais.

## Atualização

Instale LivePulse-2.2.0.apk por cima da versão anterior. O pacote e a chave de assinatura são os mesmos. Canais, contas e sons existentes são preservados. Na Área de Trabalho, o projeto Android está em LivePulse/Android e o APK está também na pasta principal LivePulse.

A versão local para Windows também recebeu cartões, fotos, carregamento e sons por canal. Para carregar o código atualizado no computador, use Parar LivePulse.cmd e depois Iniciar LivePulse.cmd. Ela continua precisando do servidor local; o APK funciona diretamente no Android.

## Validação

70 verificações Java aprovadas, incluindo 17 novas verificações de extração de fotos, correspondência de usuário, HTTPS, política de cache e seleção de som por canal. O projeto Windows passou em 8 testes, além de verificação HTTP de cadastro, importação de áudio WAV real, escolha por canal, remoção da regra e arquivos da interface. Layout de celular, acesso Som do canal e estados de verificação foram conferidos em navegador com dados de teste; esses dados não estão no APK.

Compilação e assinatura do APK verificadas. A execução em um aparelho Android e consultas de fotos com suas credenciais reais continuam pendentes. Fotos dependem da disponibilidade e das permissões da plataforma. As limitações anteriores de YouTube, cotas, internet e execução em segundo plano continuam valendo.

Fontes dos campos de perfil: https://dev.twitch.tv/docs/api/reference/#get-users, https://developers.google.com/youtube/data/reference/rest/v3/channels e https://api.kick.com/swagger/doc.yaml.
