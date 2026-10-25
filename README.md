# GPS Sentinela 0.5

Localização automática ao abrir, com rua, número cadastrado e bairro. Mantém mapa, identidade visual, município/estado, CEP, coordenadas, referência visual, voz opcional e cópia da localização.

## Alterações
- Marcador em seta, orientado pelo rumo do GPS durante movimento ou pelo deslocamento entre posições válidas. Mapa com zoom inicial 16 e altura de 55% da tela para mostrar as vias próximas; mantém rótulos originais do OpenStreetMap. Sem rumo disponível, a seta aponta para cima até haver deslocamento suficiente.
- Removidos sentido da via, painel de numeral próximo aproximado e emergência hospitalar, incluindo os respectivos scripts, dados e consultas.
- Número aparece no campo Número, somente quando retornado pelo cadastro do mapa. Ausência aparece como Não disponível; não há cálculo de número exato a partir do GPS.
- GPS inicia antes do carregamento da biblioteca remota do mapa. A permissão do Android continua necessária no primeiro uso.
- Endereço: intervalo mínimo de 1,5 segundo entre consultas, uma consulta por vez, timeout de 4 segundos, tentativa seguinte usando a posição mais recente. Falhas aplicam espera progressiva até 30 segundos. Respostas distantes 30 m ou mais da posição atual são descartadas.
- Reconsulta após deslocamento de 8 m ou 15 segundos; endereço expira após 20 segundos, deslocamento de 30 m ou posição sem atualização há 15 segundos. Campos antigos são limpos juntos.
- Polígonos de limites carregam apenas quando sua seção é aberta, evitando ler/processar 2,4 MB na inicialização.
- Pausar/retomar é opcional e não bloqueia o início automático.

## Verificação
O workflow executa testes de interface com Playwright, assembleDebug e lintDebug. Os testes simulam início automático, mapa indisponível, deslocamento com resposta atrasada, número ausente, endereço antigo, pausa/retomada e permissão negada.

Para executar a interface localmente, sirva app/src/main/assets na porta 8765 e execute `PLAYWRIGHT_MODULE=/caminho/playwright node tests/ui.test.cjs`.

O intervalo de consulta não garante tempo de atualização real: depende de GPS, rede e geocodificador. Testar em aparelho durante deslocamento continua necessário. Números cadastrados podem ser de um imóvel próximo; não comprovam o imóvel exato. Sem funcionamento garantido em segundo plano.

## Dados e serviços
Endereço envia coordenadas ao Nominatim/OpenStreetMap; o mapa usa tiles OpenStreetMap e Leaflet. Serviços públicos possuem limites e não oferecem SLA. Para distribuição em escala, configurar infraestrutura de geocodificação com capacidade compatível. Não são feitas buscas de hospital nem rotas de sentido.

Limites: 163 polígonos do mapa CPC + CPM JAN23 enviado pelo usuário, importados em 17/10/2026, camadas CPC, CPA-M6, CPA-M7, CPA-M8 e CPA-M12. Cobertura parcial e vigência não confirmada. Base visual anterior preservada em base-aprovada-v0.1.

APK de teste: uma chave temporária diferente pode exigir desinstalar a versão anterior. Nenhuma chave privada é publicada no repositório.
