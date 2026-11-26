# HiveWorks — ресурспак

Пак генерируется скриптом `tools/generate-pack.ps1` и лежит в
`src/main/resources/pack/hiveworks-pack.zip`. Плагин раздаёт его через
`resource-pack.mode: EMBEDDED` или `URL`.

## Пересборка

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/generate-pack.ps1
```

Скрипт создаёт 33 текстуры, 33 модели и 33 современных item-definition.
Требуется Windows PowerShell с `System.Drawing` (встроен).

## Совместимость

| Версия | Механизм |
|---|---|
| 1.21.1 – 1.21.3 | `custom_model_data` + legacy overrides в `assets/minecraft/models/item/*.json` |
| 1.21.4 – 26.x | компонент `minecraft:item_model` + `assets/hiveworks/items/*.json` |

`pack.mcmeta` заявляет `supported_formats` 34–99. Если 26.x потребует новый
формат — правится одна строка в скрипте.

## CMD / модели

| CMD | Модель | Базовый предмет |
|---|---|---|
| 1001–1013 | `bee_jar_<species>` | `glass_bottle` |
| 1101–1105 | `frame_<type>` | `honeycomb` |
| 1201–1204 | `hive_t1..t4` | `paper` (для ItemDisplay) |
| 2000–2003 | `honey_<quality>` | `honey_bottle` |
| 2010 | `product_comb` | `honeycomb` |
| 2011 | `product_wax` | `honeycomb` |
| 2012 | `product_propolis` | `green_dye` |
| 2013 | `product_royal_jelly` | `honey_bottle` |
| 2014 | `product_pollen` | `yellow_dye` |
| 2101 | `net` | `brush` |
| 2102 | `smoker` | `blaze_rod` |

## Кастомный улей

`visuals.custom-hive-model: true` создаёт `ItemDisplay` в центре блока улья с
моделью `hive_t<N>`. Сущность не сохраняется (`persistent=false`) и
пересоздаётся при загрузке чанка. Ванильный блок остаётся под ней и принимает
клики.

## Как добавить текстуру

1. Добавьте модель в `New-ItemModel`/`New-HiveModel` и текстуру в скрипт.
2. Пересоберите пак.
3. Убедитесь, что CMD совпадает с `ItemFactory`.
