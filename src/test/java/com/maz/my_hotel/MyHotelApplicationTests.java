package com.maz.my_hotel;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


/*
 * PURPOSE OF THIS TEST
 * ====================
 *
 * This is a very basic Spring Boot integration/smoke test.
 *
 * It answers one main question:
 *
 *      "Can my entire Spring Boot application context start successfully?"
 *
 *
 * Even though contextLoads() is empty, most of the work happens BEFORE
 * the test method executes because of @SpringBootTest.
 *
 * Spring attempts to start the full application context:
 *
 *      MyHotelApplicationTests starts
 *              ↓
 *      @SpringBootTest
 *              ↓
 *      Spring Boot application context starts
 *              ↓
 *      Configuration/properties loaded
 *              ↓
 *      Controllers created
 *              ↓
 *      Services created
 *              ↓
 *      Repositories created
 *              ↓
 *      DataSource configured
 *              ↓
 *      Flyway initialized
 *              ↓
 *      Hibernate / JPA initialized
 *              ↓
 *      Spring beans wired together
 *              ↓
 *      contextLoads() executes
 *              ↓
 *      TEST PASSES
 *
 *
 * If Spring cannot create the application context, the test fails
 * before the empty contextLoads() method becomes meaningful.
 *
 * For example:
 *
 *      Invalid Spring configuration
 *      Missing required bean
 *      Database connection failure
 *      Flyway initialization failure
 *      Hibernate/JPA initialization failure
 *
 * can all cause this test to fail.
 *
 *
 * WHY THIS TEST PREVIOUSLY FAILED
 * ===============================
 *
 * Unlike our Mockito service tests, @SpringBootTest attempts to start
 * the entire Spring application context.
 *
 * Our previous test configuration used the dev profile, which caused
 * Spring to initialize Flyway/Hikari and attempt to connect to the
 * configured development database.
 *
 * Therefore, the failure was not caused by the empty contextLoads()
 * method itself. The application context failed while Spring was
 * starting.
 *
 *
 * DO WE STILL NEED THIS TEST?
 * ===========================
 *
 * Not necessarily.
 *
 * We now have meaningful integration tests such as:
 *
 *      @SpringBootTest
 *      class HotelFlowIntegrationTest
 *
 * That test also requires the COMPLETE Spring application context
 * to start successfully before its test methods can execute.
 *
 * Therefore:
 *
 *      MyHotelApplicationTests
 *
 *          Can Spring application start?
 *                  ↓
 *                 YES
 *                  ↓
 *                 PASS
 *
 *
 * Whereas our HotelFlowIntegrationTest does:
 *
 *      Can Spring application start?
 *                  ↓
 *      Can Testcontainers PostgreSQL start?
 *                  ↓
 *      Can Flyway run the migrations?
 *                  ↓
 *      Can Hibernate validate the schema?
 *                  ↓
 *      Can POST /hotels reach the real Controller?
 *                  ↓
 *      Can the Controller call the real Service?
 *                  ↓
 *      Can the Service call the real Repository?
 *                  ↓
 *      Can Hibernate persist the Hotel to PostgreSQL?
 *                  ↓
 *      Does the API return HTTP 201?
 *                  ↓
 *      Is the response JSON correct?
 *                  ↓
 *                 PASS
 *
 *
 * So HotelFlowIntegrationTest already proves that the Spring context
 * can load AND proves that an actual application flow works.
 *
 * Because of that, once meaningful @SpringBootTest integration tests
 * exist, this standalone contextLoads() test provides little additional
 * value and can be removed.
 *
 * It can still be kept as a very small application-startup smoke test,
 * but it is not required for our testing strategy.
 */
//************** CODE STARTS HERE **************
//@SpringBootTest
//class MyHotelApplicationTests {
//
//	/*
//	 * This method intentionally contains no code.
//	 *
//	 * If execution reaches this method, Spring has already successfully
//	 * created the application context.
//	 */
//	@Test
//	void contextLoads() {
//	}
//
//}
//************** CODE ENDS HERE **************