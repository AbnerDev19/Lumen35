# Lume 35 · Android 1.5.0

Câmera analógica digital para Android 10 ou superior. Captura com CameraX e revelação local. O identificador é `br.com.lume35.camera` e as fotos são publicadas em `Pictures/Lume 35`. A versão tem código 9.

## Fotografar

- Toque no visor para focar, faça pinça para ajustar o zoom e segure para comparar com o original.
- Deslize pelo carrossel de filmes. Toque no filme selecionado para abrir a intensidade. A 0%, o motor preserva os pixels que recebe. Enquadramento, Retrato, moldura e carimbo têm controles próprios.
- **Câmera** seleciona o corpo simulado e **Lente** seleciona a assinatura óptica simulada. **Celular** seleciona os sensores que o Android expõe e os níveis de zoom. **Selfie** alterna frente e trás.
- A proporção na barra superior abre a moldura. O botão **Formato** permite seguir o corpo ou escolher 3:4, 1:1 ou 6:7.
- O controle **EV** usa a faixa de compensação do sensor ativo. **Ajustes** contém temporizador de 3 ou 10 segundos, qualidade JPEG, resolução de saída, som, vibração, grade, horizonte e impressão de ISO e obturador.
- O botão de volume também dispara. O obturador fica disponível após a câmera entregar o JPEG para revelação, com até quatro fotos em andamento.
- **Galeria** abre o rolo interno. Toque para abrir, faça pinça ou toque duas vezes para ampliar e deslize entre fotos quando estiver na escala inicial. Os botões permitem compartilhar, consultar dados e excluir.
- **Guardar JPEG original** mantém uma cópia privada para uso futuro. O rolo interno exibe as fotos reveladas. A reedição é uma etapa futura.

## Revelação

As 21 receitas têm curvas cúbicas monotônicas e LUTs RGB de 256 amostras. Cada filme pode alterar a densidade, a resposta à saturação, o equilíbrio de cor e o grão. Os cinco filmes P&B têm misturas de canais e curvas próprias. Os 20 corpos e as 9 lentes acrescentam suas assinaturas ópticas e o corpo define um acabamento padrão.

O visor do Android 13+ usa AGSL e compartilha os parâmetros da foto salva. A prévia aproxima o halo com amostras locais. No JPEG, o halo usa uma máscara reduzida a 1/8 com três passadas de desfoque. No Android 10–12, a prévia usa uma matriz e sobreposições de grão, vinheta e vazamento. O Retrato usa ML Kit e entra na revelação quando encontra uma pessoa identificável. O efeito óptico é uma interpretação estética.

## Compilar

Requisitos locais de JDK 17, Gradle 8.9, Android SDK 35 e Build Tools 35.0.0.

```bash
gradle :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
gradle :app:assembleRelease :app:lintRelease
```

No GitHub Actions, abra **Gerar APK** e execute o workflow. Ele gera `Lume35-APK` e `Lume35-release-APK`. O release usa R8 e redução de recursos.

O workflow mantém o secret `LUME_DEBUG_KEY_BASE64`. Com o secret preenchido, os dois APKs usam a chave preservada. Sem ele, a chave gerada pelo runner pode mudar a cada execução. Localmente, o release aceita `LUME_SIGNING_FILE` com o caminho da chave existente e usa `~/.android/debug.keystore` como padrão. O APK distribuído aqui usa o mesmo certificado do Lume 35 1.4.2.

## Comparar os filmes

O ZIP de amostras contém 21 pares a 100%, com céu, tons de pele, cores e uma escala de preto a branco. A imagem da direita passa pelo motor CPU usado no JPEG. O programa `tools/FilmSamples.java` gera as imagens com JDK 17, em conjunto com `BoxBlur`, `MonotoneCurve`, `FilmRecipe` e `FilmPipeline`.

## Teste em aparelho

- Atualizar a partir da 1.4.2 e abrir a primeira foto antiga e a primeira foto nova do rolo.
- Comparar visor e JPEG no Android 10–12 e 13+, em luz de janela, neon, contraluz e cenas escuras. Observar halo, grão, estabilidade do shader e vazamento em fotos consecutivas.
- Validar foco por toque, pinça, EV, flash, temporizador, volume, horizonte e disponibilidade de sensores físicos.
- Conferir selfie espelhada, recorte nas três proporções, data, ISO e obturador.
- Testar Retrato com cabelo, bordas complexas, pouca luz e fotos sem pessoa. Fotografar em sequência no tamanho máximo e acompanhar tempo de revelação e uso de memória.
- Verificar exclusão, confirmação do Android, compartilhamento e atualização do rolo no APK release.

Consulte [CHANGELOG.md](CHANGELOG.md) para o histórico.

## Revisão 1.6.0

O material em [design/DIRECAO-DE-ARTE.md](design/DIRECAO-DE-ARTE.md) descreve a auditoria da 1.5.0,
as mudanças desta revisão, os limites e os próximos passos. O diretório `design/` contém a folha
de contato, os prompts individuais, três direções de marca e as versões monocromáticas.

Para validar o aplicativo, execute com o SDK 35 configurado:

```sh
gradle :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

A revisão 1.6.0 é uma entrega de código-fonte. As verificações locais de Java e XML não substituem
a compilação Android e os testes de câmera em aparelho.
