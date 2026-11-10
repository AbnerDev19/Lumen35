# 1 · Catálogo e encomenda de arte

Auditoria da 1.5.0 antes das correções. A folha de contato foi montada a partir dos WebP reais. Um nome de produto configurado no enum comprova o vínculo do código. A imagem associada ainda precisa corresponder ao produto.

| Item | Onde aparece | Tem imagem? | O que falta ou decisão |
|---|---|---|---|
| Pro 400H, Superia 400, Ektar 100, Ultramax, Gold 200, Portra 800, 160 e 400 | Filme e carrossel | 00, 01, 02, 03, 04, 05, 06, 07 | Manter IDs e validar legibilidade em 42 dp |
| T-Max, Delta, Tri-X, CineStill 50D, Velvia | Filme e carrossel | 11, 22, 23, 24, 25 | Manter IDs |
| Original, Vencido, CineStill 800T, HP5 Plus, APX, Porcelana, Manteiga, Âmbar | Filme e carrossel | Não. `CatalogArt.roll`, de −200 a −207 | Criar oito artes distintas |
| Lume clássica | Câmera | 14, mas a arte é Pentax K1000 | 14 agora é do K1000. Criar corpo conceitual Lume |
| Kodak vintage, Fujifilm vintage, Canon AE-1, K1000, Nikon FM2, Minolta X-700, Bessa R, Ricoh GR1, Pentax 67, Rolleiflex 2.8F | Câmera | Fallback por `CatalogArt.body` na 1.5.0 | K1000, Nikon e Pentax 67 passam a 14, 18 e 12. Criar as outras sete artes |
| Olympus OM-1 e Mamiya RB67 / RZ67 | Câmera | 18 e 12 eram fotos erradas de Nikon FM2 e Pentax 67 | Fallback provisório e arte do modelo certo. Para Mamiya, padronizar RB67 como versão de referência |
| Leica M6, Canonet QL17 GIII, Electro 35, Contax T2, Olympus Mju II, Yashica T4, Hasselblad 500C/M | Câmera | 17, 21, 20, 15, 13, 19, 16 | Vínculo confirmado pelo enum. Conferir inscrições das artes já existentes antes da aprovação comercial |
| Neutra, Canon FD, Nikon AI-S, Zeiss 50, Helios, Planar 80, Mamiya, Nokton e Takumar | Lente | Na base, nove desenhos de `CatalogArt.lens` | Reutilizei 10 como neutra. Criar oito artes específicas |
| Sensor físico e zoom | `PhoneLenses` | Fallback −100 na base | 10 é ícone genérico reaproveitado. Não identificar um hardware genérico como lente real |
| 08 e 09 | Arquivos presentes, sem vínculo ativo | Sim. FUJICOLOR COLORFILM e KODAK COLORFILM | Inscrições genéricas. 09 não comprova ColorPlus. Reservar ambos |
| 10 | Arquivo presente, sem vínculo ativo na base | Sim. Lente genérica de frente | Reutilizado em Neutra e Celular |
| Ícone do Android | Manifesto | Vetor `ic_lume.xml` na base | Marca nova com foreground, fundo e monocromático raster |
| Obturador, foco, cantos, grade, horizonte e flash de captura | `ShutterView`, `FinishView`, `CameraControls` e `shutterBlink` | Não. Canvas e shape | Reservar raster para acabamento do obturador. Traços funcionais ficam geométricos para acompanhar toque, inclinação e enquadramento |
| Voltar, fechar, engrenagem e reticências | Galeria, `VisualPicker`, cabeçalho e carrossel | Não. Glifos de texto | Quatro ícones raster de alto contraste, com nome acessível separado |
| Flash, proporção, data, EV, galeria, selfie, câmera, lente, retrato, celular | `MainActivity` | Não. Texto em botão | Preservar rótulos. Arte opcional para ações reconhecíveis, nunca como única indicação |
| Botões, chips, seleção do cartão, fundo da câmera e da galeria | Activities, `CameraBodyBackground`, `FilmStrip` e `VisualPicker` | Não. Gradiente, ripple, linhas e contorno | Tokens e estados em recursos XML. Produzir só a textura de metal se necessária; não rasterizar textos e estados |
| CheckBox, Spinner, barra deslizante e ações de diálogo | Ajustes e diálogos do sistema | Não. Controles de plataforma | Opções discretas viraram linhas Lume. Sliders mantêm semântica nativa; folha e diálogos ainda precisam da mesma linguagem |
| Carimbo, furos, papel e bordas de impressão | `DateStamp`, `PrintFrame` | Não. Canvas | Data e frame dependem dos dados; manter composição dinâmica. Opcionalmente aplicar textura raster translúcida |
| Foto, miniatura e visualizador ampliado | Galeria | Sim. JPEGs reais | Não são assets do catálogo |

## Registro técnico

- Fonte de arte em 1024 × 1024 com alpha para revisão. Export final `assets/catalog/assetNN.webp`, 512 × 512, sRGB, alpha limpo, assunto ~72–82% do quadro, alinhamento óptico pelo centro. Comparar lossless e WebP 88 visualmente e evitar compressão que destrua lettering.
- IDs 00–25 permanecem. IDs propostos 26–63 devem ser registrados só após aprovação dos WebP. Separar arte, identificador do perfil, ISO nominal e receita. Nunca derivar ID novo da ordem do enum. Como `Film.recipe()` usa `FilmRecipe.valueOf(name())`, acrescentar o preset e a receita com a mesma chave no mesmo commit.
- Registrar `asset` em `Film` e `CameraStyle`; substituir o cálculo por ordinal de `LensStyle` por campo explícito. Tratar faltantes como placeholders, com verificação de existência de arquivos no build. `CameraPreferences` usa nomes persistidos e migra índice antigo. Manter os nomes antigos. Não alterar receitas existentes para adicionar uma imagem.
- Em 42 dp, o nome e o ISO legíveis são texto nativo do cartão. A arte recebe texto real na escala original; qualquer letra inventada reprova. Para fidelidade, compor lettering autorizado por cima do raster e conferir contra a foto do SKU ou variante. Usar transparência verdadeira. Chroma verde só como intermediário, pois pode contaminar as bordas de rolos verdes.
- Referências de forma e variante antes de gerar. [Fujifilm X](https://www.fujifilm-x.com/en-us/products/x-series/), [Instax Mini 11](https://www.instax.com/mini11/en/support/), [manual GA645](https://manualzilla.com/doc/7045416/fujifilm-ga645-owner-s-manual), [Klasse W Black](https://www.kitamura.jp/photo/seasons/pdf/shiki-Vol63.pdf), [catálogo Fujifilm de filmes](https://www.fujifilm.com/us/en/business/professional-photography/film), [ficha com Astia](https://asset.fujifilm.com/www/us/files/2020-04/9cdee030ab7fc76bbcfddbcd38650038/ProfessionalFilmDataGuide.pdf), [Ilford](https://www.ilfordphoto.com/wp/wp-content/uploads/2024/06/PAL-version-1-March-2024.pdf), [CineStill](https://cinestillfilm.com/products), [Lomography](https://shop.lomography.com/jp/film/all?film_dxcode=0&film_type=5431), [ColorPlus](https://35mm.org/pt-br/film/kodak-colorplus-200).
- O documento **PROMPTS.md** traz um prompt autocontido por arte. Cores de edição histórica e a posição dos logotipos devem ser tomadas da referência anexada da variante selecionada. Nenhum prompt pressupõe afiliação à Fujifilm.
