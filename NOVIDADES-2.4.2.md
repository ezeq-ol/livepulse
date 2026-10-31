# LivePulse 2.4.2 — Kick com retorno HTTPS

A Kick exige um retorno HTTPS no cadastro do aplicativo. O LivePulse usa agora:
https://ezeq-ol.github.io/livepulse/kick-callback.html

O portal Kick do aplicativo LivePulseAndroid já foi corrigido para esse endereço.
No celular, instale esta versão por cima da anterior, salve seu Client ID e Client Secret em Integrações e toque em Conectar Kick. Autorize na Kick e, ao chegar à página HTTPS, toque em Continuar no LivePulse. A conexão deve ser iniciada pelo app no mesmo aparelho.

O Client Secret continua no armazenamento privado do APK e nas requisições necessárias diretamente à Kick. Não está na página, no GitHub ou no APK distribuído como credencial embutida. Não envie o segredo no chat. A página recebe código temporário e state, remove a consulta do endereço visível e encaminha os dados ao app após o toque; não troca o código por tokens. O provedor de hospedagem pode registrar os dados técnicos da requisição. O APK valida state, prazo e cliente antes de consumir o pedido e usa PKCE na troca.

162 verificações Java e 30 verificações da página aprovadas. Página pública HTTPS e download conferidos. Sem aparelho Android conectado: a autorização real da Kick no APK não foi concluída aqui. O usuário deve finalizar no celular.
