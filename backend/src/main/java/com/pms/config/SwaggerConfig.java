package com.pms.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                    SWAGGER / OPENAPI CONFIGURATION                   ║
 * ║                                                                       ║
 * ║  Swagger UI: http://localhost:8080/swagger-ui.html                    ║
 * ║  API Docs:   http://localhost:8080/api-docs                           ║
 * ║                                                                       ║
 * ║  @OpenAPIDefinition → API-level metadata and security                 ║
 * ║  @SecurityScheme → Define how authentication works in Swagger         ║
 * ║                                                                       ║
 * ║  HOW TO USE SWAGGER WITH JWT:                                         ║
 * ║  1. Open http://localhost:8080/swagger-ui.html                        ║
 * ║  2. POST /api/auth/login → Copy the token from response               ║
 * ║  3. Click "Authorize" button (🔒) at top right                        ║
 * ║  4. Enter: Bearer <paste-token-here>                                  ║
 * ║  5. Now all secured endpoints are accessible                          ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Patient Management System API",
        description = """
            ## 🏥 Patient Management System
            
            Production-ready REST API for managing patients, doctors, and appointments.
            
            ### Features:
            - **Authentication**: JWT-based login/register
            - **Patient Management**: CRUD + Search + Risk Assessment
            - **Doctor Management**: CRUD + Availability
            - **Appointments**: Book + Cancel + Conflict Detection
            - **Emergency Queue**: Priority-based patient sorting
            - **Analytics**: Dashboard with key metrics
            
            ### Authentication:
            1. Register via `POST /api/auth/register`
            2. Login via `POST /api/auth/login`  
            3. Copy the `token` from response
            4. Click **Authorize** button → Enter `Bearer <token>`
            """,
        version = "1.0.0",
        contact = @Contact(
            name = "PMS Development Team",
            email = "dev@pms.com"
        ),
        license = @License(
            name = "MIT License"
        )
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Local Development"),
        @Server(url = "https://pms-api.onrender.com", description = "Production (Render)")
    },
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",                          // Referenced by @SecurityRequirement above
    description = "JWT Bearer Token. Format: Bearer <token>",
    scheme = "bearer",                            // Bearer scheme
    type = SecuritySchemeType.HTTP,               // HTTP authentication
    bearerFormat = "JWT",                         // Token format (for display)
    in = SecuritySchemeIn.HEADER                  // Token goes in Authorization header
)
public class SwaggerConfig {
    // No methods needed - all config is via annotations
}
