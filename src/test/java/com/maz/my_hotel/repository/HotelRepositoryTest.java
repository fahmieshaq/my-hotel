package com.maz.my_hotel.repository;

import com.maz.my_hotel.entity.Hotel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

//HotelRepositoryTest starts
//        ↓
//PostgreSQL container CREATED
//        ↓
//PostgreSQL starts
//        ↓
//Flyway runs migrations
//        ↓
//Hibernate validates schema
//        ↓
//Your JPA tests run
//        ↓
//ALL tests in HotelRepositoryTest finish
//        ↓
//PostgreSQL container STOPPED
//        ↓
//Container removed / temporary PostgreSQL database gone

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@TestPropertySource(properties = {
        // Flyway owns the database schema, just like dev/production.
        "spring.flyway.enabled=true",

        // Hibernate must NOT create or modify the schema.
        // It only verifies that the JPA entities match the schema
        // created by Flyway.
        "spring.jpa.hibernate.ddl-auto=validate"
})
class HotelRepositoryTest {

    /*
     * Start a real PostgreSQL 18 database inside Docker.
     *
     * This matches the PostgreSQL major version used by Neon.
     *
     * static:
     * One PostgreSQL container is shared by all tests in this class.
     *
     * @Container:
     * Testcontainers/JUnit manages starting and stopping the container.
     *
     * @ServiceConnection:
     * Spring Boot automatically configures its DataSource using this
     * container's JDBC URL, username and password.
     */
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18"); // PostgreSQL container is shared across all
    // test methods in that test class because
    // the container field is static

    /*
     * This is a REAL Spring Data JPA repository.
     *
     * It is NOT a Mockito mock.
     *
     * HotelRepository
     *      ↓
     * Hibernate
     *      ↓
     * Spring DataSource - DataSource is not the database. It is the Java/Spring infrastructure used to obtain connections to the database.
     *      ↓
     * Testcontainers PostgreSQL
     */
    @Autowired
    private HotelRepository hotelRepository;


    /*
     * Verify that the NOT NULL constraint for Hotel.name
     * is actually enforced by our PostgreSQL schema.
     */
    @Test
    void hotel_save_nullName_fail() {

        Hotel hotel = new Hotel(null, "Morocco");

        /*
         * () -> hotelRepository.saveAndFlush(hotel)
         *
         * This is a Java LAMBDA EXPRESSION.
         *
         * General lambda syntax:
         *
         *     (parameters) -> code_to_execute
         *
         * In our case:
         *
         *     () -> hotelRepository.saveAndFlush(hotel)
         *
         *
         * What does () mean?
         * ------------------
         * () is where the lambda's parameters would normally go.
         *
         * Empty parentheses () mean:
         * "This lambda receives NO parameters."
         *
         * Examples:
         *
         *     () -> doSomething()
         *     // Takes no parameters.
         *
         *     (name) -> printName(name)
         *     // Takes one parameter called name.
         *
         *     (a, b) -> a + b
         *     // Takes two parameters called a and b.
         *
         *
         * What does -> mean?
         * ------------------
         * -> is the lambda operator.
         *
         * It separates:
         *
         *     what the lambda receives
         *              ↓
         *             ()
         *
         * from:
         *
         *     what the lambda should execute
         *              ↓
         *     hotelRepository.saveAndFlush(hotel)
         *
         * So:
         *
         *     () -> hotelRepository.saveAndFlush(hotel)
         *
         * can be read as:
         *
         *     "A piece of executable code that takes no parameters,
         *      and when executed, calls saveAndFlush(hotel)."
         *
         *
         * Why do we use the lambda here?
         * ------------------------------
         * We want AssertJ to execute saveAndFlush() itself so that
         * AssertJ can catch and inspect the exception.
         *
         * We therefore HAND the operation to assertThatThrownBy()
         * as a lambda:
         *
         *     () -> hotelRepository.saveAndFlush(hotel)
         *
         * assertThatThrownBy() then:
         *
         *     1. Executes the lambda.
         *     2. saveAndFlush(hotel) runs.
         *     3. An exception is thrown.
         *     4. AssertJ catches that exception.
         *     5. We verify its type with isInstanceOf().
         *
         * Conceptually, AssertJ is doing something similar to:
         *
         *     try {
         *         hotelRepository.saveAndFlush(hotel);
         *     } catch (Throwable exception) {
         *         // Capture the exception so we can assert against it.
         *     }
         */
        assertThatThrownBy(() ->
                hotelRepository.saveAndFlush(hotel))
                .isInstanceOf(DataIntegrityViolationException.class);
    }


    /*
     * Verify that the NOT NULL constraint for Hotel.location
     * is actually enforced by our PostgreSQL schema.
     */
    @Test
    void hotel_save_nullLocation_fail() {
        Hotel hotel = new Hotel("Grand Plaza", null);

        assertThatThrownBy(() ->
                hotelRepository.saveAndFlush(hotel))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}