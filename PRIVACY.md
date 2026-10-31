# Política de privacidade do LivePulse

Última atualização: 31 de outubro de 2026. Refere-se à implementação Android 2.4.2.

Responsável pela distribuição: **by SAD_ZECKXS**. Contato de privacidade e suporte: **sounoob773@gmail.com**.

## 1. Informações no dispositivo

O aplicativo mantém canais monitorados, nomes e links, preferências, histórico de alertas, lembretes, informações públicas dos conteúdos, fotos em cache e sons escolhidos pelo usuário. Quando há integração autenticada, também utiliza Client IDs, chaves de API, segredos e tokens necessários às funcionalidades autorizadas. A autorização YouTube solicita acesso de leitura; Twitch e Kick seguem os fluxos e permissões implementados nas integrações.

O estado e as credenciais são armazenados criptografados no espaço privado do app, com chave gerenciada pelo Android Keystore. Fotos, sons e APKs baixados podem permanecer como arquivos no espaço privado ou cache; isso não significa que todos esses arquivos sejam criptografados individualmente. O aplicativo não pede que a senha da plataforma seja enviada ao responsável pelo LivePulse.

## 2. Conexões externas

As consultas saem diretamente do dispositivo para Twitch, Kick, YouTube/Google, provedores das fotos e miniaturas e eventuais integrações personalizadas. Esses serviços recebem o que for necessário à requisição, como IP, dados técnicos, identificadores de canal e credenciais de autorização pertinentes. O acesso segue as políticas e condições de cada destinatário.

Quando uma URL de atualização é configurada, o app consulta o servidor do arquivo JSON e baixa o APK do endereço informado pela publicação, podendo seguir redirecionamentos HTTPS. Esses provedores recebem dados técnicos da conexão, como IP e registros de acesso, conforme suas políticas. Sem URL configurada, não faz consultas de atualização. Se a distribuição ocorrer pelo GitHub, o GitHub processa os acessos de acordo com sua própria política.

Integrações personalizadas enviam consultas ao endpoint escolhido pelo usuário e podem incluir o token Bearer fornecido. Avalie o serviço antes de configurá-lo.

## 3. Finalidades

As informações são usadas para consultar conteúdo, evitar avisos duplicados, identificar os canais, tocar os sons, apresentar notificações, manter lembretes, autenticar integrações, exportar/restaurar backups e verificar/baixar atualizações. A implementação revisada não inclui anúncios, ferramentas próprias de análise de uso ou envio automático do histórico para um servidor do responsável. Bibliotecas e serviços das plataformas podem tratar dados segundo suas próprias políticas; não se presume ausência de registros nos serviços externos.

## 4. Permissões e exposição

Notificações podem revelar nomes de canais e títulos na tela bloqueada; você controla a exibição nas configurações do Android. O seletor de arquivos permite escolher áudio e o destino/origem de backups sem acesso irrestrito a todos os arquivos. A permissão de instalação por esta fonte só é solicitada para abrir o instalador de atualização; não autoriza instalação silenciosa. As restrições e permissões do sistema continuam aplicáveis.

## 5. Backup, retenção e exclusão

Backups exportados incluem configurações, canais, histórico e sons, removendo credenciais, tokens de conta e tokens Bearer das integrações na exportação atual. São arquivos JSON legíveis, com áudio incorporado, e não recebem criptografia própria; após exportá-los, seu armazenamento e compartilhamento ficam sob controle do usuário e de serviços que escolher utilizar.

Dados locais permanecem até serem removidos pelas funções disponíveis, limpeza dos dados ou desinstalação. Arquivos de cache podem ser excluídos pelo Android. Desconectar uma conta remove a conexão local e tenta revogar o acesso remoto; sem rede ou em caso de falha, faça também a revogação nas configurações da plataforma. Apagar dados locais não apaga automaticamente backups exportados ou registros mantidos por terceiros.

## 6. Solicitações e direitos

Você pode gerenciar canais, limpar o histórico, desconectar contas e controlar notificações e permissões. Para esclarecimentos ou exercício de direitos aplicáveis sobre dados eventualmente recebidos pelo responsável em pedidos de suporte, utilize o contato acima. Não envie senhas, tokens ou chaves de assinatura. Pedidos de suporte enviados voluntariamente serão utilizados para responder à solicitação e resolver o problema informado. Não envie dados desnecessários; informações em registros públicos do GitHub ficam sujeitas também aos controles da plataforma. Tratamentos adicionais exigem informar sua finalidade e a base legal aplicável antes de iniciá-los.

## 7. Links de referência

- [Privacidade Twitch](https://www.twitch.tv/p/en/legal/privacy-notice/)
- [Privacidade Kick](https://kick.com/privacy-policy)
- [Privacidade Google](https://policies.google.com/privacy)
- [Gerenciar conexões da Conta Google](https://myaccount.google.com/connections)
- [Privacidade GitHub](https://docs.github.com/en/site-policy/privacy-policies/github-general-privacy-statement)

Mudanças nas funcionalidades ou nos fluxos de dados exigem revisão desta política. Estes documentos ainda não estão incorporados à interface do APK 2.3.0.

## Retorno de autorização da Kick (2.4.2)

A Kick exige um retorno HTTPS. Após autorização, o navegador acessa https://ezeq-ol.github.io/livepulse/kick-callback.html, hospedado no GitHub Pages, com código temporário e state na URL. O GitHub pode receber esses parâmetros e dados técnicos da conexão segundo sua política. A página remove a consulta do endereço visível, não carrega recursos externos e oferece Continuar no LivePulse. Ela não recebe Client Secret, access token ou refresh token e não faz a troca do código por token. O APK confere state, prazo e PKCE e realiza essa troca diretamente com a Kick. A remoção visual da consulta não elimina registros eventualmente mantidos pelo provedor de hospedagem.
