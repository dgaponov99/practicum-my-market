# Бэкенд приложения Витрина магазина (реактивный стек)

### Состав:
- my-market-app: основной бэкенд витрины. Хранит данные, обрабатывает внешние запросы
- my-market-payment: сервис платежей. Хранит данные счета

### Требования:
- jdk 21
- docker engine

### Настройка keycloak:
- Создать realms `my-market`
- Создать глобальную роль в realm: `PAYMENT`
- Создать клиентов:
  - `app` - основное приложение
    - `Client authentification`
    - `Standart flow`
    - `Service accounts roles`
    - Root URL: `http://localhost:80`
    - Home URL: `http://localhost:80`
    - Service accounts roles: `PAYMENT`
  - `payment-service` - сервис платежей
    - `Client authentification`
    - `Standart flow`

### Настройка окружения
- Указать данные для подключения к реляционной базе данных в `application.properties` или в переменных окружения
- Указать client-secret для основного приложения

### Запуск тестов (модульные):
- Вызов `mvnw clean test`

### Запуск тестов (модульные и интеграционные):
- Запуск docker-engine
- Вызов `mvnw clean verify`

### Сборка jar:
- Вызов `mvnw clean package`
- Собранный fat-jar находится в `/target` в соответствующих приложениях

### Локальный запуск в docker:
- Вызов `mvnw clean package`
- Настроить параметры подключения до postgres и redis в `docker-compose.yaml`
- Запустить docker-engine
- Выполнить `docker-compose up -d`
- После успешной сборки и запуска приложение будет доступно по адресу `http://localhost`
