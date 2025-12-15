# ES AutoLAN

![CI](https://github.com/EnotScript/es-autolan/actions/workflows/ci.yml/badge.svg)

Auto LAN — мод для автоматического открытия одиночного мира Minecraft в LAN и (опционально) автоматического подключения.

## Features
- Автоматическое открытие мира в LAN при загрузке
- Настраиваемый порт, режим игры, читы и MOTD
- Автоподключение к выбранному миру с задержкой

## Compatibility
- Minecraft 1.21.1
- NeoForge 21.1.216

## CI
Workflow `ci.yml` запускается на push/PR в ветку `main` и выполняет:
- Установку JDK 21
- Кэширование Gradle
- `./gradlew clean build --refresh-dependencies`

Как включить: закоммитьте и запушьте изменения в GitHub — Actions запустится автоматически.

## License
MIT

## Contact
Discord: @enotscript

