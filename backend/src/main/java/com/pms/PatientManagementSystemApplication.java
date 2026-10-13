package com.pms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║              PATIENT MANAGEMENT SYSTEM - ENTRY POINT            ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * @SpringBootApplication is a COMPOSED annotation that combines:
 *
 * 1. @Configuration
 *    - Marks this class as a source of bean definitions
 *    - Spring reads @Bean methods from this class
 *
 * 2. @EnableAutoConfiguration
 *    - The MAGIC of Spring Boot!
 *    - Looks at your classpath (what JARs you have)
 *    - Automatically configures beans you likely need
 *    - Example: If mysql-connector is on classpath → auto-configure DataSource
 *    - Example: If spring-security is on classpath → auto-configure security
 *
 * 3. @ComponentScan
 *    - Scans package "com.pms" and sub-packages
 *    - Finds @Controller, @Service, @Repository, @Component classes
 *    - Registers them as Spring beans in the ApplicationContext
 *
 * @EnableJpaAuditing
 *    - Enables automatic population of @CreatedDate and @LastModifiedDate
 *    - Works with BaseEntity's audit fields
 *    - Spring injects current timestamp when entity is saved/updated
 */
@SpringBootApplication
@EnableJpaAuditing  // Auto-populate createdAt, updatedAt fields
public class PatientManagementSystemApplication {

    /**
     * APPLICATION STARTUP FLOW:
     *
     * 1. JVM starts main()
     * 2. SpringApplication.run() bootstraps Spring
     * 3. Creates ApplicationContext (IoC Container)
     * 4. Runs auto-configuration
     * 5. Scans and registers all @Component beans
     * 6. Establishes database connection
     * 7. Runs Hibernate DDL (create/update tables)
     * 8. Starts embedded Tomcat server on port 8080
     * 9. Application ready to handle HTTP requests!
     *
     * IoC Container = "Inversion of Control"
     * YOU don't create objects → SPRING creates and manages them
     * This is called Dependency Injection (DI)
     */
    public static void main(String[] args) {
        SpringApplication.run(PatientManagementSystemApplication.class, args);
        System.out.println("""
                ╔══════════════════════════════════════════════════╗
                ║     Patient Management System Started! 🏥        ║
                ║  API:     http://localhost:8080/api              ║
                ║  Swagger: http://localhost:8080/swagger-ui.html  ║
                ╚══════════════════════════════════════════════════╝
                """);
    }
}
