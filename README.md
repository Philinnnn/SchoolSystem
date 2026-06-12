# SchoolSystem

Веб-приложение для управления учебным процессом школы: оценки, расписание, классы, пользователи с ролевым доступом. Реализовано на Spring Boot с акцентом на безопасность (2FA, аудит действий, защита от атак).

## Стек

- **Backend:** Java, Spring Boot, Spring Security, Spring Data JPA
- **БД:** Microsoft SQL Server, миграции через Flyway
- **Frontend:** Thymeleaf, HTML/CSS
- **Интеграции:** Telegram Bot API (вход через Telegram, уведомления)
- **Безопасность:** TOTP-двухфакторная аутентификация, OAuth2, кастомный WAF-фильтр, rate limiting, HSTS, журналирование действий (audit log)

## Возможности

- Ролевая модель: администратор, директор, учитель, ученик — у каждой роли свой набор страниц и прав
- Управление классами, предметами, расписанием
- Журнал оценок и успеваемости
- Авторизация по логину/паролю с поддержкой TOTP (Google Authenticator и аналоги) и входом через Telegram
- Автоматические бэкапы базы данных
- Аудит-лог действий пользователей (вход, выход, ошибки авторизации)
- Встроенный WAF-фильтр и rate limiting для базовой защиты от атак
- Панель настроек приложения для администратора

## Запуск проекта

### Требования

- Java 17+
- Maven
- Microsoft SQL Server (локально или в Docker)

### Настройка окружения

Создайте файл `.env` в корне проекта (или `.env.dist` как шаблон) со следующими переменными:

```env
DB_URL=jdbc:sqlserver://localhost:1433;databaseName=schoolsystem;encrypt=true;trustServerCertificate=true
DB_USERNAME=sa

TELEGRAM_BOT_TOKEN=your_telegram_bot_token
TELEGRAM_BOT_USERNAME=your_bot_username
TELEGRAM_LOGIN_TOKEN_TTL_MINUTES=5
```

### Запуск базы данных (Docker)

```bash
docker run -e "ACCEPT_EULA=Y" -e "MSSQL_SA_PASSWORD=YourPassword123" \
  -p 1433:1433 --name schoolsystem-db -d mcr.microsoft.com/mssql/server:2022-latest
```

При первом запуске приложения миграции Flyway автоматически создадут схему и тестовые данные.

### Сборка и запуск

```bash
mvn clean install
mvn spring-boot:run
```

Приложение будет доступно по адресу `http://localhost:8089`.

## Структура проекта

```
src/main/java/.../schoolsystem/
├── config/        # конфигурация приложения, бэкапы, Telegram, env loader
├── controller/     # контроллеры по ролям (admin, director, teacher, student) и общие
├── entity/         # JPA-сущности
├── repository/     # Spring Data репозитории
├── security/       # Spring Security, 2FA, WAF, rate limiting, аудит
├── service/        # бизнес-логика
└── telegram/       # интеграция с Telegram Bot API
src/main/resources/
├── db/migration/   # Flyway-миграции
├── templates/      # Thymeleaf-шаблоны по ролям
└── static/         # статические ресурсы (CSS)
```

## Примечания

Проект разработан в учебных целях. Часть параметров безопасности (например, WAF) настраивается через `application.properties` и может быть отключена для разработки.
