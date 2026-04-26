# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

PennyWise is a modern Android personal finance management application built with Kotlin and Jetpack Compose. It follows Clean Architecture principles with a clear separation between Data, Domain, and Presentation layers.

Key technical features:
- Language: Kotlin
- UI Framework: Jetpack Compose with Material 3
- Architecture: Clean Architecture (MVVM)
- Database: Room with Kotlin Coroutines and Flow
- Dependency Injection: Hilt
- Navigation: Jetpack Navigation Compose
- Authentication: AndroidX Biometric API
- Data Persistence: DataStore Preferences
- Testing: JUnit, Mockk, Espresso, Robolectric with JaCoCo coverage

## Common Development Commands

### Building and Running
```powershell
# Build the project
.\gradlew.bat build

# Install on connected device
.\gradlew.bat installDebug

# Run all tests
.\gradlew.bat test

# Generate test coverage report
.\gradlew.bat jacocoTestReport
```

### Testing
```powershell
# Run unit tests
.\gradlew.bat testDebugUnitTest

# Run instrumented tests
.\gradlew.bat connectedDebugAndroidTest

# Run specific test class
.\gradlew.bat testDebugUnitTest --tests "*CurrencyFormatterTest*"
```

## Codebase Structure

```
app/src/main/java/com/pennywise/app/
├── data/                    # Data layer
│   ├── local/              # Local data sources (Room)
│   │   ├── dao/           # Data Access Objects
│   │   ├── entity/        # Database entities
│   │   └── converter/      # Type converters
│   └── repository/         # Repository implementations
├── domain/                 # Domain layer
│   ├── model/             # Domain models (Transaction, User, BankCard, etc.)
│   ├── repository/         # Repository interfaces
│   ├── usecase/            # Business logic use cases
│   └── validation/         # Input validation and error handling
├── presentation/           # Presentation layer
│   ├── screens/            # UI screens (Home, AddExpense, Settings, etc.)
│   ├── components/         # Reusable UI components
│   ├── viewmodel/          # ViewModels with Hilt injection
│   ├── navigation/         # Navigation setup
│   ├── auth/               # Authentication management
│   ├── util/               # Utilities (CurrencyFormatter, etc.)
│   └── theme/              # Theme and styling
└── di/                     # Dependency injection modules (Hilt)
```

## Architecture Guidelines

1. **Clean Architecture Layers**:
   - Presentation Layer: UI components (Composables), ViewModels, user interactions
   - Domain Layer: Business logic, use cases, domain models, validation
   - Data Layer: Data sources (Room), repositories, database entities

2. **Single-User Approach**:
   - Database schema does NOT include userId fields
   - All data belongs to the authenticated user on that device
   - Device-level authentication (biometric/PIN lock) for security

3. **Dependency Injection**:
   - All components use constructor injection with Hilt
   - Repositories are abstracted from data sources
   - ViewModels manage UI state with StateFlow

## Key Components

- **Currency System**: Smart currency sorting based on usage frequency
- **Validation Layer**: Input validation and error handling with proper error messages
- **Recurring Expenses**: Support for subscription and recurring payments
- **Split Payments**: Support for installment-based payments with automatic tracking
- **Multi-Currency Support**: Track expenses in different currencies with smart sorting
- **Offline First**: Works completely offline with local data storage using Room

## Testing Strategy

The project includes comprehensive testing with:
- Unit tests for use cases and repositories
- UI tests for critical user flows
- Performance tests for currency operations
- Integration tests for data layer operations
- Accessibility tests for UI components

Test coverage is tracked with JaCoCo and integrated into the CI/CD pipeline.