# GPS Sentinela 0.2 — protótipo

Preserva a identidade visual da base `base-aprovada-v0.1`.

## Alterações
- Detalhes: bairro, município/estado, CEP quando disponível, coordenadas, precisão, idade da posição e referência visual manual.
- Botão Emergência grave: localização copiável, discador 192 e catálogo inicial de dez referências hospitalares paulistas.
- Ordenação por distância geodésica em linha reta, nunca apresentada como tempo ou melhor rota. Desativada sem posição recente (30 segundos) ou precisão de até 100 metros.
- Filtro por neurocirurgia documentada, sem promessa de plantão ou adequação clínica.
- Destino informado pela regulação em qualquer cidade de SP e navegação externa pelo Google Maps.

## Limites importantes
Este catálogo NÃO representa cobertura hospitalar completa de todo o estado. Há regiões sem referência cadastrada. Não identifica automaticamente o hospital adequado ao paciente, não consulta vagas, equipes, sangue ou aceitação em tempo real e não substitui regulação/triagem. Nenhuma UBS ou UPA é incluída no catálogo. Algumas referências institucionais são antigas: consulta da página não comprova manutenção atual do serviço. Crianças, queimaduras, trauma craniano e outros perfis exigem confirmação específica.

O botão de rota exige confirmação de destino com regulação/resgate. Coordenadas OpenStreetMap representam prédios e não a porta de urgência; conferir acesso no navegador. HCFMB sem geocodificação confirmada aparece sem distância. O tempo e o trajeto são calculados no Maps.

Fontes por hospital estão em `app/src/main/assets/hospitals.json`. As posições do usuário não são gravadas pelo aplicativo. Reverso de endereço envia coordenadas ao Nominatim; tiles ao OpenStreetMap; ao abrir a rota, o destino é enviado ao Google Maps. Não inserir identificação de pacientes no campo de referência. Aplicativo sem funcionamento garantido em segundo plano.

## Verificação
`node tests/emergency.test.cjs` verifica frescor/precisão, distância e URL. GitHub Actions executa assembleDebug e lintDebug. Teste em aparelho ainda necessário, especialmente GPS real, discador e abertura externa do Maps.

APK debug: nova chave temporária a cada build pode exigir desinstalar a versão anterior. Nenhuma chave privada é publicada no repositório.

## Versão 0.3
Velocidade removida. Sentido estimado pela sequência de posições recentes e conexões da via no OpenStreetMap/Overpass; sem prever conversões. GPS impreciso, paralelas ambíguas ou dados antigos suspendem a indicação. Numeração próxima aproximada, consultada no mínimo a cada 6 segundos em movimento; sem número disponível, não há inferência de numeral. Requer internet e serviços públicos, não oferece SLA.

### Limites
163 polígonos importados sem simplificar as coordenadas do mapa enviado pelo usuário: https://www.google.com/maps/d/viewer?mid=1aDNiKW0DUK61isW1h4OFwd4MV87KpaLr (Mapa - CPC + CPM JAN23). Camadas CPC, CPA-M6, CPA-M7, CPA-M8, CPA-M12. Importação 17/10/2026. Não é cobertura estadual; vigência não confirmada. Pontos de hospitais, delegacias e bombeiros não fazem parte desta importação de limites. Os hospitais desse mapa não são evidência de atendimento a trauma grave.

Testes: node tests/emergency.test.cjs e node tests/direction.test.cjs; interface móvel via Playwright no Actions; assembleDebug e lintDebug. Validar em campo antes de uso operacional.

## Versão 0.4
Campos visuais de precisão e última atualização removidos; validação interna mantida. Ícone adaptativo próprio (escudo e pin). 16 referências documentadas, com evidência específica descrita por hospital (não equivalem todas a centros de trauma de alta complexidade). Busca online de hospitais via Overpass em raio de 50/100 km, sem limite fixo de quantidade no cliente; cobertura depende do OSM. Resultados sem evidência institucional ficam separados, com confirmação operacional explícita para este caso/sessão antes da rota. UPA/UBS/AMA/CAPS e serviços especializados incompatíveis são excluídos.

OSRM Table calcula trajetos de carro; ordenação usa distância dos trajetos calculados, que não é garantia do menor trajeto possível, menor tempo real ou hospital mais próximo entre todos os existentes. Não usa trânsito em tempo real. Falha parcial/nula não vira zero km; linha reta é identificada. Rotas expiram em 60 s ou deslocamento >=100 m; localização antiga impede comparação. Coordenadas são enviadas a Overpass/OSRM/Nominatim para consultas; nenhuma informação clínica é coletada. Serviços públicos sem SLA, protótipo não homologado para despacho operacional. Confirme capacidade para o caso e entrada de urgência com regulação.
