# Anime Tracker

Anime Tracker — это клиент-серверное веб-приложение для ведения персонального каталога аниме.
Позволяет добавлять произведения, писать рецензии, ставить оценки и отслеживать статус выхода новых эпизодов.

---

## Информация о последней версии

**Версия 0.2.0**

Общий каталог аниме (/anime-catalogue):

- CRUD для аниме (создание, получение по ID, получение списка, обновление, удаление).
- GET с фильтрацией по студии и названию (@RequestParam).
- GET с переменной пути (@PathVariable) для детального просмотра.
- Сортировка списка по убыванию популярности.

Пользователи и личные коллекции (/users, /anime-collection):

- Регистрация и удаление пользователей.
- Добавление аниме в личную коллекцию (POST /anime-collection?userId=...&animeId=...).
- Просмотр всех аниме пользователя с сортировкой по оценке.
- Детальная информация о конкретном аниме в коллекции пользователя.
- Обновление оценки и рецензии (PUT).
- Удаление аниме из коллекции (DELETE).

Модели и архитектура:

- Сущности: Anime, Season, Episode, User, AnimeUser, Genre (связь пользователя с аниме).
- Многослойная архитектура: Controller → Service → Repository (in-memory хранилище).
- Полноценные DTO и ручные мапперы для всех объектов.
- Корректная обработка циклических ссылок при сериализации JSON.
- Реляционная база данных PostgreSQL с JPA/Hibernate.
- Связи: @OneToMany/@ManyToOne (аниме ↔ сезоны ↔ эпизоды, пользователи ↔ коллекция), @ManyToMany (аниме ↔ жанры).
- Настроены каскадные операции (CascadeType.ALL) и типы загрузки (FetchType.LAZY/EAGER).
- Демонстрация атомарности транзакций с @Transactional: частичное сохранение без транзакции и полный откат при ошибке.

Обработка ошибок: все эндпоинты возвращают соответствующие HTTP-статусы (200, 201, 400, 404, 204).

Checkstyle: код приведён к Google Java Style.

---

## Стек технологий (на данном этапе)

- Java 17
- Spring Boot 3
- Maven
- Spring Web (REST-контроллеры)
- Spring Data JPA / Hibernate
- PostgreSQL
- Ручной маппер (класс-утилита)
- Lombok (геттеры, сеттеры, конструкторы)
- Checkstyle (Google Java Style)
- README

---

## Запуск последней версии

**Требования к окружению:**

- Java 17 или новее
- Maven 3.8 или новее

**Инструкция по запуску:**

1. Клонируйте репозиторий:
   > git clone https://github.com/darfe-e/MediaTracker

   > cd MediaTracker

2. Создайте базу данных в PostgreSQL:
   > CREATE DATABASE anime_tracker_db;

3. Соберите и запустите приложение:
   > mvn spring-boot:run

4. После запуска API будет доступен по адресу:
   > http://localhost:8080

5. Остановка приложения — Ctrl+C в терминале с запущенным Spring Boot.

## Инструкция по использованию

На текущем этапе взаимодействие с приложением осуществляется исключительно через REST API.
Для тестирования можно использовать Postman.

**Базовые URL**

- /anime-catalogue - Каталог со всеми аним е	
- /users - Пользователи
- /anime-collection - Коллекция пользователя	

**Основные эндпоинты**

**Каталог аниме**
- GET /anime-catalogue – список аниме (фильтрация по studio, title)
- GET /anime-catalogue/{id} – детальная информация об аниме (с сезонами и эпизодами)
- GET /anime-catalogue//without-probl/{id} – оптимизированная загрузка с join fetch (один запрос)
- POST /anime-catalogue – создание нового аниме
- DELETE /anime-catalogue/{id} – удаление аниме

**Пользователи**
- POST /users?name=... – регистрация пользователя
- DELETE /users/{id} – удаление пользователя

**Избранное пользователей**
- GET /users/{userId}/favorites – список избранного пользователя
- GET /users/{userId}/favorites/{animeId} – информация о конкретной записи
- POST /users/{userId}/favorites/{animeId} – добавить аниме в избранное
- DELETE /users/{userId}/favorites/{animeId} – удалить из избранного

**Отзывы пользователей**
- GET /users/{userId}/review/{animeId} – получить отзыв
- GET /users/{userId}/review – получить все отзывы
- POST /users/{userId}/review – создать отзыв (тело запроса – ReviewDto)
- PUT /users/{userId}/review – обновить отзыв (тело запроса – ReviewDto)
- DELETE /users/{userId}/review/{animeId} – удалить отзыв

## Проверка в SonarCloud

 >https://sonarcloud.io/project/configuration/AutoScan?id=darfe-e_MediaTracker