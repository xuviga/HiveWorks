# Публикация на Hangar

HiveWorks публикуется как [`XuViGaN/HiveWorks`](https://hangar.papermc.io/XuViGaN/HiveWorks)
(id `7912`) на платформе `PAPER` с тегом `SUPPORTS_FOLIA`. Folia — не отдельная
платформа Hangar. Версия сейчас: `0.2.3`.

## Один раз

1. Создай API-ключ Hangar, scoped на проект `HiveWorks`, с правами
   `create_version` и `edit_page`.
2. GitHub: **Settings → Secrets and variables → Actions → New repository
   secret**.
3. Имя — `HANGAR_API_TOKEN`, значение — ключ. Ключ в репозиторий не коммитить.

Локальный ключ лежит в `C:\Users\XuViGaN\.hangar\hiveworks-publisher.key`
(вне репозитория) и истекает 2027-11-27. Ротировать заранее.

## Релиз

```bash
git tag v0.2.4
git push origin v0.2.4
```

Тег без суффикса публикуется в `Release`, тег с суффиксом (`v0.2.4-folia`) —
в скрытый `Snapshot`. Версия тега должна совпадать с версией в `pom.xml`.

Можно запустить workflow вручную: **Actions → publish → Run workflow**,
указав версию и канал.

## Локально

```bash
mvn -B -Platest clean package
HANGAR_API_TOKEN=... ./scripts/publish-hangar.sh 0.2.3 Release
```

Сухой прогон:

```bash
DRY_RUN=1 ./scripts/publish-hangar.sh 0.2.3 Release
```

## Ограничение Hangar 15 МБ

Hangar режет multipart-запрос на 15 000 000 байт. В `pom.xml` из shaded
`sqlite-jdbc` исключены нативные библиотеки Android, FreeBSD, ARM32, ppc и
32-битных x86, чтобы jar был ~5 МБ. Оставлены Linux (glibc/musl), Windows и
macOS для x86_64/aarch64. При добавлении новых нативов проверяй размер jar.
