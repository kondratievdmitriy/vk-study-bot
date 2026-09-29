# Study Bot 📚

## О проекте

**Study Bot** — это чат-бот для VK, который помогает поэтапно изучать учебные материалы. Пользователь загружает материал (книгу, конспект лекций), бот разбивает его на блоки-уроки, а затем пользователь изучает их по расписанию и отвечает на проверочные вопросы. Бот оценивает усвоение материала и корректирует дальнейший план обучения.

Позже бот будет поддерживать несколько пользователей с общей базой материалов.

```
            +----------------------+
            |       ПОЛЬЗОВАТЕЛЬ    |
            +----------+-----------+
                       |
                       v
              +-----------------+
              |     Study Bot    |
              +--------+--------+
                       |
        +--------------+--------------+
        |              |              |
        v              v              v
  +-----------+ +------------+ +-------------+
  | Загрузка   | | Разделение | |  Проверка   |
  | материала | | на блоки-  | |  усвоения   |
  |           | | уроки      | |  + вопросы  |
  +-----------+ +------------+ +-------------+
                                         |
                                         v
                                 +-------------+
                                 | Корректировка|
                                 |  расписания  |
                                 +-------------+
```

## Составные части проекта

### Архитектура

Проект построен по принципу разделения ответственности:

| Компонент | Назначение |
|---|---|
| `Platform` (интерфейс) | Абстракция мессенджер-платформы (VK, Telegram и т.д.) |
| `VKPlatform` | Реализация платформы для VK через Long Poll API |
| `Bot` | Ядро: приём сообщения, разбор команды, вызов обработчика |
| `Command` (интерфейс) | Контракт команды: имя, описание, детальная справка, исполнение |
| `CommandRegistry` | Реестр команд; при добавлении команды справка подхватывается автоматически |
| `AuthorCommand` | Команда `/author` — информация об авторах |
| `AboutCommand` | Команда `/about` — назначение и концепция бота |
| `HelpCommand` | Команда `/help` — список команд и справка по конкретной команде |
| `Main` | Точка входа: сборка всех компонентов и запуск платформы |

### Структура каталогов

```
study-bot/
+- pom.xml
+- README.md
+- .gitignore
+- src/
    +- main/
    |   +- java/ru/studybot/
    |       +- Main.java
    |       +- bot/
    |       |   +- Bot.java
    |       +- command/
    |       |   +- Command.java
    |       |   +- CommandRegistry.java
    |       |   +- AuthorCommand.java
    |       |   +- AboutCommand.java
    |       |   +- HelpCommand.java
    |       +- platform/
    |           +- Platform.java
    |           +- vk/
    |               +- VKPlatform.java
    +- test/
        +- java/ru/studybot/
            +- bot/
            |   +- BotTest.java
            +- command/
                +- AuthorCommandTest.java
                +- AboutCommandTest.java
                +- HelpCommandTest.java
```

## Команды

| Команда | Описание |
|---|---|
| `/author` | Выводит информацию об авторах проекта |
| `/about` | Описывает назначение и концепцию бота |
| `/help` | Список всех доступных команд |
| `/help <command>` | Подробная справка по конкретной команде |

### Принцип автоматической справки

Команды реализуют интерфейс `Command`, который требует описать имя и описание. `HelpCommand` берёт список команд из `CommandRegistry` автоматически — при добавлении новой команды достаточно вызвать `registry.register(newCmd)`, и команда сразу появится в `/help` без правки `HelpCommand`.

## Устройство базы данных

Используется **PostgreSQL**. База данных хранит пользователей, их прогресс, учебные материалы и разбивку материалов на блоки-уроки.

### Схема

```
+-----------------------------+      +-----------------------------+
|         materials           |      |            users            |
|-----------------------------|      |-----------------------------|
| id (PK)                     |      | id (PK)                     |
| title                       |<--+  | vk_id (UNIQUE)              |
| description                 |   |  | username                    |
| total_blocks                |   |  | created_at                  |
| created_at                  |   |  +-------------+--------------+
+----------+------------------+   |                |
           |                     |                |
           v                     |                v
+-----------------------------+   |  +-----------------------------+
|     material_blocks         |   |  |       user_progress         |
|-----------------------------|   |  |-----------------------------|
| id (PK)                     |   |  | id (PK)                     |
| material_id (FK)-------------+  |  | user_id (FK)-----------------+
| block_number                |   |  | material_id (FK)-------------+
| title                       |   |  | current_block               |
| content                     |   |  | score                       |
| question                    |   |  | status                      |
| created_at                  |   |  | schedule_interval           |
+-----------------------------+   |  | last_studied_at             |
                                  |  +-----------------------------+
                                  +--+
```

### SQL-скрипт инициализации

```sql
-- Таблица учебных материалов (общая для всех пользователей)
CREATE TABLE materials (
    id           SERIAL PRIMARY KEY,
    title        VARCHAR(500) NOT NULL,
    description  TEXT,
    total_blocks INTEGER NOT NULL DEFAULT 0,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Таблица блоков-уроков, на которые разбит материал
CREATE TABLE material_blocks (
    id           SERIAL PRIMARY KEY,
    material_id  INTEGER NOT NULL REFERENCES materials(id) ON DELETE CASCADE,
    block_number INTEGER NOT NULL,
    title        VARCHAR(500),
    content      TEXT NOT NULL,
    question     TEXT,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (material_id, block_number)
);

-- Таблица пользователей
CREATE TABLE users (
    id         SERIAL PRIMARY KEY,
    vk_id      BIGINT UNIQUE NOT NULL,
    username   VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Таблица прогресса каждого пользователя по конкретному материалу
CREATE TABLE user_progress (
    id                SERIAL PRIMARY KEY,
    user_id           INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    material_id       INTEGER NOT NULL REFERENCES materials(id) ON DELETE CASCADE,
    current_block     INTEGER NOT NULL DEFAULT 1,
    score             INTEGER NOT NULL DEFAULT 0,
    status            VARCHAR(50) NOT NULL DEFAULT 'active',
    schedule_interval INTEGER NOT NULL DEFAULT 1,
    last_studied_at   TIMESTAMP,
    UNIQUE (user_id, material_id)
);
```

### Логика связей

1. **materials** — общая таблица учебных материалов. Один материал могут изучать несколько пользователей.
2. **material_blocks** — разбивка материала на блоки-уроки. Каждый блок содержит текст и проверочный вопрос.
3. **users** — пользователи, идентифицируемые по `vk_id`.
4. **user_progress** — связка «пользователь и материал»: текущий блок, оценка, статус обучения, интервал расписания, дата последнего изучения. Связь `user_id + material_id` уникальна.

## Технологии

- **Java 17**
- **Apache Maven** — управление зависимостями и сборка
- **Jackson** — парсинг JSON (ответы VK API)
- **PostgreSQL** — база данных
- **JUnit 5** — модульное тестирование
- **SLF4J** — логирование

## Запуск

### Требования

- Java 17+
- Maven 3.9+
- PostgreSQL 14+

### Сборка

```bash
mvn clean package
```

### Запуск

```bash
export VK_TOKEN="ваш_токен_сообщества_vk"
java -jar target/study-bot-1.0-SNAPSHOT.jar
```

### Запуск тестов

```bash
mvn test
```

## Git-репозиторий

Проект ведётся в Git с соблюдением гигиены коммитов:

- Каждая задача — в отдельной ветке: `feat/bot-core`, `feat/vk-platform`, `feat/commands` и т.д.
- Коммиты атомарны, с понятными сообщениями.
- Слияние в `main` через Pull Request.
- Формат сообщений: `type: краткое описание` (например, `feat: add author command`).

## План декомпозиции

| Задача | Содержание |
|---|---|
| 1 | Каркас бота, команды author/about/help, платформа VK, тесты |
| 2 | Загрузка и хранение материалов в БД, разделение на блоки-уроки |
| 3 | Система проверочных вопросов и оценка усвоения |
| 4 | Расписание изучения и корректировка плана |
| 5 | Многопользовательский режим: общая база материалов, индивидуальный прогресс |
| 6 | Дополнительные команды: просмотр прогресса, добавление материала и др. |
