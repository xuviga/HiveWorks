# HiveWorks

[![build](https://github.com/xuviga/HiveWorks/actions/workflows/build.yml/badge.svg)](https://github.com/xuviga/HiveWorks/actions/workflows/build.yml)

**Уютное пчеловодство для Paper 1.21.x – 26.x.**

Пчёлы-предметы с генами, рамки в улье, качество мёда от цветов, крафты и атлас видов.
Без обязательных зависимостей.

> Статус: **0.1.0 — каркас**. Ядро производства, GUI и хранения собирается, геймплей
> наполняется по чек-листу из [docs/DESIGN.md](docs/DESIGN.md).

## Сборка

Требуется JDK 21+ и Maven 3.9+.

```bash
mvn -B clean package
# target/HiveWorks-0.1.0.jar
mvn -B test
```

## Установка

1. Положите `HiveWorks-0.1.0.jar` в `plugins/`.
2. Запустите сервер — создастся `plugins/HiveWorks/` с конфигами.
3. ПКМ пустой рукой по улью — открыть панель.

## Документация

- [Дизайн-документ](docs/DESIGN.md)
- [Дорожная карта](docs/DESIGN.md#12-дорожная-карта)
- [Чек-лист MVP](docs/DESIGN.md#13-чек-лист-mvp-01)

## Лицензия

Proprietary (all rights reserved). Бесплатно для использования на любом сервере,
которым вы управляете. Перепродажа, редистрибуция и производные работы — только
с письменного разрешения автора.
