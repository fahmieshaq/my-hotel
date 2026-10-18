// Run all tests including the spring boot application test class
PS C:\Users\fjamc\OneDrive\Desktop\my-hotel> .\mvnw test  ---> Tries to run MyHotelApplicationTests too and leads it to failure due to some missing configs

// Run multiple tests
PS C:\Users\fjamc\OneDrive\Desktop\my-hotel> .\mvnw "-Dtest=HotelControllerTest,HotelServiceTest" test

// Test service
PS C:\Users\fjamc\OneDrive\Desktop\my-hotel> .\mvnw -Dtest=HotelServiceTest test

// Test controller
PS C:\Users\fjamc\OneDrive\Desktop\my-hotel> .\mvnw -Dtest=HotelControllerTest test
PS C:\Users\fjamc\OneDrive\Desktop\my-hotel> .\mvnw -Dtest=HotelControllerTest#hotels_updateHotel_doNotPassAnyHotelId_fail test

// Test repo
PS C:\Users\fjamc\OneDrive\Desktop\my-hotel> .\mvnw -Dtest=HotelRepositoryTest test

If you change flyway by deleting a version file, the test may still read migration files from cahce, so do

PS C:\Users\fjamc\OneDrive\Desktop\my-hotel> .\mvnw clean -Dtest=HotelRepositoryTest test

~~~~~~~~~~~~~~~~~~~~~~~~

// ABOUT CLEANING UP STALE MIGRATION FIELS
Your database is still in the “location is nullable” state, so saveAndFlush() no longer throws.
 That’s why AssertJ says “Expecting code to raise a throwable” at hotel_save_nullLocation_fail.
Two things usually cause this after deleting V2:
1.
Flyway doesn’t rollback applied migrations just because the file was deleted.
2.
Stale migration file on classpath (very common): deleted V2 can still exist under target\classes\db\migration, so Flyway still applies it.

To fix this, you can:
1. Clean the database manually or with Flyway clean.
2. Delete the stale migration files under target\classes\db\migration.
3. Re-run the tests with mvnw clean.

RULE:

If V2 was ever applied to prod DB and then you deploy an app version where V2 file is deleted, Flyway usually fails at startup with a migration validation error (applied migration missing locally). So deleting migration files after they’ve been applied is unsafe for prod.
Best practice: never delete applied migrations; add a new migration (V3) to revert/adjust schema instead.

~~~~~~~~~~~~~~~~~~~~~~~~~~~~


*** Integration tests:

These test several real layers together:

HTTP request
     ↓
REAL Controller
     ↓
REAL Service
     ↓
REAL Repository
     ↓
REAL Hibernate
     ↓
REAL PostgreSQL Testcontainer

For example:

POST /hotels
{
    "name": "Grand Plaza",
    "location": "Morocco"
}
        ↓
HotelController
        ↓
HotelService
        ↓
HotelRepository
        ↓
PostgreSQL
        ↓
201 Created
{
    "id": 1,
    "name": "Grand Plaza",
    "location": "Morocco"
}