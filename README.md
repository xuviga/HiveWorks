# HiveWorks

[![build](https://github.com/xuviga/HiveWorks/actions/workflows/build.yml/badge.svg)](https://github.com/xuviga/HiveWorks/actions/workflows/build.yml)

**Уютное пчеловодство для Paper 1.21.x – 26.x.**

Пчёлы-предметы с генами, рамки в улье, качество мёда от цветов, крафты и атлас видов.
Без обязательных зависимостей.

> Статус: **0.2.0-SNAPSHOT**. Ресурспак, кастомная модель улья и королева;
> охрана и разорители — следующие шаги.

## Сборка

Требуется JDK 21+ и Maven 3.9+.

```bash
mvn -B clean package
# target/HiveWorks-0.2.0-SNAPSHOT.jar
mvn -B test

# сборка против Paper 26.3 (нужен JDK 25)
mvn -B -Platest clean package
```

Ресурспак пересобирается скриптом:

```powershell
powershell -File tools/generate-pack.ps1
```

## Установка

1. Положите `HiveWorks-0.1.0.jar` в `plugins/`.
2. Запустите сервер — создастся `plugins/HiveWorks/` с конфигами.
3. ПКМ пустой рукой по улью — открыть панель.

## Документация

- [Дизайн-документ](docs/DESIGN.md)
- [Ресурспак](docs/RESOURCE_PACK.md)
- [Тест на Paper 26.3](docs/TESTING_26.3.md)
- [Дорожная карта](docs/DESIGN.md#12-дорожная-карта)
- [Чек-лист MVP](docs/DESIGN.md#13-чек-лист-mvp-01)

## Лицензия

Proprietary (all rights reserved). Бесплатно для использования на любом сервере,
которым вы управляете. Перепродажа, редистрибуция и производные работы — только
с письменного разрешения автора.
