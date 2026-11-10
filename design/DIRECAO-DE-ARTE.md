# Lume 35 · revisão de produto e direção de arte

1. Três perfis mostram corpos de outros fabricantes ou modelos, e vários filmes e lentes usam desenhos genéricos.
2. O Retrato mistura cor em sRGB e usa um único raio com três passadas de caixa. Isso apaga diferenças de profundidade e pode denunciar a borda do recorte.
3. A prévia e o JPEG já têm pipelines relacionados, mas os acabamentos têm controles desiguais e a tira de filme imprime 12A em todas as fotos.
4. A câmera tem controles funcionais, porém estilos serifados, glifos e diálogos do sistema fragmentam a linguagem industrial.
5. A galeria guarda dados que o grid esconde, e o ícone antigo é uma silhueta genérica sem assinatura visual própria.

## 1 · Arte de catálogo

Veja [CATALOGO.md](CATALOGO.md) para a matriz da base, arquivo por arquivo, e [PROMPTS.md](PROMPTS.md) para os prompts prontos. As artes novas são uma encomenda de produção. O projeto usa os 26 WebP disponíveis e corrige os vínculos inequívocos. As simulações Provia, Astia, Acros II, FP4, Lomography 400 e ColorPlus precisam de receitas próprias e imagens aprovadas antes de aparecer no app. CineStill 800T e HP5 já são receitas, com arte pendente. X100V e X-Pro3 devem entrar como perfis estéticos de corpos digitais. GA645, Klasse W, Natura Black e Mini 11 pedem perfis distintos de enquadramento e acabamento. O nome da câmera nunca indica que o sensor do telefone ganhou outro hardware.

## 2 · Retrato

| Parâmetro | Base auditada | Revisão e motivo |
|---|---|---|
| Origem da máscara | ML Kit Selfie Segmenter 16.0.0-beta6. Sem chamada a enableRawSizeMask. Redução proporcional com lado maior até 640 px e leitura de dimensões da API | Canvas quadrado 512, escala única e padding cinza. O mapa inverso remove o padding antes de refinar. A API pode redimensionar o resultado para a entrada quando a opção raw está desligada. Não havia comprovação de máscara quadrada bruta esticada na 1.5.0. |
| Eixo e proporção | Duas reamostragens bilineares. EXIF e espelhamento já passam pelo decode | Geometria explícita em ambos os eixos. Para x, `u = (left + (x+.5)/w*modelW)*mw/512-.5`. Mesmo princípio em y. Preserva proporção; corte final continua em PhotoStore. |
| Borda | Remapeamento por confiança de .18 a .72. A cor não participa da decisão da máscara | Feather de .14 a .86, vizinhança 5×5 ponderada por luminância, refinamento só na faixa incerta e rejeição de pixels de primeiro plano no gather. Reduz contaminação de ombro e cabelo. Cabelo fino e contraluz continuam a exigir teste. |
| Abertura sintética | 3 box blurs separáveis, raio uniforme, fundo até 1/8 | 29 taps em disco, leve íris hexagonal e lado maior de até 400 px. Raio de `amount × min(24, .075 × ladoMenor) × [.26 + .74 × smoothstep(distância)]`. Distância 2D até a máscara via duas passadas, sem profundidade métrica. Objetos separados no mesmo plano podem desfocar de modo diferente. |
| Altas luzes | Média de sRGB e peso de máscara | Decodifica sRGB em luz linear; realce máximo de 20% acima de luminância .70 a .98; forma de íris sem cruzes artificiais. O realce não recupera altas luzes já saturadas no original. |
| Grão e fallback | Retrato antes da revelação e fallback para a foto sem máscara válida | Ordem preservada. Revelação após compor aplica o mesmo grão no fundo e no sujeito. Sem pessoa ou fundo suficientes, mantém foto original e aviso. |

Código completo em `PortraitProcessor.java`, `PortraitBokeh.java` e `LinearRgb.java`. A implementação é CPU no JPEG e não altera o visor de Retrato em tempo real. Custo teórico do gather no pior caso de miniatura quadrada é 29 × 400², cerca de 4,64 milhões de candidatos, mais a composição de uma passagem em resolução final. É uma contagem de operações, não uma medição de tempo. Uma comparação de desempenho em aparelho deve usar 1600, 2560 e 4000 px, cenas escuras e duas capturas em fila.

## 3 · Efeitos analógicos

A referência visual serve para definir materialidade e parcimônia. O código do app de referência não faz parte do projeto auditado.

| Efeito | Problema atual e aparência desejada | Parâmetros e implementação | Custo GPU previsto |
|---|---|---|---|
| Data laranja | Já existe um desenho matricial; deve lembrar LCD de compacta, legível sem cobrir o centro | `DateStamp` e controles já existentes. Liga/desliga, formato e cor por perfil | Canvas no visor, poucas primitivas. Exportação CPU |
| Tira perfurada | 12A era fixo; quadro não comunicava sequência | `PrintFrame.STRIP_35` lê contador persistente por disparo. Número também vai ao EXIF. Falhas podem deixar lacunas, como numa sequência física | Sem passe extra no visor. Canvas na exportação |
| Poeira | Faltava irregularidade no papel digital | Controle 0–100, até 35 pontos pequenos, semente por captura. Aplicada na foto e restrita ao quadro | Zero na GPU do visor; Canvas no JPEG |
| Riscos | Uma textura cheia cria repetição óbvia | Controle 0–100, até quatro traços finos, ligeiramente inclinados, consistentes por captura | Zero no visor; Canvas no JPEG |
| Light leak cromático | Luz tinha matiz fixo mesmo em filmes distintos | Laranja, âmbar, carmim ou violeta. Cor como uniforme AGSL e multiplicador CPU. Mantém a geometria da receita | ALU no passe atual, sem amostra de textura nova. Android 10–12 usa gradiente Canvas |
| Halação | Controle anterior era binário e a receita podia continuar ligada | Automático da receita ou 0/25/50/75/100. Ajusta amplitude da resposta em ambos os pipelines. O raio e limiar seguem os originais | Sem passe novo; com halo/flare ativo, AGSL faz até quatro amostras adicionais da câmera. Exportação reduzida a 1/8 para o halo |
| Dupla exposição | Não havia segundo fotograma | Escolher uma segunda imagem, ajustar 0–100, preencher com corte central proporcional e misturar RGB linear antes da curva e do grão. O acesso ao arquivo escolhido é persistente. Se falhar, salva a foto base e avisa | Zero na GPU do visor. Uma decodificação, um bitmap de alinhamento e uma passagem em CPU no JPEG. Tela de sobreposição ao vivo fica para outra fase |
| Polaroid / Instax | Polaroid existia. Faltava distinção do papel instantâneo estreito | `INSTAX` mantém a foto, com margem inferior de 19,5% da largura. Proporção do papel não simula dimensões físicas exatas | Zero no visor; canvas de impressão no JPEG |
| Vinheta | Vinha da receita e da lente sem controle direto | Escala 0–100 da vinheta combinada, com geometria da lente preservada | ALU já presente ou um gradiente Canvas no fallback |
| Luz de borda | O leak grande podia roubar contraste da foto | Controle 0–100, faixa quente lateral de 11% da largura, lado determinístico por captura | Zero no visor; um gradiente no JPEG |

Os custos são estimativas. `FilmPatina` trabalha no retângulo da foto. Modo Original compara a imagem da câmera e mantém o comportamento de comparação existente. Os efeitos de impressão novos não são representados no modo de comparação.

## 4 · Marca

As três direções estão em PNG colorido e monocromático. Origem no canto superior esquerdo da tela 108 × 108; centro 54/54; safe zone de 66 dp de diâmetro. Os arquivos 432 × 432 equivalem a quatro pixels por unidade lógica. A versão monocromática é uma máscara de alpha para ícones temáticos.

| Nome | Símbolo e grade | Cores e monocromática | Julgamento |
|---|---|---|---|
| Janela de Luz | L do chassi à esquerda, corte no canto alto à direita e índice central 8 × 8. Traço de 6. Pontos principais de x=33 a 75, y=33 a 74 | Prata #CCD0D4, fundo #0E0E0F, índice #FFA454. No mono, índice e chassi se tornam um recorte único de uma tinta | Recomendada e já ligada no manifesto. A assimetria identifica a marca reduzida e funciona como gravação de uma só passada |
| Íris Indexada | Aro centrado, Ø48, traço 5, núcleo angular de 4 e índice 6 × 6 | Prata e laranja sobre preto fosco; todos os traços sólidos em uma tinta | Evoca lente e anel de abertura. Fica perto dos símbolos de câmera mais comuns |
| Quadro 35 | Algarismos em dois módulos, 3 e 5, com traço 5 e um índice 5 × 5, entre x=32–74 e y=35–73 | Prata e laranja sobre preto fosco; mesmas massas em branco para a máscara | Boa gravação editorial. A leitura dos dois números sofre mais em tamanho pequeno |

Logotipo textual `LUME 35`, sans geométrica média, espaçamento sugerido .08 em, nunca embutido no ícone adaptativo. O laranja é marca de índice. Use-o também em posição e foco para que seu significado seja consistente. O launcher usa `mipmap-anydpi-v26/lume_mark.xml`; v33 acrescenta o monocromático. O logo antigo foi deixado no projeto como referência, fora do manifesto.

## 5 · Telas e comportamento

### Tokens

| Função | Token e decisão |
|---|---|
| Preto fosco | #0E0E0F. Reduz ruído visual ao redor da imagem |
| Grafite | #202124 para superfícies; #73767C para contornos. Sem marrom decorativo como base |
| Prata escovada | #CCD0D4 em telemetria e peças. Textura opcional no hardware simulado, sem textura atrás de texto |
| Texto | #F2F2EF para rótulos. Prata ou verde claro para dado secundário |
| Verde Classic Chrome | #AFC3AB na simulação ativa no EVF. Referência cromática, sem copiar a marca |
| Laranja de índice | #FFA454 para foco, seleção e toque. Estado acompanha contorno ou texto |
| Vermelho de gravação | #FF6262 reservado. O app ainda não grava vídeo |
| Tipo | Sans do sistema 14 sp em rótulos, 20 sp em títulos, mono 14 sp em valores. 12 sp só para metadados auxiliares; 200% de fonte precisa de teste |
| Espaço e material | Base 4 dp; passos 8/12/16/24/32. Alvo de 48 dp. Raio 8 dp, 12 em folhas. Fundo plano; contorno em vez de elevação para a maioria dos controles |
| Movimento | 80 ms ao pressionar, 150–180 ms de retorno, 160–220 ms de revelação. Respeitar escala de animação do sistema e evitar flash intenso. Um clique háptico por mudança de índice |

### Áreas

| Área | Problema verificado | Regra e resultado | Código ou estado |
|---|---|---|---|
| Viewfinder | O enquadramento, grade e nível já existem. EV era botão isolado e faltava leitura de exposição na tela | Moldura sem ornamento pesado. EV e bateria atuais, filme ativo e última exposição explicitamente rotulada; velocidade, abertura e ISO vêm do último EXIF. Grade/horizonte mantidos. Histograma opcional proposto em entrada de baixa resolução, desligado por padrão e a 4 Hz | `industrial_evf.xml`, `MainActivity.updateReadout`. Histograma e telemetria em tempo real ficam para integração Camera2/ImageAnalysis |
| Filme, intensidade e menu | Carrossel ajustável; não era um dial serrilhado. Sliders e diálogos tinham linguagens distintas | Cartão gráfico, valor legível, indicação “em uso”. Dial de detentes discretos com haptic e acionamento acessível. Toque abre seletor; gesto e teclado avançam de forma equivalente | `industrial_catalog_card.xml` integrado. `IndustrialChoice` integrado nas opções. `industrial_physical_controls.xml` é layout para futura ligação dos dials físicos |
| EV, abertura e alavanca | Compensação real via CameraX; nenhuma API de controle de abertura é chamada na base | Dial EV usa faixa e passo informados pela câmera. Anel de aparência de abertura controla Retrato e usa rótulo “Desfoque” para evitar confusão. Alavanca Fotografia/Retrato/Dupla seleciona fluxo. Sem valores f/ fictícios | EV existente e leitura ligados; anel, alavanca e serrilhado ainda não ligados. Usar IDs `physical_ev`, `physical_aperture` e `physical_mode` |
| Obturador | Já tinha círculos Canvas, animação de 80/160 ms, som do sistema opcional e pulso de .80 | Preservar área de 72 dp, reduzir pulso a .24, manter compressão e retorno. Gravação própria de obturador deve ser licenciada, mono e opcional. O som atual segue o dispositivo | `ShutterView` e `takePhoto`. Áudio mecânico gravado e teste de animação reduzida ainda pendentes |
| Seletores de filme, corpo, lente e celular | Tinham lista de cartões ilustrados e cor por item, o que fragmentava o estado ativo | Fundo grafite, linha de foco comum, nome em caixa alta, subtítulo semântico. Campo ISO nominal pertence ao preset, separado do ISO de captura. Não inferir ISO em Original | `VisualPicker`, `industrial_catalog_card.xml`. ISO nominal separado já integrado |
| Ajustes | Spinner e checkbox padrão interrompiam o ritmo de câmera | Linha com rótulo à esquerda e valor à direita, detente por toque, longa pressão no sentido inverso, DPAD e TalkBack. Sliders mantêm semântica e cor. A confirmação ainda é de plataforma | `SettingsPanel`, `IndustrialChoice`, `industrial_menu_row.xml`. Migrar a folha externa, formato, EV, Retrato, detalhes e confirmação de exclusão na próxima fase |
| Galeria e visualizador | Grid tinha só miniatura, e dados ficavam em diálogo separado. A ampliação já tinha pinça e ações | Contact sheet grafite, quadro, data e filme, com cache e leitura EXIF assíncrona. Fotos antigas sem número mostram “—”. Preservar foto ampliada sem moldura de UI sobre o conteúdo e confirmação antes da exclusão | `GalleryActivity`, `industrial_contact_cell.xml`. Furos gráficos na margem da grade e barra do visualizador ficam para o refinamento |
| Estados | Ripple, toasts e contador já existem; o foco de teclado e a leitura de fila eram inconsistentes | Normal grafite, pressionado #36312A, desativado fosco, seleção/foco laranja com contorno, fila anunciada, erro textual. Foto revelada deve entrar com fade único. Evitar anunciar valores a cada quadro | `industrial_control.xml`, status `LIVE_REGION_POLITE`; padronização global do erro, foco e fade da galeria ainda pendentes |
| Acessibilidade | Há descrições e muitos alvos de 48 dp, mas não é possível certificar contraste sobre vídeo nem fontes ampliadas por inspeção de código | Rótulos essenciais sobre fundo sólido, texto regular ≥4,5:1, foco indicado por cor e contorno, ícones decorativos ignorados, layout wrap e TalkBack. Usar estados e valores acessíveis em controles tipo dial | XML e `IndustrialChoice` prontos. QA em 320 dp, fonte 200%, TalkBack, teclado, Android 10 e 13+ |

### Aceite e ordem de implementação

1. **Maior impacto com menos risco.** Corrigir 12/14/18, reaproveitar 10, aplicar marca e tokens, substituir as linhas dos ajustes e fazer revisão em fonte grande.
2. **Retrato.** Comparar a base com 1.6 em cabelo, vidro, objetos e contraluz. Validar selfie espelhada, 1:1, 3:4, 6:7, bordas e cenas sem pessoa. Medir tempo e pico de memória nos três tamanhos.
3. **Efeitos e galeria.** Conferir 0%, seed, duplicação de quadros, arquivos antigos sem EXIF, URI revogada, dois disparos na fila e cor de vazamento em Android 10–12 e 13+. Confirmar consistência entre visor e JPEG onde o visor oferece o efeito.
4. **Artes.** Priorizar Original, CineStill 800T, HP5, câmera Lume e 8 lentes. Aprovar primeiro 4 imagens a 42 dp. Produzir depois seis corpos Fujifilm e seis novas emulsões com pares enum/receita e teste visual.
5. **Controles físicos e captura avançada.** Ligar layout dos dials ao comportamento real, estados de seleção, som licenciado e histograma leve. Revalidar contraste, gesto, DPAD e TalkBack. Só depois avaliar preview bokeh, sobreposição ao vivo e execução por GPU.

### Arquivos desta revisão e próximos

- Alterados. `MainActivity.java`, `data/PhotoStore.java`, `camera/PhoneLenses.java`, `model/CameraStyle.java`, `model/Film.java`, `model/LensStyle.java`, `imaging/PortraitProcessor.java`, `imaging/FilmPipeline.java`, `imaging/FilmPreview.java`, `imaging/PrintFrame.java`, `ui/SettingsPanel.java`, `ui/CatalogSelectors.java`, `ui/VisualPicker.java`, `ui/GalleryActivity.java`, `AndroidManifest.xml`, `app/build.gradle`, `res/values/strings.xml`.
- Novos. `LinearRgb.java`, `PortraitBokeh.java`, `FilmPatina.java`, `IndustrialChoice.java`, `res/layout/industrial_*.xml`, `res/drawable/industrial_control.xml`, `res/values/lume_industrial.xml`, `res/mipmap-*/lume_mark*`, `tools/ReviewSmoke.java` e `design/`.
- Próximos. `Film.java`, `FilmRecipe.java`, `LensStyle.java`, `CatalogSelectors.java`, `CatalogArt.java`, `FilmStrip.java`, `CameraControls.java`, novos `assets/catalog/asset26.webp` a `asset63.webp`, `assets/ui/` e eventual áudio licenciado.

Validação disponível. Sete verificações Java do núcleo de imagem e leitura estrutural do XML. A revisão fonte foi numerada 1.6.0 para diferenciar da base. O APK 1.5.0 foi usado como referência da entrega anterior. A revisão 1.6.0 ainda exige Gradle, Android Lint e teste físico.
