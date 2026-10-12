package com.maz.my_hotel.integration;

import com.maz.my_hotel.repository.HotelRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class HotelIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18"); // make sure to the add the version that matches your actual prod postgres

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HotelRepository hotelRepository;

    @Test
    void hotels_createHotel_userSubmitsHotelInfo_success() throws Exception {
        String body = """
                {
                    "name": "Grand Plaza",
                    "location": "Moroco"
                }
                """;

        mockMvc.perform(post("/hotels")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Grand Plaza"))
                .andExpect(jsonPath("$.location").value("Moroco"));

        assertThat(hotelRepository.count()).isEqualTo(2);
    }
}


//*********************************** HERE IS THE CODE WITH COMMENTS FOR LEARNING PURPOSES *****************************
//// This test class belongs to the integration package.
//package com.maz.my_hotel.integration;
//
//
//// Currently NOT USED in this test because we manually create the JSON request
//// as a String below. This import can be removed for now.
//
//
//// We inject the REAL HotelRepository at the end of the test
//// to verify that the Hotel was actually inserted into PostgreSQL.
//import com.maz.my_hotel.repository.HotelRepository;
//
//
//// JUnit's @Test annotation marks a method as a test method.
//import org.junit.jupiter.api.Test;
//
//
//// Spring annotation used to inject objects/beans created by Spring.
//import org.springframework.beans.factory.annotation.Autowired;
//
//
//// Loads the FULL Spring Boot application context.
////
//// Unlike:
////      @WebMvcTest  -> mainly tests the web/controller layer
////      @DataJpaTest -> mainly tests the JPA/repository layer
////
//// @SpringBootTest loads the real Controller, Service, Repository,
//// Hibernate, Flyway, DataSource, etc.
//import org.springframework.boot.test.context.SpringBootTest;
//
//
//// Allows Spring Boot to automatically get the database connection
//// information from our PostgreSQL Testcontainer.
////
//// It connects:
//// PostgreSQLContainer
////      ↓
//// JDBC URL / username / password
////      ↓
//// Spring DataSource
////      ↓
//// Hibernate
//import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
//
//
//// Automatically configures MockMvc so we can make HTTP requests
//// against our Spring MVC application during the test.
//import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
//
//
//// Provides constants for HTTP content types such as application/json.
//import org.springframework.http.MediaType;
//
//
//// Allows us to override Spring properties specifically for this test.
//import org.springframework.test.context.TestPropertySource;
//
//
//// MockMvc lets us send simulated HTTP requests through Spring MVC.
////
//// IMPORTANT:
//// MockMvc does NOT mean HotelController or HotelService are mocked.
////
//// In this integration test:
////
////      MockMvc
////          ↓
////      REAL Controller
////          ↓
////      REAL Service
////          ↓
////      REAL Repository
////
//// MockMvc simply lets us exercise the HTTP/MVC layer without having
//// to manually send requests from Postman or a browser.
//import org.springframework.test.web.servlet.MockMvc;
//
//
//// @Container tells Testcontainers that the field containing
//// PostgreSQLContainer should be managed as a Docker container.
//import org.testcontainers.junit.jupiter.Container;
//
//
//// Enables Testcontainers integration with JUnit 5 for this test class.
//import org.testcontainers.junit.jupiter.Testcontainers;
//
//
//// Java class provided by Testcontainers for creating and controlling
//// a PostgreSQL Docker container.
//import org.testcontainers.postgresql.PostgreSQLContainer;
//
//
//// AssertJ assertion used at the end of the test to verify
//// something actually exists in the database.
//import static org.assertj.core.api.Assertions.assertThat;
//
//
//// Allows us to write:
////
////      post("/hotels")
////
//// instead of:
////
////      MockMvcRequestBuilders.post("/hotels")
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//
//
//// Allows us to inspect fields inside the returned JSON response.
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//
//
//// Allows us to assert HTTP status codes such as:
////
////      status().isCreated()
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//
//
///*
// * Load the FULL Spring Boot application.
// *
// * webEnvironment = RANDOM_PORT means Spring also starts the web
// * environment using a random available port rather than a fixed port
// * such as 8080.
// *
// * IMPORTANT:
// * Since this particular test is using MockMvc rather than making
// * requests to the actual network port, RANDOM_PORT is not necessary
// * for this test. @SpringBootTest without RANDOM_PORT would be enough
// * when using MockMvc.
// */
//@SpringBootTest //(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//
//
///*
// * Tell Spring Boot to automatically create/configure MockMvc
// * so we can send HTTP requests through Spring MVC.
// */
//@AutoConfigureMockMvc
//
//
///*
// * Enable Testcontainers support for this JUnit test class.
// *
// * Testcontainers will look for fields marked with @Container.
// */
//@Testcontainers
//
//
///*
// * Override these properties specifically for this integration test.
// */
//@TestPropertySource(properties = {
//
//        /*
//         * Enable Flyway.
//         *
//         * Flyway will run our REAL existing migration SQL files against
//         * the temporary PostgreSQL database inside Testcontainers.
//         *
//         * For example:
//         *
//         * src/main/resources/db/migration/V1__....sql
//         */
//        "spring.flyway.enabled=true",
//
//
//        /*
//         * Hibernate must NOT create or modify the database schema.
//         *
//         * Flyway owns the schema.
//         *
//         * Hibernate only VALIDATES that our JPA entities match
//         * the schema that Flyway created.
//         */
//        "spring.jpa.hibernate.ddl-auto=validate"
//})
//
//
//class HotelIntegrationTest {
//
//
//    /*
//     * Create a REAL PostgreSQL 18 database inside Docker.
//     *
//     * @Container:
//     * Testcontainers manages the lifecycle of this container.
//     *
//     * @ServiceConnection:
//     * Spring Boot automatically gets the JDBC URL, username and
//     * password from this PostgreSQLContainer and configures its
//     * DataSource to connect to it.
//     *
//     * static:
//     * One PostgreSQL container is shared by all test methods
//     * in this test class.
//     *
//     *
//     * Test lifecycle:
//     *
//     * HotelFlowIntegrationTest starts
//     *          ↓
//     * PostgreSQL container starts
//     *          ↓
//     * Spring DataSource connects to it
//     *          ↓
//     * Flyway runs migrations
//     *          ↓
//     * Hibernate validates schema
//     *          ↓
//     * Integration tests run
//     *          ↓
//     * All tests finish
//     *          ↓
//     * PostgreSQL container stops
//     *          ↓
//     * Temporary database is gone
//     */
//    @Container
//    @ServiceConnection
//    static PostgreSQLContainer postgres =
//            new PostgreSQLContainer("postgres:18");
//
//
//    /*
//     * Spring injects the configured MockMvc object.
//     *
//     * We use MockMvc to send our POST /hotels request through
//     * the REAL Spring MVC layer.
//     */
//    @Autowired
//    private MockMvc mockMvc;
//
//
//    /*
//     * Spring injects the REAL HotelRepository.
//     *
//     * This is NOT a Mockito mock.
//     *
//     * We use it at the end of the test to directly check PostgreSQL
//     * and confirm that the HTTP request actually resulted in a
//     * Hotel being persisted.
//     */
//    @Autowired
//    private HotelRepository hotelRepository;
//
//
//
//    /*
//     * Test the complete create-Hotel flow:
//     *
//     * POST /hotels
//     *      ↓
//     * Spring MVC
//     *      ↓
//     * REAL HotelController
//     *      ↓
//     * REAL HotelService
//     *      ↓
//     * REAL HotelRepository
//     *      ↓
//     * Hibernate
//     *      ↓
//     * Spring DataSource
//     *      ↓
//     * REAL PostgreSQL Testcontainer
//     *      ↓
//     * Hotel inserted
//     *      ↓
//     * HTTP 201 Created
//     */
//    @Test
//    void hotels_createHotel_userSubmitsHotelInfo_success() throws Exception {
//
//
//        /*
//         * Create the JSON request body that a real client would send.
//         *
//         * We intentionally use JSON here because we want to test
//         * the HTTP boundary as part of the integration test:
//         *
//         * JSON
//         *      ↓
//         * @RequestBody
//         *      ↓
//         * HotelRequestDTO
//         *      ↓
//         * @Valid
//         *      ↓
//         * HotelController
//         */
//        String body = """
//                {
//                    "name": "Grand Plaza",
//                    "location": "Morocco"
//                }
//                """;
//
//
//        /*
//         * Perform an HTTP POST request to:
//         *
//         *      POST /hotels
//         *
//         * This request travels through the REAL Spring MVC application.
//         */
//        mockMvc.perform(
//
//                        /*
//                         * Create the POST /hotels request.
//                         */
//                        post("/hotels")
//
//
//                                /*
//                                 * Tell Spring that the request body
//                                 * contains JSON.
//                                 *
//                                 * HTTP header conceptually:
//                                 *
//                                 * Content-Type: application/json
//                                 */
//                                .contentType(MediaType.APPLICATION_JSON)
//
//
//                                /*
//                                 * Put our JSON String into the HTTP
//                                 * request body.
//                                 */
//                                .content(body)
//                )
//
//
//                /*
//                 * Verify that the complete operation returned:
//                 *
//                 * HTTP 201 Created
//                 */
//                .andExpect(status().isCreated())
//
//
//                /*
//                 * Verify that the JSON response contains a numeric ID.
//                 *
//                 * We do NOT assert:
//                 *
//                 *      id == 1
//                 *
//                 * because the important contract is that PostgreSQL
//                 * generated an ID, not that the ID must specifically be 1.
//                 */
//                .andExpect(jsonPath("$.id").isNumber())
//
//
//                /*
//                 * Verify the response JSON contains:
//                 *
//                 * "name": "Grand Plaza"
//                 */
//                .andExpect(
//                        jsonPath("$.name")
//                                .value("Grand Plaza")
//                )
//
//
//                /*
//                 * Verify the response JSON contains:
//                 *
//                 * "location": "Morocco"
//                 */
//                .andExpect(
//                        jsonPath("$.location")
//                                .value("Morocco")
//                );
//
//
//        /*
//         * Finally, query the REAL database through HotelRepository.
//         *
//         * This gives us an additional guarantee that the Hotel
//         * wasn't merely returned in the HTTP response:
//         *
//         * it was actually persisted into PostgreSQL.
//         *
//         * Expected database state:
//         *
//         * hotel
//         * --------------------------------
//         * id | Grand Plaza | Morocco
//         *
//         * Therefore there should be exactly one Hotel row.
//         */
//        assertThat(hotelRepository.count()).isEqualTo(1);
//    }
//}
//
//
////        @Test
////        void hotels_createHotel_userSubmitsHotelInfo_success() {}
////
////        @Test
////        void hotels_createHotel_missingName_fail() {}
////
////        @Test
////        void hotels_createHotel_missingLocation_fail() {}
////
////        @Test
////        void hotels_getAllHotels_hotelsExist_success() {}
////
////        @Test
////        void hotels_getAllHotels_noHotelsExist_success() {}
////
////        @Test
////        void hotels_getHotelById_hotelExists_success() {}
////
////        @Test
////        void hotels_getHotelById_hotelDoesNotExist_fail() {}
////
////        @Test
////        void hotels_updateHotel_hotelExists_success() {}
////
////        @Test
////        void hotels_updateHotel_hotelDoesNotExist_fail() {}
////
////        @Test
////        void hotels_deleteHotel_hotelExists_success() {}
////
////        @Test
////        void hotels_deleteHotel_hotelDoesNotExist_fail() {}
////
////
////        I'd organize integration tests primarily by API resource/domain, and then split further by operation only when a file becomes large.
////
////        For your current project, start with:
////
////        src/test/java/com/maz/my_hotel/
////
////        ├── integration/
////        │   ├── HotelIntegrationTest.java
////│   ├── RoomIntegrationTest.java
////│   └── BookingIntegrationTest.java
////│
////        ├── controller/
////        │   └── HotelControllerTest.java
////│
////        ├── service/
////        │   └── HotelServiceTest.java
////│
////        └── repository/
////        └── HotelRepositoryTest.java
//
////        When the file gets too large
////
////        This is where I'd split by operation/use case:
////
////        integration/
////        └── hotel/
////        ├── CreateHotelIntegrationTest.java
////    ├── GetHotelIntegrationTest.java
////    ├── UpdateHotelIntegrationTest.java
////    └── DeleteHotelIntegrationTest.java
//
//
//
////        For HOORUZ, I'd definitely split further
////
////        Because your real application will have a lot of scenarios, eventually something like this scales better:
////
////        integration/
////        ├── package/
////        │   ├── CreatePackageIntegrationTest.java
////│   ├── UpdatePackageIntegrationTest.java
////│   └── PurchasePackageIntegrationTest.java
////│
////        ├── consultation/
////        │   ├── CreateConsultationSessionIntegrationTest.java
////│   ├── CancelConsultationSessionIntegrationTest.java
////│   └── CompleteConsultationSessionIntegrationTest.java
////│
////        ├── coupon/
////        │   ├── CreateCouponIntegrationTest.java
////│   └── RedeemCouponIntegrationTest.java
////│
////        └── wallet/
////        ├── CustomerWalletIntegrationTest.java
////    └── ConsultantWalletIntegrationTest.java
////
////        The important principle is:
////
////        Split integration tests according to business/API behavior, not according to Controller/Service/Repository layers.
//
////        only split it into CreateHotelIntegrationTest, GetHotelIntegrationTest, etc. once HotelIntegrationTest becomes crowded. This avoids prematurely creating lots of tiny test classes.