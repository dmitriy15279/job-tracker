# Job Tracker — подробное объяснение проекта для начинающего программиста

Этот файл написан для того, чтобы человек, который **впервые видит этот проект**, понял:
- зачем нужен каждый файл;
- как эти файлы связаны друг с другом;
- что делает почти каждая строчка кода.

Файлы, относящиеся к UI (визуальному интерфейсу в браузере: `src/main/resources/static/index.html`,
`style.css`, `app.js`), в этом документе **намеренно не рассматриваются** — здесь описан только backend
(серверная часть) на Java/Spring Boot.

---

## 0. Что вообще делает этот проект

Job Tracker — это простой REST API (веб-сервис, с которым общаются через HTTP-запросы в формате JSON)
для отслеживания откликов на вакансии. Пользователь может:
- создать запись о том, что он откликнулся на вакансию (компания, должность, дата отклика);
- посмотреть список всех откликов;
- посмотреть один отклик по его номеру (id).

Технологии:
- **Java 21** — язык программирования.
- **Spring Boot 4** — фреймворк, который берёт на себя всю "инфраструктурную" работу: поднимает
  веб-сервер, обрабатывает HTTP-запросы, связывает объекты друг с другом (Dependency Injection) и т.д.
- **PostgreSQL** — реляционная база данных, где хранятся данные.
- **Flyway** — инструмент для управления версиями схемы базы данных (миграции).
- **Gradle** — инструмент сборки проекта (компилирует код, скачивает зависимости, собирает jar-файл).
- **Docker / docker-compose** — упаковка приложения и базы данных в контейнеры для лёгкого запуска.
- **Lombok** — библиотека, которая автоматически генерирует "скучный" код (геттеры/сеттеры/конструкторы).

### Общая архитектура (как файлы связаны друг с другом)

Проект построен по классической слоистой архитектуре Spring MVC:

```
HTTP-запрос от клиента (например, браузера или Postman)
        │
        ▼
[Controller]  — принимает HTTP-запрос, проверяет входные данные, вызывает Service
        │
        ▼
[Service]     — здесь бизнес-логика: что делать с данными, как их преобразовать
        │
        ▼
[Repository]  — интерфейс, через который Service обращается к базе данных
        │
        ▼
[PostgreSQL]  — сама база данных, таблицы которой созданы через Flyway-миграции
```

Ниже — разбор каждого файла в том порядке, в котором проще всего понять проект: сначала конфигурация
и точка входа, потом слой хранения данных, потом бизнес-логика, потом веб-слой, потом тесты и
инфраструктура (Docker).

---

## 1. `settings.gradle` — имя проекта и настройка Gradle

Путь: `settings.gradle`

Это самый первый файл, который Gradle читает при сборке проекта. Он объясняет Gradle, "что это за
проект" и подключает вспомогательные механизмы сборки.

```groovy
plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '1.0.0'
}

rootProject.name = 'job-tracker'
```

Построчно:
- `plugins { ... }` — блок, в котором подключаются плагины Gradle (расширения, добавляющие функциональность
  системе сборки).
- `id 'org.gradle.toolchains.foojay-resolver-convention' version '1.0.0'` — подключает плагин, который
  умеет **автоматически скачивать нужную версию Java (JDK)**, если она не установлена на компьютере.
  Это нужно, потому что дальше в `build.gradle` указано требование "Java 21", и если у разработчика
  её нет — Gradle сам её найдёт и скачает через сервис Foojay.
- `rootProject.name = 'job-tracker'` — задаёт имя корневого (единственного) проекта — `job-tracker`.
  Это имя используется, например, в названии собранного jar-файла.

---

## 2. `build.gradle` — описание сборки, зависимостей и версий

Путь: `build.gradle`

Это главный конфигурационный файл Gradle. Он говорит: какой язык используем, какие библиотеки
(зависимости) нужны проекту, и как запускать тесты.

```groovy
plugins {
	id 'java'
	id 'org.springframework.boot' version '4.1.1'
	id 'io.spring.dependency-management' version '1.1.7'
}
```
- `id 'java'` — базовый плагин Gradle, который добавляет поддержку компиляции Java-кода (задачи
  `compileJava`, `test`, `jar` и т.д.).
- `id 'org.springframework.boot' version '4.1.1'` — плагин Spring Boot. Он умеет собирать
  "исполняемый" (fat/uber) jar-файл, в который упакованы и код приложения, и все его зависимости —
  такой jar можно запустить командой `java -jar app.jar` без установки отдельного сервера приложений.
  Версия `4.1.1` — это версия самого Spring Boot.
- `id 'io.spring.dependency-management' version '1.1.7'` — плагин, который позволяет не указывать
  версии для каждой библиотеки Spring вручную: Spring Boot сам подбирает совместимые версии всех
  зависимостей (это называется "BOM" — Bill of Materials).

```groovy
group = 'com.example'
version = '0.0.1-SNAPSHOT'
```
- `group` — идентификатор организации/пакета проекта (аналог "groupId" в Maven). Совпадает с корневым
  Java-пакетом `com.example.jobtracker`.
- `version` — версия самого проекта. `SNAPSHOT` означает "нестабильная версия в разработке" — так
  принято помечать версии, которые ещё не являются финальным релизом.

```groovy
java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}
```
- Указывает Gradle, что для компиляции и запуска нужно использовать **Java 21** (LTS-версия). Если на
  компьютере такой версии нет, сработает плагин из `settings.gradle` и скачает её автоматически.

```groovy
repositories {
	mavenCentral()
}
```
- Говорит Gradle, откуда скачивать библиотеки (зависимости) — из публичного репозитория Maven Central,
  где хранится подавляющее большинство Java/Kotlin библиотек с открытым кодом.

```groovy
dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
	implementation 'org.springframework.boot:spring-boot-starter-flyway'
	implementation 'org.springframework.boot:spring-boot-starter-validation'
	implementation 'org.springframework.boot:spring-boot-starter-webmvc'
	implementation 'org.flywaydb:flyway-database-postgresql'
	compileOnly 'org.projectlombok:lombok'
	runtimeOnly 'org.postgresql:postgresql'
	annotationProcessor 'org.projectlombok:lombok'
	testImplementation 'org.springframework.boot:spring-boot-starter-data-jpa-test'
	testImplementation 'org.springframework.boot:spring-boot-starter-flyway-test'
	testImplementation 'org.springframework.boot:spring-boot-starter-validation-test'
	testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
	testCompileOnly 'org.projectlombok:lombok'
	testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
	testAnnotationProcessor 'org.projectlombok:lombok'
}
```
Это список всех библиотек, от которых зависит проект. Разберём каждую:

- `implementation 'org.springframework.boot:spring-boot-starter-data-jpa'` — "стартер" (готовый набор
  зависимостей) для работы с базами данных через JPA (Java Persistence API) и Hibernate. Даёт нам
  возможность писать `@Entity`-классы и `JpaRepository`-интерфейсы (см. ниже).
- `implementation 'org.springframework.boot:spring-boot-starter-flyway'` — стартер для Flyway —
  инструмента миграций базы данных (см. раздел про `V1__create_job_applications_table.sql`).
- `implementation 'org.springframework.boot:spring-boot-starter-validation'` — стартер для валидации
  входных данных через аннотации типа `@NotBlank`, `@NotNull` (используется в `CreateJobApplicationRequest`).
- `implementation 'org.springframework.boot:spring-boot-starter-webmvc'` — стартер для создания
  веб-приложений/REST API на основе Spring MVC (даёт `@RestController`, встроенный веб-сервер Tomcat
  и т.д.).
- `implementation 'org.flywaydb:flyway-database-postgresql'` — модуль Flyway, который умеет работать
  конкретно с PostgreSQL (у Flyway разные модули для разных СУБД).
- `compileOnly 'org.projectlombok:lombok'` — подключает Lombok, но только на этапе компиляции (сама
  библиотека не попадает в финальный jar — она нужна лишь для генерации кода во время сборки).
- `runtimeOnly 'org.postgresql:postgresql'` — JDBC-драйвер PostgreSQL. Нужен только во время выполнения
  программы (когда она реально подключается к базе), а не во время компиляции — поэтому `runtimeOnly`.
- `annotationProcessor 'org.projectlombok:lombok'` — заставляет компилятор Java запускать Lombok
  как "annotation processor" — то есть на этапе компиляции Lombok реально дописывает код (геттеры,
  сеттеры, конструкторы) в класс, помеченный аннотациями типа `@Getter`.
- `testImplementation '...-data-jpa-test'`, `'...-flyway-test'`, `'...-validation-test'`,
  `'...-webmvc-test'` — тестовые версии тех же стартеров, доступные только при запуске тестов
  (`src/test`), не попадающие в основной код приложения.
- `testCompileOnly 'org.projectlombok:lombok'` и `testAnnotationProcessor 'org.projectlombok:lombok'` —
  то же самое, что и выше, но для тестового кода — на случай, если в тестах тоже используются
  Lombok-аннотации.
- `testRuntimeOnly 'org.junit.platform:junit-platform-launcher'` — библиотека, нужная для фактического
  запуска тестов JUnit 5 во время выполнения задачи `test`.

```groovy
tasks.named('test') {
	useJUnitPlatform()
}
```
- Настраивает встроенную Gradle-задачу `test` так, чтобы она использовала **JUnit 5** (JUnit Platform)
  как движок для запуска тестов — без этой строки Gradle не будет знать, каким способом запускать
  тестовые классы, написанные в стиле JUnit 5 (аннотация `@Test` из `org.junit.jupiter.api`).

---

## 3. `gradle/wrapper/gradle-wrapper.properties` — версия Gradle

Путь: `gradle/wrapper/gradle-wrapper.properties`

Этот файл — часть механизма "Gradle Wrapper" (`gradlew` / `gradlew.bat`). Он гарантирует, что у всех
разработчиков и на сервере сборки используется **одна и та же версия Gradle**, без необходимости
устанавливать Gradle вручную.

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-9.5.1-bin.zip
networkTimeout=10000
retries=0
retryBackOffMs=500
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```
- `distributionBase=GRADLE_USER_HOME` / `zipStoreBase=GRADLE_USER_HOME` — говорит, что скачанный
  дистрибутив Gradle нужно хранить в домашней папке пользователя Gradle (обычно `~/.gradle`), а не
  внутри самого проекта.
- `distributionPath=wrapper/dists` / `zipStorePath=wrapper/dists` — подпапка внутри `GRADLE_USER_HOME`,
  куда именно сохраняется скачанный архив и распакованная версия.
- `distributionUrl=https\://services.gradle.org/distributions/gradle-9.5.1-bin.zip` — прямая ссылка на
  архив с конкретной версией Gradle — **9.5.1**. Именно эта версия будет скачана и использована при
  запуске `./gradlew ...` (обратный слэш перед `:` — это экранирование символа в `.properties`-файле,
  не влияет на сам URL).
- `networkTimeout=10000` — сколько миллисекунд ждать ответа сети при скачивании (10 секунд).
- `retries=0` — сколько раз повторить попытку скачивания при неудаче (здесь — не повторять).
- `retryBackOffMs=500` — пауза перед повторной попыткой (не используется, так как `retries=0`).
- `validateDistributionUrl=true` — включает проверку, что скачанный файл действительно соответствует
  ожидаемому дистрибутиву Gradle (защита от повреждённых/поддельных файлов).

(Файлы `gradlew`, `gradlew.bat` и `gradle-wrapper.jar` — это исполняемые скрипты/бинарник, которые
собственно и скачивают/запускают нужную версию Gradle; они генерируются автоматически и обычно не
редактируются вручную, поэтому подробно построчно не разбираются.)

---

## 4. `Application.java` — точка входа в приложение

Путь: `src/main/java/com/example/jobtracker/Application.java`

Это класс, с которого начинается выполнение всей программы — в нём находится метод `main`.

```java
package com.example.jobtracker;
```
- Объявляет, что этот класс принадлежит пакету `com.example.jobtracker` — корневому пакету всего
  проекта. Все остальные классы лежат во вложенных пакетах (`controller`, `service`, `persistence`
  и т.д.), и Spring Boot по умолчанию сканирует **все классы внутри и ниже** пакета, в котором лежит
  главный класс — это называется "component scanning".

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
```
- Импортируем два класса из Spring Boot:
  - `SpringApplication` — класс с утилитными методами для запуска Spring-приложения.
  - `SpringBootApplication` — аннотация, включающая автоконфигурацию Spring Boot.

```java
@SpringBootApplication
public class Application {
```
- `@SpringBootApplication` — это "составная" аннотация, которая одновременно включает в себя три
  вещи:
  1. `@Configuration` — класс может содержать конфигурацию бинов (объектов, управляемых Spring);
  2. `@EnableAutoConfiguration` — Spring Boot автоматически настраивает нужные компоненты
     (веб-сервер, подключение к БД, JPA и т.д.) на основе того, какие библиотеки есть в classpath
     (например, если видит `spring-boot-starter-webmvc` — сам поднимет встроенный сервер Tomcat);
  3. `@ComponentScan` — Spring ищет и регистрирует все классы, помеченные аннотациями вроде
     `@RestController`, `@Service`, `@Repository`, начиная с текущего пакета и ниже.
- `public class Application { ... }` — сам класс, точка входа программы.

```java
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
```
- `public static void main(String[] args)` — стандартный метод Java, с которого начинается выполнение
  любой Java-программы, запускаемой напрямую (например, командой `java -jar app.jar`).
- `SpringApplication.run(Application.class, args);` — запускает весь механизм Spring Boot:
  - поднимает контекст приложения (объект, который хранит все созданные Spring-объекты — "бины");
  - запускает встроенный веб-сервер (Tomcat) на порту 8080 (по умолчанию);
  - подключается к базе данных, применяет Flyway-миграции, инициализирует JPA;
  - `Application.class` — говорит, с какого класса начинать сканирование конфигурации;
  - `args` — аргументы командной строки, переданные при запуске программы (например,
    `java -jar app.jar --server.port=9090`).

---

## 5. `application.yml` — конфигурация приложения

Путь: `src/main/resources/application.yml`

Файл в формате YAML, который Spring Boot читает при старте и использует для настройки поведения
приложения (подключение к БД, поведение JPA, Flyway и т.д.). Файл лежит в `src/main/resources`,
потому что Spring Boot автоматически ищет файлы `application.properties`/`application.yml` именно
там.

```yaml
spring:
  application:
    name: job-tracker
```
- `spring.application.name` — логическое имя приложения. Используется, например, в логах, в
  Actuator-эндпоинтах (если подключены) или при регистрации в системах вроде Eureka (в этом проекте
  их нет, но имя всё равно полезно для идентификации приложения).

```yaml
  datasource:
    url: jdbc:postgresql://localhost:5433/job_tracker
    username: postgres
    password: postgres
```
- `spring.datasource.url` — JDBC-адрес базы данных PostgreSQL: протокол `jdbc:postgresql://`, хост
  `localhost`, порт `5433` (нестандартный, потому что стандартный порт PostgreSQL 5432 может быть
  занят другой локальной установкой Postgres — см. `docker-compose.yml`, где именно порт 5433 хоста
  пробрасывается на 5432 внутри контейнера), и имя базы данных `job_tracker`.
- `spring.datasource.username` / `password` — логин и пароль для подключения к базе. Здесь заданы
  простые значения `postgres`/`postgres`, подходящие для локальной разработки (в реальном продакшене
  такие секреты обычно не хранят в открытом виде в репозитории, а передают через переменные окружения
  или секрет-хранилища — заметьте, что в `docker-compose.yml` для контейнера `app` эти значения как
  раз переопределяются через переменные окружения `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`).

```yaml
  jpa:
    hibernate:
      ddl-auto: validate
```
- `spring.jpa.hibernate.ddl-auto` — управляет тем, что Hibernate (реализация JPA) делает со схемой
  базы данных при старте. Значение `validate` означает: **Hibernate только проверяет**, что структура
  таблиц в базе соответствует Java-сущностям (`@Entity`-классам), но **не создаёт и не изменяет** схему
  автоматически. Если структура не совпадает — приложение не запустится с ошибкой. Это осознанное
  архитектурное решение проекта (см. `CLAUDE.md`): единственный способ менять схему БД — писать новую
  Flyway-миграцию.

```yaml
  flyway:
    enabled: true
```
- `spring.flyway.enabled: true` — явно включает Flyway. При старте приложения Flyway просканирует папку
  `src/main/resources/db/migration`, найдёт там SQL-файлы миграций (например, `V1__...sql`) и применит
  те из них, которые ещё не были применены к текущей базе данных (Flyway хранит служебную таблицу
  `flyway_schema_history`, где отмечает, какие миграции уже выполнены).

---

## 6. `db/migration/V1__create_job_applications_table.sql` — первая Flyway-миграция

Путь: `src/main/resources/db/migration/V1__create_job_applications_table.sql`

Это обычный SQL-файл, который Flyway выполняет один раз при первом запуске (и больше никогда
повторно, если только вы не пересоздадите базу). Имя файла имеет строгий формат Flyway:
`V<номер_версии>__<описание>.sql` — `V1` означает "версия миграции №1", а `create_job_applications_table`
— это просто читаемое описание для человека (двойное подчёркивание `__` — обязательный разделитель).

```sql
CREATE TABLE job_applications (
    id BIGSERIAL PRIMARY KEY,
    company VARCHAR(255) NOT NULL,
    position VARCHAR(255) NOT NULL,
    applied_date DATE NOT NULL
);
```
- `CREATE TABLE job_applications ( ... );` — создаёт новую таблицу с именем `job_applications` в базе
  данных PostgreSQL.
- `id BIGSERIAL PRIMARY KEY` — колонка `id`:
  - `BIGSERIAL` — специальный тип PostgreSQL, который автоматически создаёт последовательность
    (`SEQUENCE`) для генерации уникальных возрастающих 64-битных целых чисел (аналог "автоинкремента"
    в других СУБД). Каждая новая строка получает следующее число автоматически, если явно не указать
    значение.
  - `PRIMARY KEY` — делает эту колонку первичным ключом: значения должны быть уникальными и не могут
    быть `NULL`; по этой колонке строится индекс для быстрого поиска.
- `company VARCHAR(255) NOT NULL` — колонка для названия компании, строка переменной длины до 255
  символов, `NOT NULL` означает, что значение обязательно (пустых/отсутствующих значений не может
  быть).
- `position VARCHAR(255) NOT NULL` — аналогично, колонка для названия должности/позиции.
- `applied_date DATE NOT NULL` — колонка с датой отклика (без времени, только дата), тоже обязательная.

Эта таблица напрямую соответствует Java-сущности `JobApplication` (см. ниже), при этом в Java поле
называется `appliedDate` (camelCase — принятый в Java стиль именования), а в базе — `applied_date`
(snake_case — принятый в SQL/PostgreSQL стиль); соответствие между ними задаётся аннотацией
`@Column(name = "applied_date")` в Java-классе.

---

## 7. `persistence/entity/JobApplication.java` — JPA-сущность (Entity)

Путь: `src/main/java/com/example/jobtracker/persistence/entity/JobApplication.java`

Это Java-класс, который представляет **одну строку таблицы `job_applications`** в виде объекта.
Такие классы называются "сущностями" (Entity) — с ними работает Hibernate/JPA, автоматически
превращая объекты Java в строки таблицы и обратно (это называется ORM — Object-Relational Mapping).

```java
package com.example.jobtracker.persistence.entity;
```
- Пакет `persistence.entity` — согласно архитектуре проекта (см. `CLAUDE.md`), здесь хранятся именно
  `@Entity`-классы, отдельно от репозиториев (`persistence`) и DTO (`controller.dto`).

```java
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
```
- `jakarta.persistence.*` — аннотации из спецификации JPA (пакет называется `jakarta`, а не старый
  `javax`, потому что проект использует новую версию спецификации Jakarta EE).
- `java.time.LocalDate` — стандартный класс Java для хранения "просто даты" без времени и часового
  пояса (идеально подходит для поля вроде "дата отклика").
- `lombok.*` — аннотации библиотеки Lombok, которые на этапе компиляции автоматически генерируют
  код (геттеры, сеттеры, конструкторы), чтобы не писать его руками.

```java
@Entity
@Table(name = "job_applications")
```
- `@Entity` — говорит JPA/Hibernate, что этот класс является сущностью и должен быть связан с таблицей
  в базе данных.
- `@Table(name = "job_applications")` — явно указывает имя таблицы в базе — `job_applications` (без
  этой аннотации Hibernate по умолчанию использовал бы имя класса, что могло бы не совпадать с реальным
  именем таблицы из SQL-миграции).

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
```
Это аннотации Lombok, которые **избавляют разработчика от написания шаблонного кода**:
- `@Getter` — автоматически генерирует метод-геттер для каждого приватного поля класса (например,
  `getId()`, `getCompany()`, `getPosition()`, `getAppliedDate()`).
- `@Setter` — аналогично генерирует методы-сеттеры (`setId(...)`, `setCompany(...)` и т.д.), которые
  позволяют изменять значения полей.
- `@NoArgsConstructor` — генерирует конструктор **без аргументов** (`public JobApplication() {}`).
  Он обязателен для JPA-сущностей: Hibernate создаёт объект сущности через пустой конструктор, а
  затем заполняет поля через рефлексию.
- `@AllArgsConstructor` — генерирует конструктор, принимающий **все поля класса** в качестве
  аргументов, в том порядке, в котором они объявлены (`id, company, position, appliedDate`). Именно
  этот конструктор используется в `JobApplicationService.create(...)` (см. ниже), чтобы одной строкой
  создать новый объект.

```java
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
```
- `private Long id;` — поле для первичного ключа. Тип `Long` (а не примитив `long`), потому что до
  сохранения в базу у новой сущности значения ещё нет, и оно должно уметь быть `null`.
- `@Id` — помечает поле как первичный ключ сущности (соответствует `PRIMARY KEY` в SQL).
- `@GeneratedValue(strategy = GenerationType.IDENTITY)` — говорит Hibernate, что значение этого поля
  **генерируется самой базой данных** при вставке строки (это соответствует `BIGSERIAL` в
  PostgreSQL-миграции — база сама присваивает следующее число из последовательности). Java-код никогда
  не устанавливает `id` вручную для новой записи — именно поэтому в `JobApplicationService.create()`
  туда передаётся `null`.

```java
    @Column(nullable = false)
    private String company;

    @Column(nullable = false)
    private String position;
```
- `private String company;` / `private String position;` — поля для названия компании и должности.
- `@Column(nullable = false)` — явно указывает, что колонка не может быть `NULL` в базе данных
  (соответствует `NOT NULL` в SQL-миграции). Имя колонки не указано явно, поэтому Hibernate использует
  имя самого поля (`company`, `position`) — они и так совпадают с именами колонок в базе.

```java
    @Column(name = "applied_date", nullable = false)
    private LocalDate appliedDate;
}
```
- `private LocalDate appliedDate;` — поле для даты отклика.
- `@Column(name = "applied_date", nullable = false)` — здесь имя поля в Java (`appliedDate`, camelCase)
  **не совпадает** с именем колонки в базе (`applied_date`, snake_case), поэтому имя колонки указано
  явно через `name = "applied_date"`. `nullable = false` — снова соответствует `NOT NULL` в миграции.

---

## 8. `persistence/JobApplicationRepository.java` — репозиторий (доступ к данным)

Путь: `src/main/java/com/example/jobtracker/persistence/JobApplicationRepository.java`

```java
package com.example.jobtracker.persistence;

import com.example.jobtracker.persistence.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
}
```

Это, пожалуй, самый "магический" файл в проекте, несмотря на то, что в нём нет тела кода вообще.

- `public interface JobApplicationRepository` — это просто **интерфейс** (в Java интерфейс описывает
  набор методов без реализации). Здесь он вообще не объявляет ни одного собственного метода.
- `extends JpaRepository<JobApplication, Long>` — интерфейс наследуется (расширяет) от
  `JpaRepository<T, ID>` — стандартного интерфейса из Spring Data JPA, где:
  - `T` (первый параметр-тип) = `JobApplication` — тип сущности, с которой работает репозиторий;
  - `ID` (второй параметр-тип) = `Long` — тип первичного ключа этой сущности.
- Просто унаследовавшись от `JpaRepository`, интерфейс **автоматически получает** десятки готовых
  методов, которые уже реализованы фреймворком "под капотом", например:
  - `save(entity)` — сохранить (вставить или обновить) сущность;
  - `findById(id)` — найти сущность по первичному ключу (возвращает `Optional<JobApplication>` —
    "может быть значение, а может не быть");
  - `findAll()` — получить список всех записей;
  - `deleteById(id)`, `count()`, `existsById(id)` и многие другие.
- Как это работает "под капотом": во время старта приложения Spring Data JPA сканирует все интерфейсы,
  унаследованные от `JpaRepository`, и **автоматически создаёт класс-реализацию** (proxy-объект) прямо
  во время выполнения программы, используя информацию о сущности (`JobApplication`) и её первичном
  ключе (`Long`). Разработчику не нужно писать ни одной строчки SQL или Java-кода для базовых операций
  CRUD (Create, Read, Update, Delete).
- Этот интерфейс используется в `JobApplicationService` (внедряется через конструктор) — сервис
  вызывает его методы `save(...)` и `findAll()`/`findById(...)`.

---

## 9. `controller/dto/CreateJobApplicationRequest.java` — DTO для входящего запроса

Путь: `src/main/java/com/example/jobtracker/controller/dto/CreateJobApplicationRequest.java`

DTO (Data Transfer Object) — это объект, который используется исключительно для передачи данных
между слоями/системами (в данном случае — данные, которые клиент присылает в теле HTTP-запроса
при создании нового отклика на вакансию). DTO намеренно отделены от JPA-сущностей — так изменения во
внутренней структуре базы данных не "протекают" наружу в публичный API, и наоборот.

```java
package com.example.jobtracker.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateJobApplicationRequest(
        @NotBlank String company,
        @NotBlank String position,
        @NotNull LocalDate appliedDate) {
}
```

- `import jakarta.validation.constraints.NotBlank;` и `NotNull` — аннотации из Jakarta Bean Validation
  (реализуется библиотекой Hibernate Validator, подключённой через `spring-boot-starter-validation`).
  Они описывают **правила проверки** входных данных.
- `public record CreateJobApplicationRequest(...)` — это Java **record** — специальный компактный вид
  класса (появился в Java 16+), предназначенный именно для неизменяемых объектов-данных ("носителей
  данных"). Одной строкой `record Имя(Тип поле1, Тип поле2, ...)` компилятор автоматически генерирует:
  - приватные `final`-поля для каждого параметра;
  - конструктор, принимающий все поля;
  - геттеры без префикса `get` — то есть просто `company()`, `position()`, `appliedDate()` (а не
    `getCompany()`);
  - корректные реализации `equals()`, `hashCode()` и `toString()`.
  Это идеально подходит для DTO, потому что такие объекты не должны изменяться после создания.
- `@NotBlank String company` — поле `company` типа `String`. Аннотация `@NotBlank` означает: значение
  не должно быть `null`, не должно быть пустой строкой `""`, и не должно состоять только из пробелов.
  Если клиент пришлёт пустое название компании — Spring вернёт ошибку валидации.
- `@NotBlank String position` — аналогичное правило для должности.
- `@NotNull LocalDate appliedDate` — поле `appliedDate` типа `LocalDate` (дата без времени).
  `@NotNull` требует, чтобы значение просто не было `null` (для дат `@NotBlank` не подходит, так как
  та аннотация работает только со строками — понятие "пустой строки" неприменимо к дате).
- Эти аннотации **сами по себе ничего не проверяют** — они лишь метаданные. Реальная проверка
  запускается благодаря аннотации `@Valid` в контроллере (см. `JobApplicationController.create(...)`)
  — именно там Spring Boot видит `@Valid` перед параметром и запускает валидатор перед вызовом метода.
  Если валидация не проходит — клиент автоматически получает ответ с кодом `400 Bad Request` и
  описанием ошибок, ещё до того, как выполнится хоть одна строчка кода внутри метода контроллера.

---

## 10. `controller/dto/JobApplicationResponse.java` — DTO для ответа клиенту

Путь: `src/main/java/com/example/jobtracker/controller/dto/JobApplicationResponse.java`

```java
package com.example.jobtracker.controller.dto;

import java.time.LocalDate;

public record JobApplicationResponse(
        Long id,
        String company,
        String position,
        LocalDate appliedDate) {
}
```

- Это тоже `record`, но предназначен для **исходящих** данных — того, что API отдаёт клиенту в ответ
  на запрос (сериализуется в JSON автоматически Spring MVC через библиотеку Jackson).
- `Long id` — идентификатор записи (в отличие от `CreateJobApplicationRequest`, здесь id есть, потому
  что клиенту нужно знать, под каким номером сохранилась запись — сам клиент этот id не присылает,
  его генерирует база данных).
- `String company`, `String position`, `LocalDate appliedDate` — те же данные, что и в запросе,
  просто возвращаются обратно клиенту вместе с присвоенным id.
- Здесь нет аннотаций валидации (`@NotBlank`, `@NotNull`), потому что валидация нужна только для
  **входящих** данных, а не для того, что сервер сам формирует и отправляет наружу.
- Отдельный DTO для ответа (вместо того, чтобы напрямую отдавать `JobApplication`-сущность) — хорошая
  практика: он не тянет за собой лишние JPA-аннотации/технические детали и даёт контроль над тем,
  что именно видит клиент в API.

---

## 11. `service/JobApplicationService.java` — бизнес-логика

Путь: `src/main/java/com/example/jobtracker/service/JobApplicationService.java`

Сервисный слой — это "мозг" приложения: здесь принимаются решения о том, что делать с данными,
происходит преобразование между сущностями (Entity) и DTO, и обрабатываются ситуации вроде
"запись не найдена".

```java
package com.example.jobtracker.service;

import com.example.jobtracker.persistence.entity.JobApplication;
import com.example.jobtracker.controller.dto.CreateJobApplicationRequest;
import com.example.jobtracker.controller.dto.JobApplicationResponse;
import com.example.jobtracker.persistence.JobApplicationRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
```
- Импортируем сущность, оба DTO, репозиторий, `List` (для списков), `HttpStatus` (перечисление кодов
  HTTP-статусов, например 404), аннотацию `@Service` и класс `ResponseStatusException` (специальное
  исключение Spring, которое автоматически превращается в HTTP-ответ с заданным статус-кодом).

```java
@Service
public class JobApplicationService {
```
- `@Service` — стереотипная аннотация Spring (по сути частный случай `@Component`), которая
  помечает класс как **бин** — объект, которым управляет Spring-контейнер: он сам создаёт этот
  объект при старте приложения и внедряет (Dependency Injection) его туда, где он нужен (например, в
  контроллер). Название `@Service` носит смысловую нагрузку — по конвенции такими помечают классы,
  содержащие бизнес-логику (в отличие от `@Controller`/`@RestController` — веб-слой, или
  `@Repository` — слой доступа к данным).

```java
    private final JobApplicationRepository repository;

    public JobApplicationService(JobApplicationRepository repository) {
        this.repository = repository;
    }
```
- `private final JobApplicationRepository repository;` — поле, хранящее ссылку на репозиторий.
  `final` означает, что после инициализации в конструкторе это поле нельзя переприсвоить — то есть
  зависимость неизменяема на протяжении жизни объекта.
- `public JobApplicationService(JobApplicationRepository repository) { this.repository = repository; }`
  — конструктор, принимающий репозиторий как параметр. Это называется **Dependency Injection через
  конструктор** — рекомендуемый в Spring способ внедрения зависимостей (в отличие от
  `@Autowired`-поля). Spring сам находит бин типа `JobApplicationRepository` (тот самый интерфейс,
  для которого Spring Data JPA сгенерировал реализацию) и передаёт его сюда автоматически при создании
  `JobApplicationService`.

### Метод `create`

```java
    public JobApplicationResponse create(CreateJobApplicationRequest request) {
        JobApplication jobApplication = new JobApplication(
                null,
                request.company(),
                request.position(),
                request.appliedDate());
        JobApplication saved = repository.save(jobApplication);
        return toResponse(saved);
    }
```
- Принимает на вход DTO запроса `CreateJobApplicationRequest` (уже провалидированный контроллером).
- `new JobApplication(null, request.company(), request.position(), request.appliedDate());` —
  создаёт новый объект сущности, используя тот самый конструктор со всеми полями, который
  сгенерировал Lombok через `@AllArgsConstructor`. Аргументы передаются в том же порядке, в котором
  поля объявлены в классе `JobApplication`: `id, company, position, appliedDate`.
  - Первый аргумент — `null` для `id`, потому что новая запись ещё не сохранена в базу и id ей
    присвоит сама база данных при вставке (см. `@GeneratedValue(strategy = GenerationType.IDENTITY)`).
  - `request.company()`, `request.position()`, `request.appliedDate()` — вызовы автоматически
    сгенерированных record-геттеров, которые достают значения из DTO запроса.
- `JobApplication saved = repository.save(jobApplication);` — вызывает унаследованный от
  `JpaRepository` метод `save(...)`. Так как у переданной сущности `id == null`, Hibernate понимает,
  что это **новая** запись, и выполняет `INSERT` в таблицу `job_applications`. После вставки
  PostgreSQL присваивает новый `id` (через `BIGSERIAL`), и Hibernate заполняет это значение в
  возвращаемом объекте `saved`.
- `return toResponse(saved);` — преобразует сохранённую сущность (уже с заполненным `id`) в DTO ответа
  и возвращает его.

### Метод `getAll`

```java
    public List<JobApplicationResponse> getAll() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }
```
- `repository.findAll()` — унаследованный метод `JpaRepository`, возвращающий список **всех** записей
  таблицы `job_applications` в виде `List<JobApplication>` (список сущностей).
- `.stream()` — превращает список в Stream API — механизм Java для последовательной/декларативной
  обработки коллекций (map/filter/reduce и т.д.).
- `.map(this::toResponse)` — применяет к каждому элементу потока метод `toResponse` (ссылка на метод
  текущего объекта `this` — эквивалент лямбды `jobApplication -> toResponse(jobApplication)`),
  преобразуя каждую сущность `JobApplication` в DTO `JobApplicationResponse`.
- `.toList()` — собирает результирующий поток обратно в неизменяемый `List<JobApplicationResponse>`
  (метод `toList()` появился в Java 16 как удобная замена более длинному
  `.collect(Collectors.toList())`).
- Итог: метод возвращает список всех откликов на вакансии, уже преобразованных в "безопасный для
  внешнего API" формат DTO.

### Метод `getById`

```java
    public JobApplicationResponse getById(Long id) {
        JobApplication jobApplication = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Job application " + id + " not found"));
        return toResponse(jobApplication);
    }
```
- `repository.findById(id)` — унаследованный метод, ищущий запись по первичному ключу. Возвращает
  `Optional<JobApplication>` — специальный контейнер Java, который либо содержит значение, либо пуст
  (используется вместо возврата `null`, чтобы явно заставить код обработать случай "значения нет").
- `.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job application " + id + " not found"))`
  — если `Optional` пуст (запись с таким `id` не найдена в базе), выполняется переданная лямбда,
  которая создаёт и выбрасывает (`throw`) исключение `ResponseStatusException` с кодом
  `HttpStatus.NOT_FOUND` (соответствует HTTP-статусу `404`) и текстовым сообщением, включающим сам
  `id`. Spring MVC перехватывает такие исключения на уровне веб-слоя автоматически и превращает их в
  корректный HTTP-ответ `404 Not Found` с этим сообщением — без необходимости писать отдельный
  обработчик ошибок (`@ExceptionHandler`). Именно поэтому в архитектуре проекта (см. `CLAUDE.md`)
  указано, что случаи "не найдено" сигнализируются через `ResponseStatusException`, а не через
  собственные классы исключений.
- Если запись найдена — `Optional` "разворачивается" в обычный объект `JobApplication`, который затем
  преобразуется в DTO через `toResponse(...)` и возвращается.

### Приватный метод `toResponse`

```java
    private JobApplicationResponse toResponse(JobApplication jobApplication) {
        return new JobApplicationResponse(
                jobApplication.getId(),
                jobApplication.getCompany(),
                jobApplication.getPosition(),
                jobApplication.getAppliedDate());
    }
}
```
- Приватный вспомогательный метод, который **вручную** (без сторонних библиотек-мапперов вроде
  MapStruct — это осознанное архитектурное решение проекта, см. `CLAUDE.md`) преобразует объект
  сущности `JobApplication` в DTO `JobApplicationResponse`.
- `jobApplication.getId()`, `.getCompany()`, `.getPosition()`, `.getAppliedDate()` — вызовы
  геттеров, автоматически сгенерированных Lombok-аннотацией `@Getter` в классе `JobApplication`.
- `new JobApplicationResponse(...)` — создаёт новый record-объект DTO, подставляя значения полей в
  том порядке, в котором они объявлены в `record JobApplicationResponse(Long id, String company,
  String position, LocalDate appliedDate)`.
- Этот метод используется во всех трёх публичных методах сервиса (`create`, `getAll`, `getById`),
  избегая дублирования логики преобразования.

---

## 12. `controller/JobApplicationController.java` — веб-слой (REST-контроллер)

Путь: `src/main/java/com/example/jobtracker/controller/JobApplicationController.java`

Контроллер — это "входная дверь" приложения: он принимает HTTP-запросы, делегирует всю реальную
работу сервису и формирует HTTP-ответ. Согласно архитектуре проекта, здесь **не должно быть
бизнес-логики** — только приём/валидация запроса и вызов сервиса.

```java
package com.example.jobtracker.controller;

import com.example.jobtracker.service.JobApplicationService;
import com.example.jobtracker.controller.dto.CreateJobApplicationRequest;
import com.example.jobtracker.controller.dto.JobApplicationResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
```
- Импортируются: сервис и оба DTO; `jakarta.validation.Valid` — аннотация, включающая валидацию
  входных данных; `java.net.URI` — класс для построения URI-адресов; `List` — для возврата списков;
  `ResponseEntity` — обёртка, позволяющая контролировать не только тело ответа, но и статус-код и
  заголовки HTTP-ответа; набор аннотаций Spring MVC (`@GetMapping`, `@PathVariable`, `@PostMapping`,
  `@RequestBody`, `@RequestMapping`, `@RestController`), описывающих, как обрабатывать HTTP-запросы.

```java
@RestController
@RequestMapping("/api/job-applications")
public class JobApplicationController {
```
- `@RestController` — составная аннотация, равнозначная `@Controller` + `@ResponseBody`. Она говорит
  Spring: (1) этот класс — веб-контроллер, чьи методы обрабатывают HTTP-запросы; (2) значения,
  возвращаемые методами, нужно **сериализовать напрямую в тело HTTP-ответа** (обычно в JSON, через
  Jackson), а не искать по ним имя HTML-шаблона (как было бы при обычном `@Controller`).
- `@RequestMapping("/api/job-applications")` — задаёт **общий базовый путь (префикс)** для всех
  методов этого контроллера. Все конечные точки (endpoints) этого класса будут начинаться с
  `/api/job-applications`.

```java
    private final JobApplicationService service;

    public JobApplicationController(JobApplicationService service) {
        this.service = service;
    }
```
- Аналогично сервису — здесь используется Dependency Injection через конструктор: Spring сам находит
  бин `JobApplicationService` (он был помечен `@Service`, значит Spring уже создал его объект) и
  передаёт сюда.

### Метод `create` — создание нового отклика

```java
    @PostMapping
    public ResponseEntity<JobApplicationResponse> create(@Valid @RequestBody CreateJobApplicationRequest request) {
        JobApplicationResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/job-applications/" + response.id())).body(response);
    }
```
- `@PostMapping` — говорит Spring, что этот метод обрабатывает HTTP-запросы **POST** без
  дополнительного пути (то есть ровно на `/api/job-applications`, взятый из `@RequestMapping` класса).
  POST традиционно используется для создания новых ресурсов.
- `@RequestBody CreateJobApplicationRequest request` — говорит Spring: взять тело HTTP-запроса (в
  формате JSON), десериализовать его (через Jackson) в объект `CreateJobApplicationRequest` и
  передать как параметр метода.
- `@Valid` — эта аннотация, поставленная **перед** параметром, включает автоматическую валидацию
  объекта `request` согласно правилам, описанным аннотациями `@NotBlank`/`@NotNull` внутри самого
  `CreateJobApplicationRequest`. Если валидация проваливается — метод вообще не выполнится, и клиент
  сразу получит ответ `400 Bad Request` с деталями ошибки.
- `ResponseEntity<JobApplicationResponse>` — тип возвращаемого значения: обёртка над телом ответа,
  позволяющая явно задать HTTP статус-код и заголовки, а не только тело.
- `JobApplicationResponse response = service.create(request);` — делегирует всю реальную работу
  (создание записи, сохранение в базу) сервису — контроллер сам ничего не создаёт и не сохраняет.
- `ResponseEntity.created(URI.create("/api/job-applications/" + response.id())).body(response);`
  — строит HTTP-ответ:
  - `ResponseEntity.created(uri)` — статический метод-помощник, который создаёт ответ со статусом
    **`201 Created`** (стандартный HTTP-код для "ресурс успешно создан") и добавляет заголовок
    `Location`, указывающий на адрес, по которому теперь можно получить созданный ресурс.
  - `URI.create("/api/job-applications/" + response.id())` — формирует этот адрес, подставляя id
    только что созданной записи (например, `/api/job-applications/5`).
  - `.body(response)` — устанавливает тело ответа — сам созданный объект `JobApplicationResponse`,
    который будет автоматически сериализован в JSON.

### Метод `getAll` — получение списка всех откликов

```java
    @GetMapping
    public List<JobApplicationResponse> getAll() {
        return service.getAll();
    }
```
- `@GetMapping` — обрабатывает HTTP **GET**-запросы на базовый путь `/api/job-applications` (без
  дополнительных сегментов пути).
- Метод просто вызывает `service.getAll()` и возвращает результат — список всех откликов. Так как
  класс помечен `@RestController`, Spring автоматически сериализует список DTO в JSON-массив и
  отправит его в теле ответа с кодом по умолчанию `200 OK` (явный `ResponseEntity` здесь не нужен,
  потому что нет необходимости в особом статус-коде или заголовках).

### Метод `getById` — получение одного отклика по id

```java
    @GetMapping("/{id}")
    public JobApplicationResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }
}
```
- `@GetMapping("/{id}")` — обрабатывает GET-запросы вида `/api/job-applications/{какое-то-число}`.
  `{id}` — это "переменная часть пути" (path variable) — заполнитель, который Spring извлекает из
  реального URL запроса.
- `@PathVariable Long id` — говорит Spring: взять значение из сегмента пути `{id}` (например, из
  `/api/job-applications/5` извлечь `5`), автоматически преобразовать строку в тип `Long` и передать
  как аргумент метода. Имя параметра (`id`) здесь совпадает с именем плейсхолдера в пути (`{id}`),
  поэтому дополнительных указаний не требуется.
- `return service.getById(id);` — делегирует поиск сервису. Если запись найдена — она вернётся и
  будет сериализована в JSON с кодом `200 OK`. Если не найдена — сервис выбросит
  `ResponseStatusException` со статусом `404 Not Found` (см. разбор `JobApplicationService.getById`
  выше), и Spring MVC сам превратит это исключение в корректный HTTP-ответ ещё до того, как метод
  контроллера успеет что-либо вернуть.

### Сводная таблица эндпоинтов контроллера

| HTTP-метод | Путь                          | Что делает                                | Успешный статус |
|------------|--------------------------------|--------------------------------------------|-----------------|
| POST       | `/api/job-applications`        | Создать новый отклик                       | 201 Created     |
| GET        | `/api/job-applications`        | Получить список всех откликов              | 200 OK          |
| GET        | `/api/job-applications/{id}`   | Получить один отклик по id                 | 200 OK (или 404 Not Found, если не найден) |

---

## 13. `JobTrackerApplicationTests.java` — базовый тест контекста Spring

Путь: `src/test/java/com/example/jobtracker/JobTrackerApplicationTests.java`

```java
package com.example.jobtracker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JobTrackerApplicationTests {

	@Test
	void contextLoads() {
	}

}
```
- `import org.junit.jupiter.api.Test;` — импорт аннотации `@Test` из JUnit 5 (Jupiter) — помечает
  метод как тестовый, который должен быть запущен тест-раннером.
- `import org.springframework.boot.test.context.SpringBootTest;` — импорт аннотации `@SpringBootTest`.
- `@SpringBootTest` — специальная аннотация Spring Boot для интеграционных тестов: она **поднимает
  весь Spring-контекст приложения целиком** (то есть создаёт все бины — контроллер, сервис,
  репозиторий, подключается к базе данных, применяет Flyway-миграции и т.д.), как будто приложение
  реально запускается, но без открытия настоящего сетевого порта для внешних клиентов (если явно не
  указано иное).
- `class JobTrackerApplicationTests { ... }` — обычный Java-класс, содержащий тесты (класс без
  модификатора `public` — это нормально и стандартно для тестовых классов, JUnit умеет находить и
  такие).
- `@Test void contextLoads() { }` — метод-тест с **пустым телом**. Смысл этого теста не в проверке
  какого-то конкретного результата, а в самом факте: если Spring не сможет правильно инициализировать
  все свои компоненты (например, если в конфигурации есть ошибка, отсутствует нужный бин, не удаётся
  подключиться к базе данных для применения Flyway-миграций и т.д.) — тест **упадёт с исключением**
  ещё до того, как дойдёт до пустого тела метода. Если же весь контекст поднимается без ошибок — тест
  считается пройденным. Это простой, но полезный "дымовой тест" (smoke test), гарантирующий, что
  приложение в принципе способно стартовать.

---

## 14. `Dockerfile` — сборка Docker-образа приложения

Путь: `Dockerfile`

Dockerfile описывает пошаговую инструкцию для сборки Docker-образа — самодостаточного "снимка"
приложения со всем необходимым для запуска (JRE, сам jar-файл), который можно запустить на любой
машине с установленным Docker, без ручной настройки Java/Gradle. Здесь используется **многоэтапная
сборка (multi-stage build)** — приём, при котором сборка кода происходит в одном "этапе" (образе), а
финальный образ для запуска собирается на основе другого, более лёгкого образа, куда копируется только
уже готовый результат (jar-файл), без инструментов сборки.

```dockerfile
# TODO: убрать компиляцию
FROM eclipse-temurin:21-jdk AS build
```
- Комментарий `# TODO: убрать компиляцию` — заметка от разработчика на будущее (например, идея
  впоследствии собирать jar не внутри Docker-сборки, а заранее, вне контейнера, чтобы ускорить сборку
  образа — но на момент написания этого документа это пока не сделано).
- `FROM eclipse-temurin:21-jdk AS build` — начинает **первый этап** сборки с базового образа
  `eclipse-temurin:21-jdk` (официальный образ с установленным JDK 21 — полным набором инструментов
  для компиляции и запуска Java-кода, включая компилятор `javac`). Ключевое слово `AS build` даёт
  этому этапу имя `build`, чтобы позже на него можно было сослаться.

```dockerfile
WORKDIR /app
```
- Устанавливает рабочую директорию внутри контейнера в `/app` — все последующие команды (`COPY`,
  `RUN`) будут выполняться относительно этого пути. Если папки `/app` ещё нет, Docker создаст её сам.

```dockerfile
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
```
- `COPY gradlew settings.gradle build.gradle ./` — копирует внутрь контейнера скрипт Gradle Wrapper
  (`gradlew`) и файлы конфигурации сборки (`settings.gradle`, `build.gradle`) в текущую рабочую
  директорию (`/app`).
- `COPY gradle gradle` — копирует папку `gradle` (содержащую `wrapper/gradle-wrapper.properties` и
  `gradle-wrapper.jar`) внутрь контейнера, тоже в `/app/gradle`.
- Важная деталь: на этом шаге **исходный код (`src/`) ещё не копируется**. Это сделано специально
  ради оптимизации кэширования Docker-слоёв: если исходный код меняется, а файлы зависимостей — нет,
  Docker сможет переиспользовать закэшированный слой со скачанными зависимостями (следующая команда),
  вместо того чтобы скачивать их заново при каждой пересборке.

```dockerfile
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon
```
- `chmod +x gradlew` — делает скрипт `gradlew` исполняемым (флаг `+x` — "разрешить выполнение"; это
  нужно, потому что при копировании файла в Linux-контейнер права на выполнение могли не
  сохраниться, особенно если проект разрабатывается на Windows, где нет такого понятия, как unix-права
  на исполнение).
- `&&` — означает "выполнить следующую команду только если предыдущая завершилась успешно".
- `./gradlew dependencies --no-daemon` — запускает задачу Gradle `dependencies`, которая просто
  скачивает и разрешает (resolve) все зависимости проекта, объявленные в `build.gradle`, но не
  компилирует сам код приложения. Благодаря тому, что до этого шага скопированы только файлы сборки
  (без `src`), Docker закэширует этот слой — если исходный код позже изменится, а зависимости
  останутся прежними, этот шаг (обычно самый долгий, так как требует скачивания библиотек из
  интернета) не будет выполняться заново.
- `--no-daemon` — флаг, отключающий Gradle Daemon (фоновый процесс Gradle, который обычно ускоряет
  повторные локальные запуски за счёт удержания JVM "тёплой" между вызовами). Внутри одноразового
  Docker-контейнера daemon не имеет смысла (контейнер для сборки создаётся и уничтожается один раз) —
  наоборот, он бы просто тратил лишнюю память/время на запуск и остановку.

```dockerfile
COPY src src
RUN ./gradlew bootJar --no-daemon
```
- `COPY src src` — теперь, когда зависимости уже скачаны и закэшированы, копируется сам исходный код
  проекта (`src/`) внутрь контейнера.
- `RUN ./gradlew bootJar --no-daemon` — запускает задачу Gradle `bootJar` (эта задача добавляется
  плагином `org.springframework.boot` из `build.gradle`), которая компилирует Java-код и собирает
  **исполняемый "fat jar"** — единый jar-файл, содержащий и скомпилированный код приложения, и все его
  зависимости, готовый к запуску командой `java -jar`. Результат сохраняется в `build/libs/`.

```dockerfile
FROM eclipse-temurin:21-jre
```
- Начинает **второй, финальный этап** сборки, с чистого базового образа `eclipse-temurin:21-jre` —
  это существенно более лёгкий образ, чем `21-jdk`, потому что содержит только JRE (Java Runtime
  Environment — минимум, нужный **для запуска** уже скомпилированных программ), без компилятора и
  других инструментов сборки, которые в финальном образе не нужны. Это уменьшает итоговый размер
  Docker-образа и снижает количество потенциальных уязвимостей (меньше установленного ПО — меньше
  поверхность атаки).

```dockerfile
WORKDIR /app
```
- Снова задаёт рабочую директорию `/app`, но уже в контексте нового (финального) образа — предыдущий
  этап `build` со всеми его слоями сюда не переносится, кроме того, что явно скопировано ниже.

```dockerfile
COPY --from=build /app/build/libs/*.jar app.jar
```
- Копирует файл (jar) из **предыдущего этапа сборки** (`--from=build` — ссылается на имя, заданное
  через `AS build` выше) — конкретно из пути `/app/build/libs/*.jar` (это стандартное место, куда
  Gradle кладёт собранные jar-файлы; звёздочка `*` — маска, подхватывающая единственный jar,
  сгенерированный `bootJar`) — и кладёт его в текущий (финальный) образ под именем `app.jar` в
  рабочей директории `/app`. Это ключевой момент multi-stage сборки: в финальный образ попадает
  **только готовый jar**, а все инструменты сборки (JDK, Gradle-кэш, исходники) остаются в
  промежуточном этапе и не раздувают итоговый образ.

```dockerfile
EXPOSE 8080
```
- Документирует (но не открывает автоматически "снаружи"), что приложение внутри контейнера слушает
  сетевой порт **8080** (это порт по умолчанию для встроенного Tomcat-сервера Spring Boot). Реальный
  проброс порта наружу задаётся отдельно — либо флагом `-p` у `docker run`, либо секцией `ports` в
  `docker-compose.yml` (см. ниже).

```dockerfile
ENTRYPOINT ["java", "-jar", "app.jar"]
```
- Задаёт команду, которая выполняется при **запуске** контейнера (в отличие от `RUN`, которая
  выполняется один раз во время **сборки** образа). Здесь запускается сама Java-программа:
  `java -jar app.jar` — стандартный способ запустить исполняемый jar-файл, собранный Spring Boot.
  Форма записи в виде массива строк (`["java", "-jar", "app.jar"]`, "exec form") предпочтительна перед
  строковой формой, так как позволяет процессу Java стать процессом с PID 1 внутри контейнера и
  корректно получать сигналы (например, `SIGTERM` при остановке контейнера), а не оборачиваться
  дополнительной shell-оболочкой.

---

## 15. `docker-compose.yml` — запуск всего стека (приложение + база данных)

Путь: `docker-compose.yml`

Docker Compose позволяет одной командой (`docker-compose up`) поднять **несколько связанных
контейнеров** (в данном случае — базу данных и само приложение) с нужными настройками сети, томов
(volumes) для хранения данных, и переменных окружения.

```yaml
services:
  db:
    image: postgres:16
    container_name: job-tracker-db
    restart: unless-stopped
```
- `services:` — корневой блок, где перечисляются все контейнеры ("сервисы"), которые нужно запустить.
- `db:` — имя первого сервиса — база данных. Это имя одновременно служит **сетевым именем хоста**
  внутри виртуальной сети Docker Compose — именно поэтому в переменной `SPRING_DATASOURCE_URL` сервиса
  `app` (ниже) используется адрес `jdbc:postgresql://db:5432/...`, а не `localhost`.
- `image: postgres:16` — говорит Docker использовать готовый официальный образ PostgreSQL версии 16
  (скачивается с Docker Hub, если его ещё нет локально) вместо того, чтобы собирать образ самим.
- `container_name: job-tracker-db` — задаёт человеко-читаемое фиксированное имя контейнера (иначе
  Docker Compose сгенерировал бы имя автоматически, например `job-tracker-db-1`).
- `restart: unless-stopped` — политика перезапуска: если контейнер аварийно завершится (например,
  из-за ошибки) или система перезагрузится, Docker автоматически перезапустит его — за исключением
  случая, когда контейнер был **явно остановлен вручную** пользователем (тогда Docker уважает это
  решение и не перезапускает).

```yaml
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-job_tracker}
      POSTGRES_USER: ${POSTGRES_USER:-postgres}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-postgres}
```
- `environment:` — переменные окружения, передаваемые внутрь контейнера — официальный образ
  `postgres` использует их для первичной настройки базы данных при первом запуске (создание базы,
  пользователя и пароля).
- `${POSTGRES_DB:-job_tracker}` — синтаксис подстановки переменных Docker Compose: **если** в
  окружении (или в файле `.env` рядом с `docker-compose.yml`) определена переменная `POSTGRES_DB` —
  используется её значение; **иначе** (после `:-`) используется значение по умолчанию —
  `job_tracker`. Это позволяет переопределять имя базы, логин и пароль снаружи (например, для разных
  окружений), не редактируя сам `docker-compose.yml`, а при этом всё продолжает работать "из коробки"
  и без такой настройки.
- Аналогично `POSTGRES_USER` (по умолчанию `postgres`) и `POSTGRES_PASSWORD` (по умолчанию
  `postgres`) — совпадает со значениями, захардкоженными в `application.yml` для локальной разработки
  без Docker.

```yaml
    ports:
      - "${DB_PORT:-5433}:5432"
```
- `ports:` — проброс портов из контейнера наружу, на хост-машину. Формат: `"порт_на_хосте:порт_в_контейнере"`.
- `${DB_PORT:-5433}:5432` — порт **5432 внутри контейнера** (стандартный порт PostgreSQL) пробрасывается
  на порт **`DB_PORT` хост-машины**, а если переменная `DB_PORT` не задана — по умолчанию используется
  **5433**. Именно поэтому в `application.yml` (см. выше) для подключения "снаружи" (например, при
  локальном запуске приложения не в Docker, а напрямую через `./gradlew bootRun`) используется порт
  `5433`, а не стандартный `5432` — это сделано, чтобы не конфликтовать с уже установленным на
  компьютере разработчика локальным PostgreSQL, который часто занимает порт 5432 по умолчанию.

```yaml
    volumes:
      - db-data:/var/lib/postgresql/data
```
- `volumes:` — монтирует **именованный том (named volume)** `db-data` (объявлен в самом низу файла,
  в блоке `volumes:` верхнего уровня) в путь `/var/lib/postgresql/data` внутри контейнера — именно
  туда PostgreSQL физически сохраняет файлы базы данных. Благодаря этому **данные переживают
  пересоздание контейнера**: если контейнер `db` удалить и создать заново (например, командой
  `docker-compose down && docker-compose up`), сами данные останутся сохранёнными в томе `db-data` и
  будут снова подключены к новому контейнеру. Без volume все данные терялись бы при каждом
  удалении контейнера.

```yaml
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-postgres} -d ${POSTGRES_DB:-job_tracker}"]
      interval: 5s
      timeout: 5s
      retries: 10
```
- `healthcheck:` — настраивает встроенную в Docker проверку "здоровья" (готовности) контейнера.
- `test: ["CMD-SHELL", "pg_isready -U ... -d ..."]` — команда, которую Docker периодически выполняет
  **внутри** контейнера, чтобы понять, готова ли база данных принимать подключения.
  `pg_isready` — официальная утилита PostgreSQL, которая проверяет, готов ли сервер к работе, и
  завершается с кодом 0 при успехе. Флаги `-U` (пользователь) и `-d` (имя базы) используют те же
  переменные окружения, что и выше.
- `interval: 5s` — как часто выполнять проверку (раз в 5 секунд).
- `timeout: 5s` — сколько времени ждать ответа от команды проверки, прежде чем считать эту конкретную
  попытку неудачной.
- `retries: 10` — сколько раз подряд проверка должна провалиться, прежде чем Docker пометит контейнер
  как "unhealthy" (нездоровый).
- Этот healthcheck используется сервисом `app` ниже, чтобы дождаться реальной готовности базы данных,
  а не просто факта, что контейнер запустился (запуск процесса Postgres и его готовность принимать
  подключения — не одно и то же, особенно при первом запуске, когда база инициализируется).

```yaml
  app:
    build:
      context: .
```
- `app:` — второй сервис, само приложение Job Tracker.
- `build: context: .` — вместо готового образа (`image:`, как у `db`) здесь указано, что образ нужно
  **собрать самостоятельно**, используя `Dockerfile`, найденный в текущей директории (`.`, там же, где
  лежит `docker-compose.yml`) — тот самый `Dockerfile`, разобранный в предыдущем разделе.

```yaml
    container_name: job-tracker-app
    restart: unless-stopped
```
- Аналогично сервису `db` — фиксированное имя контейнера и политика автоматического перезапуска.

```yaml
    depends_on:
      db:
        condition: service_healthy
```
- `depends_on:` — задаёт зависимость сервиса `app` от сервиса `db`: Docker Compose гарантирует
  определённый порядок запуска.
- `condition: service_healthy` — простого "запустить `db` раньше `app`" недостаточно: этот параметр
  требует, чтобы Docker дождался, пока `db` пройдёт свой `healthcheck` **успешно** (то есть база
  данных реально готова принимать подключения), и только после этого запускал контейнер `app`. Это
  предотвращает ошибки на старте приложения, когда Spring Boot/Flyway пытались бы подключиться к базе,
  которая физически ещё не готова.

```yaml
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/${POSTGRES_DB:-job_tracker}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER:-postgres}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:-postgres}
```
- Здесь настройки подключения к базе данных **переопределяют** значения, заданные "по умолчанию" в
  `application.yml`. Spring Boot автоматически подхватывает переменные окружения вида
  `SPRING_DATASOURCE_URL` и сопоставляет их со свойствами конфигурации `spring.datasource.url` (это
  называется "relaxed binding" — Spring умеет сопоставлять `SPRING_DATASOURCE_URL` с
  `spring.datasource.url` автоматически, без дополнительной настройки).
- `SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/${POSTGRES_DB:-job_tracker}` — обратите внимание:
  здесь используется хост **`db`** (имя сервиса — Docker Compose создаёт для контейнеров общую
  внутреннюю сеть, где сервисы видят друг друга по именам, как по DNS-именам) и **стандартный порт
  5432** (не 5433!) — потому что внутри Docker-сети приложение обращается к базе данных напрямую по
  внутреннему порту контейнера, а не через проброшенный наружу порт хоста.
- `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` — переопределяют логин/пароль теми же
  значениями (с теми же значениями по умолчанию), что использовались при инициализации базы в сервисе
  `db`, гарантируя, что учётные данные совпадают.

```yaml
    ports:
      - "8080:8080"
```
- Пробрасывает порт **8080 контейнера** (порт, на котором слушает встроенный сервер Tomcat, см.
  `EXPOSE 8080` в Dockerfile) на такой же порт **8080 хост-машины** — то есть после запуска
  `docker-compose up` API будет доступен по адресу `http://localhost:8080`.

```yaml
volumes:
  db-data:
```
- Объявляет **именованный том** `db-data` на верхнем уровне файла (том должен быть объявлен здесь,
  чтобы его можно было монтировать в сервисе `db` через `volumes:` выше). Docker сам управляет
  физическим расположением данных этого тома на диске хост-машины, обеспечивая их сохранность между
  перезапусками контейнеров.

---

## 16. Как всё это работает вместе — сквозной пример

Чтобы закрепить понимание, проследим полный путь **одного запроса** — создание нового отклика на
вакансию — через все слои приложения:

1. Клиент отправляет `POST /api/job-applications` с телом:
   ```json
   {"company": "Google", "position": "Backend Developer", "appliedDate": "2026-08-30"}
   ```
2. Встроенный веб-сервер (Tomcat, поднятый автоконфигурацией Spring Boot, см. `Application.java`)
   принимает соединение на порту 8080.
3. Spring MVC находит подходящий метод — `JobApplicationController.create(...)`, потому что путь и
   HTTP-метод (POST) совпадают с `@RequestMapping("/api/job-applications")` + `@PostMapping`.
4. Jackson десериализует JSON из тела запроса в объект `CreateJobApplicationRequest`.
5. Благодаря `@Valid` запускается валидация: проверяются правила `@NotBlank` и `@NotNull` — если бы,
   например, `company` было пустой строкой, клиент сразу получил бы `400 Bad Request`, и код ниже
   вообще не выполнился бы.
6. Контроллер вызывает `service.create(request)`.
7. Внутри `JobApplicationService.create(...)` создаётся новая сущность `JobApplication` (с `id = null`)
   и передаётся в `repository.save(...)`.
8. `JobApplicationRepository` (автоматически реализованный Spring Data JPA) выполняет SQL-запрос
   `INSERT INTO job_applications (...) VALUES (...)` через Hibernate и JDBC-драйвер PostgreSQL.
9. PostgreSQL сохраняет строку и генерирует новый `id` через `BIGSERIAL` (созданный в Flyway-миграции
   `V1__create_job_applications_table.sql`).
10. Hibernate заполняет сгенерированный `id` обратно в Java-объект `saved`.
11. Сервис преобразует `saved` в DTO `JobApplicationResponse` через приватный метод `toResponse(...)`.
12. Контроллер оборачивает результат в `ResponseEntity` со статусом `201 Created` и заголовком
    `Location: /api/job-applications/<новый id>`.
13. Jackson сериализует `JobApplicationResponse` обратно в JSON, и клиент получает ответ вроде:
    ```json
    {"id": 1, "company": "Google", "position": "Backend Developer", "appliedDate": "2026-08-30"}
    ```

Похожим образом работают и `GET`-запросы, только без шага валидации входных данных и без создания
новой строки в базе — только чтение (`findAll()` или `findById(id)`).

---

## 17. Краткий словарь терминов для новичка

- **REST API** — способ построения веб-сервисов, где данные ("ресурсы", например "отклики на
  вакансии") доступны по предсказуемым URL-адресам, а действия над ними выражаются через
  HTTP-методы (GET — получить, POST — создать, PUT/PATCH — изменить, DELETE — удалить).
- **DTO (Data Transfer Object)** — простой объект, который используется только для переноса данных
  между слоями приложения или по сети, без какой-либо логики внутри.
- **Entity (сущность)** — Java-класс, представляющий одну строку таблицы базы данных, с которым
  работает JPA/Hibernate.
- **JPA / Hibernate** — стандарт (JPA) и его конкретная реализация (Hibernate) для связывания
  Java-объектов с таблицами реляционной базы данных (ORM — Object-Relational Mapping), чтобы не
  писать SQL-запросы вручную для базовых операций.
- **Репозиторий (Repository)** — интерфейс/класс, отвечающий за доступ к данным (сохранение, поиск,
  удаление), скрывающий детали работы с базой от остального кода.
- **Dependency Injection (внедрение зависимостей)** — механизм, при котором объект не создаёт свои
  зависимости самостоятельно (`new ...`), а получает их "снаружи" (например, через конструктор) —
  этим управляет Spring, что делает код проще для тестирования и переиспользования.
- **Бин (Bean)** — объект, жизненным циклом которого управляет Spring-контейнер (создаёт, хранит,
  внедряет туда, где он нужен).
- **Миграция (Flyway migration)** — версионированный SQL-скрипт, описывающий одно изменение схемы
  базы данных; Flyway применяет их по порядку и запоминает, какие уже выполнены.
- **Lombok** — библиотека, генерирующая шаблонный Java-код (геттеры, сеттеры, конструкторы) во время
  компиляции по аннотациям, чтобы разработчик не писал его вручную.
- **Docker-образ (image)** — "снимок" файловой системы и настроек, из которого запускается контейнер.
- **Docker-контейнер (container)** — запущенный экземпляр образа — изолированный процесс со своей
  файловой системой, сетью и т.д.
- **Docker Compose** — инструмент для описания и одновременного запуска нескольких связанных
  контейнеров (например, приложение + база данных) одной командой.
