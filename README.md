# LivePulse para Android

Radar de canais Twitch, Kick, YouTube e integrações JSON personalizadas. Executa no dispositivo, sem servidor próprio. As consultas às plataformas e a atualização opcional precisam de internet.

## Funcionalidades

- Alertas de transmissões, histórico e lembretes até marcar como visto.
- Sons por canal ou evento, importação de áudio e botão de teste.
- Foto real do canal quando disponível e ícone da plataforma nas notificações.
- Identificação de lives, vídeos e Shorts no YouTube, com indicação de classificação não confirmada e correção manual.
- Conexão de contas com configuração guiada e exportação/restauração de backup.
- Atualizações por feed HTTPS configurável, download verificado e instalação confirmada pelo Android.

## Instalação e requisitos

Android 8.0/API 26 ou superior. A versão atual é 2.4.2, código 20402. Autorize notificações para receber avisos. Contas, APIs e cotas dependem dos provedores. Há limitações de execução em segundo plano impostas pelo Android.

Quando uma distribuição oficial for publicada, o APK e seu SHA-256 deverão ser baixados do Release correspondente. A atualização deve usar a mesma assinatura e ser instalada por cima da anterior; desinstalar pode apagar dados. Não há URL de publicação configurada no APK entregue atualmente.

## Compilar

Requer JDK 21, Python, Android SDK Platform 35 e Build Tools 36.1.0. Execute `python prepare_google.py` para preparar as dependências e depois `powershell -ExecutionPolicy Bypass -File build.ps1`. Consulte LEIA-ME-ANDROID.md e ATUALIZACOES.md. O build pode gerar uma chave para uma instalação nova; somente a chave privada original permite atualizar as instalações já existentes.

A chave original, senhas, credenciais e dados de usuários nunca devem entrar no Git. As pastas signing, build e dependencies foram excluídas desta preparação. Preserve uma cópia privada da chave original fora do repositório.

## Documentação

- [Termos de uso](TERMS.md)
- [Política de privacidade](PRIVACY.md)
- [Direitos e licença](LICENSE.md)
- [Atualizações](ATUALIZACOES.md)
- [Validação 2.3](VALIDACAO-2.3.txt)
- [Dependências e atribuições](THIRD-PARTY-NOTICES.txt)

162 verificações Java aprovadas na entrega 2.4.2; interface revisada com bridge simulada. A página HTTPS e o download público foram verificados. Não foi executada instalação em aparelho físico conectado nem concluída uma autorização real Kick no APK. Não há afiliação oficial com Twitch, Kick ou YouTube.

Responsável: **by SAD_ZECKXS**. Contato: **sounoob773@gmail.com**. Publicar no GitHub não registra oficialmente o programa no INPI nem registra a marca LivePulse.

## Download e atualização

[Baixar LivePulse 2.4.2](https://github.com/ezeq-ol/livepulse/releases/download/v2.4.2/LivePulse-2.4.2.apk) · [Releases](https://github.com/ezeq-ol/livepulse/releases)

Para ativar as consultas de novas versões no APK, abra Atualizações e salve:

`https://raw.githubusercontent.com/ezeq-ol/livepulse/main/updates.json`

A versão 2.4.2 já instalada não anunciará a si mesma como atualização: o aviso aparece quando for publicada uma versão compatível com versionCode maior. O APK entregue antes desta publicação exige configurar a URL uma vez. Os documentos de termos e privacidade estão publicados neste repositório e ainda não aparecem dentro da interface do APK 2.4.2.


## Suporte

Contato público: sounoob773@gmail.com. Para relatar problemas, descreva a versão do Android e do LivePulse e os passos para reproduzir. Não inclua senhas, tokens, credenciais ou backups nos Issues.

Código visível publicamente não significa autorização geral para redistribuição ou exploração comercial. Nenhuma licença de código aberto foi escolhida pelo responsável nesta preparação.

## Assistir nos apps das plataformas

A versão 2.4.2 prioriza os apps oficiais Twitch, Kick e YouTube ao tocar em Assistir, tanto na notificação como no histórico. O link original do canal ou conteúdo é preservado. Se o app não estiver instalado, estiver desativado ou não tiver uma atividade para atender ao link, o Android abre o link pela alternativa HTTPS normal, geralmente o navegador padrão. Outras integrações continuam usando HTTPS.

## Escolher o som de cada notificação

Na Visão geral, toque em Editar som junto do áudio de um canal. No Estúdio, selecione o canal e o tipo de evento, escolha um arquivo, toque em Testar som e depois em Salvar mudanças. Só salvar aplica o áudio; escolher e testar mantêm os alertas atuais. Cada combinação tem seu próprio rascunho durante esta abertura do app. Descartar mudanças restaura o som salvo. Usar som herdado/original também exige salvar para remover a regra específica. O volume continua sendo uma preferência geral do app.

Após Salvar mudanças, o Estúdio recarrega os dados salvos e mostra o botão Alterações salvas com sucesso. A seleção do arquivo é limpa. Ao selecionar outro canal/evento ou escolher outro áudio, os controles ficam prontos para uma nova configuração.

## Conexão Kick com retorno HTTPS (2.4.2)

Cadastre exatamente `https://ezeq-ol.github.io/livepulse/kick-callback.html` como retorno no portal Kick, com leitura de usuário e canal e webhooks desligados. No LivePulse, salve seu Client ID e Client Secret em Integrações. Toque em Conectar Kick, autorize no navegador do mesmo celular e depois toque em Continuar no LivePulse na página HTTPS. O APK valida o pedido e troca o código diretamente com a Kick. Não configure `https://livepulse://oauth/kick`: esse endereço não é válido para este fluxo. A página é estática, publicada pelo GitHub Pages a partir de `/docs`; não existe servidor de troca de tokens no projeto. Consulte a política de privacidade para o trânsito do código temporário pela hospedagem.

Validação da página de retorno: `node tests/kick-callback.test.cjs`. O teste usa apenas valores fictícios.
