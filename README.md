# GPS Sentinela 0.7 — teste offline

Android nativo, sem coordenadas na interface, limites policiais ou voz.
O motor Mapsforge e suas fontes/temas estão no APK. Não há CDN, Nominatim nem leitor de voz.

Na primeira posição válida, identifica a UF por polígonos locais e oferece o pacote correspondente. Escolha manual disponível se o GPS não tiver sinal. Os mapas públicos Mapsforge são regionais: SP/RJ/MG/ES usam Sudeste, por exemplo; a cobertura maior é informada antes do download. DownloadManager oferece Wi-Fi, progresso, cancelamento e continuidade após fechar o aplicativo. O pacote só é ativado após verificar o cabeçalho/tamanho do mapa e analisar a base municipal. Arquivos parciais nunca são usados.

Ruas são consultadas na geometria salva, por distância ao segmento, com rejeição em cruzamentos ambíguos, GPS impreciso e respostas antigas. Número só é apresentado se o cadastro vincular o endereço à mesma rua e estiver até 25 m com precisão GPS até 30 m; nunca é interpolado. Bairro só é mostrado se houver campo cadastral. Muitos arquivos Mapsforge não preservam todos os campos de endereço: **não há cobertura completa de números/bairros**, nem paridade garantida com serviços online. Município é identificado por contenção em polígonos, não pela cidade mais próxima.

Dados: OpenStreetMap/Mapsforge (ODbL; https://download.mapsforge.org/maps/v5/south-america/brazil/). Municípios: IBGE, distribuição de Luiz Pedone (2017, MIT; https://github.com/luizpedone/municipal-brazilian-geodata). Limites antigos podem divergir das divisões atuais. Mapas e municípios podem ser atualizados manualmente em Mapas offline; a fonte pública muda com o tempo.

## Testar

1. Instale o APK e autorize localização precisa.
2. Escolha Baixar pelo Wi-Fi e aguarde a confirmação de pacote salvo. Sudeste tem aproximadamente 800 MB (variável); reserve 1,3 GB.
3. Ative modo avião mantendo a localização/GPS ligada. Feche e reabra o app.
4. Ao ar livre, aguarde o sinal, verifique mapa, rua e município; o primeiro fix offline pode demorar.
5. Confirme que números/bairros ausentes ficam explicitamente indisponíveis, e que ao parar o GPS ou afastar-se a informação antiga desaparece.
6. Teste download interrompido, retomada, cancelamento e troca de estado. A área não baixada requer novo pacote.

Build: Java 17, Gradle 8.9, SDK 35. `gradle testDebugUnitTest assembleDebug lintDebug`.
Não é navegação curva a curva. Dados offline não incluem trânsito. GPS apenas em primeiro plano.

O APK de teste usa assinatura debug do build; se Android rejeitar atualização por assinatura diferente, preserve o anterior e instale após desinstalar (isso apaga dados locais). Código-fonte disponibilizado no repositório para permitir recompilação e substituição das bibliotecas LGPL.
