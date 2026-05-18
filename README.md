#  Anime Tracker

**Anime Tracker** - это клиент-серверное веб-приложение для ведения персонального каталога аниме. Позволяет добавлять произведения в коллекцию, писать рецензии, ставить оценки и отслеживать статус выхода новых эпизодов.

**Хостинг (Render + Neon + Cloudflare):** https://mediatracker-84o.pages.dev/  

## ️ Основные возможности

- **Каталог аниме:**
   - CRUD операции для аниме.
   - Фильтрация по студии и названию (@RequestParam).
   - Детальный просмотр по ID (@PathVariable).
   - Сортировка списка по убыванию популярности.
   - Сложные GET-запросы с фильтрацией по жанру и минимальному числу сезонов (JPQL и нативный SQL).
   - Пагинация (Pageable).
   - Кэширование запросов с использованием `HashMap` и составного ключа; автоматическая инвалидация кэша.
   - Импорт данных из внешних API (AniList, Jikan).

- **Пользователи и личные коллекции:**
   - Регистрация и удаление пользователей.
   - Добавление аниме в личную коллекцию.
   - Просмотр всех аниме пользователя с сортировкой по оценке.
   - Детальная информация о конкретном аниме в коллекции.
   - Обновление оценки и рецензии (PUT) и удаление аниме из коллекции (DELETE).

- **Отзывы и оценки:**
   - Получение, создание, обновление и удаление отзывов (с валидацией входных данных).

- **Архитектура и технологии:**
   - Многослойная архитектура: Controller → Service → Repository.
   - Полноценные DTO и ручные мапперы.
   - Реляционная база данных PostgreSQL с JPA/Hibernate.
   - Корректная обработка циклических ссылок при сериализации JSON.
   - Настройка каскадных операций (`CascadeType.ALL`) и типов загрузки (`FetchType.LAZY/EAGER`).
   - Демонстрация атомарности транзакций с `@Transactional`.
   - Глобальная обработка ошибок через `@ControllerAdvice` с единым форматом ответа.
   - Валидация входных данных с помощью `@Valid` и аннотаций Bean Validation.
   - Логирование через Logback с ротацией файлов и разными уровнями для пакетов.
   - AOP-аспект для логирования времени выполнения методов сервисов.
   - Документация API через Swagger/OpenAPI (доступна по `/swagger-ui/index.html`).
   - Код приведён к Google Java Style (Checkstyle).

##  Стек технологий

| Категория              | Технологии                                              |
| ---------------------- |---------------------------------------------------------|
| **Язык и платформа**   | Java 17, Spring Boot 3, Maven                           |
| **Web и REST**         | Spring Web, Jackson                                     |
| **Базы данных и ORM**  | PostgreSQL, Spring Data JPA / Hibernate                 |
| **Валидация и AOP**    | Spring Validation (@Valid, Bean Validation), Spring AOP |
| **Логирование и API**  | Logback, springdoc-openapi (Swagger/OpenAPI UI)         |
| **Утилиты и стиль**    | Lombok, Checkstyle (Google Java Style)                  |
| **Тестирование**       | JUnit, Mockito, GitHub Actions (CI/CD)                  |

##  Полезные ссылки

- [Swagger UI](https://mediatracker-a2tr.onrender.com/swagger-ui/index.html)
- [OpenAPI спецификация](https://mediatracker-a2tr.onrender.com/swagger-ui/index.html/v3/api-docs)
- [SonarCloud](https://sonarcloud.io/project/configuration/AutoScan?id=darfe-e_MediaTracker)