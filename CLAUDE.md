# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.5.6 order service application using Java 21, designed for realtime order processing. The application integrates with MySQL database, Redis for caching, Kafka for messaging, and includes OAuth2 security.

## Build System & Commands

**Build Tool**: Gradle with Wrapper
- **Build**: `./gradlew build`
- **Run**: `./gradlew bootRun`
- **Test**: `./gradlew test`
- **Clean**: `./gradlew clean`

## Technology Stack

**Core Framework**: Spring Boot 3.5.6 with Java 21
**Dependencies**:
- Spring Web (REST APIs)
- Spring Data JPA (Database ORM)
- Spring Data Redis (Caching)
- Spring Kafka (Message streaming)
- Spring Security + OAuth2 Client
- MySQL Connector
- Lombok (Code generation)
- JUnit 5 (Testing)

## Database Configuration

**Primary Database**: MySQL
- Database: `order_service`
- Default connection: `localhost:3306`
- JPA configured with `hibernate.ddl-auto=update`
- SQL logging enabled in development

**Caching**: Redis integration configured via Spring Data Redis

## Package Structure

**Base Package**: `com.example.order_service`
- Note: Original package name 'com.example.order-service' was invalid due to hyphen

**Current Structure**: Minimal Spring Boot application with only main application class

## Security

OAuth2 Client authentication configured with Spring Security integration.

## Development Notes

- Application uses Spring Boot's auto-configuration
- MySQL dialect: `org.hibernate.dialect.MySQL8Dialect`
- Server timezone: UTC with SSL disabled for local development
- Lombok annotations available for reducing boilerplate code