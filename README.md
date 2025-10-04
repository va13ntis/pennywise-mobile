# PennyWise - Smart Personal Finance Management

[![CI/CD Pipeline](https://github.com/your-username/pennywise-mobile/actions/workflows/ci.yml/badge.svg)](https://github.com/your-username/pennywise-mobile/actions/workflows/ci.yml)
[![PR Validation](https://github.com/your-username/pennywise-mobile/actions/workflows/pr-validation.yml/badge.svg)](https://github.com/your-username/pennywise-mobile/actions/workflows/pr-validation.yml)
[![Nightly Build](https://github.com/your-username/pennywise-mobile/actions/workflows/nightly.yml/badge.svg)](https://github.com/your-username/pennywise-mobile/actions/workflows/nightly.yml)
[![Test Coverage](https://codecov.io/gh/your-username/pennywise-mobile/branch/main/graph/badge.svg)](https://codecov.io/gh/your-username/pennywise-mobile)

PennyWise is a modern Android application built with Kotlin and Jetpack Compose that helps users manage their personal finances effectively. The app follows Clean Architecture principles and uses the latest Android development technologies.

## Features

- **Transaction Management**: Add, edit, and categorize income and expenses
- **Budget Tracking**: Set up budgets for different categories and track spending
- **Financial Reports**: View spending patterns and financial insights
- **Modern UI**: Beautiful Material 3 design with light/dark theme support
- **Local Storage**: Secure local database using Room
- **Offline First**: Works completely offline with local data storage

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: Clean Architecture (MVVM)
- **Database**: Room with Kotlin Coroutines
- **Dependency Injection**: Hilt (planned)
- **Navigation**: Navigation Compose
- **Theme**: Material 3 with dynamic colors
- **Build System**: Gradle with Kotlin DSL

## Project Structure

The project follows Clean Architecture principles with the following structure:

```
app/src/main/java/com/pennywise/app/
├── data/                    # Data layer
│   ├── local/              # Local data sources (Room)
│   │   ├── dao/           # Data Access Objects
│   │   ├── entity/        # Database entities
│   │   └── converter/     # Type converters
│   └── repository/        # Repository implementations
├── domain/                 # Domain layer
│   ├── model/             # Domain models
│   ├── repository/        # Repository interfaces
│   └── usecase/           # Business logic use cases
└── presentation/          # Presentation layer
    ├── screens/           # UI screens
    ├── components/        # Reusable UI components
    ├── viewmodel/         # ViewModels
    └── theme/             # Theme and styling
```

## Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 34
- Minimum SDK: API 24 (Android 7.0)
- JDK 17 or later

## Build Instructions

### 1. Clone the Repository

```bash
git clone <repository-url>
cd pennywise-mobile
```

### 2. Open in Android Studio

1. Open Android Studio
2. Select "Open an existing Android Studio project"
3. Navigate to the project directory and select it
4. Wait for the project to sync and index

### 3. Build and Run

1. Connect an Android device or start an emulator
2. Click the "Run" button (green play icon) in Android Studio
3. Select your target device
4. Wait for the app to build and install

### 4. Alternative: Command Line Build

```bash
# Build the project
./gradlew build

# Install on connected device
./gradlew installDebug

# Run tests
./gradlew test
```

## Configuration

### Environment Setup

The project is configured with the following default settings:

- **Application ID**: `com.pennywise.app`
- **Minimum SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)
- **Compile SDK**: API 34

### Dependencies

Key dependencies include:

- **Jetpack Compose**: 2024.02.00
- **Material 3**: Latest stable version
- **Room**: 2.6.1 with KSP
- **Navigation Compose**: 2.7.7
- **Lifecycle**: 2.7.0
- **Coroutines**: 1.7.3

## Architecture Overview

### Clean Architecture

The app follows Clean Architecture principles with three main layers:

1. **Presentation Layer**: Contains UI components, ViewModels, and user interactions
2. **Domain Layer**: Contains business logic, use cases, and domain models
3. **Data Layer**: Contains data sources, repositories, and external interfaces

PennyWise is designed as a personal finance manager where each installation manages one user's financial data. Authentication is used for device security (app lock/unlock) and future cloud sync capabilities.

### Key Components

- **Use Cases**: Implement business logic and coordinate between repositories
- **Repositories**: Abstract data sources and provide a clean API
- **ViewModels**: Manage UI state and handle user interactions
- **Composables**: UI components built with Jetpack Compose

## Development Guidelines

### Code Style

- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add comprehensive comments for complex logic
- Follow Material Design guidelines for UI components

### Testing

- Write unit tests for use cases and repositories
- Write UI tests for critical user flows
- Maintain high test coverage

### Git Workflow

- Use feature branches for new development
- Write descriptive commit messages
- Review code before merging

## CI/CD Pipeline

PennyWise uses GitHub Actions for continuous integration and deployment with comprehensive testing:

### Workflows

- **CI/CD Pipeline** (`ci.yml`): Full test suite including unit, integration, UI, performance, and accessibility tests
- **PR Validation** (`pr-validation.yml`): Quick validation for pull requests with essential tests
- **Nightly Build** (`nightly.yml`): Comprehensive nightly testing including security checks and performance benchmarks
- **Test Reporting** (`test-reporting.yml`): Generates detailed test reports and visualizations

### Test Coverage

The pipeline includes:

- **Unit Tests**: Currency models, utilities, and business logic
- **Integration Tests**: Database operations and service layer
- **UI Tests**: Espresso-based UI component testing
- **Performance Tests**: AndroidX Microbenchmark performance testing
- **Accessibility Tests**: Screen reader and accessibility compliance
- **Load Tests**: High-frequency operation testing

### Test Reports

- JaCoCo test coverage reports (HTML and XML)
- Currency-specific coverage analysis
- Performance benchmark results
- Accessibility compliance reports
- Security vulnerability scans

### Pipeline Features

- **Fast Feedback**: Unit tests run first for quick feedback
- **Parallel Execution**: Tests run in parallel for faster completion
- **Artifact Storage**: Test results and reports stored for 30 days
- **Code Coverage**: Integration with Codecov for coverage tracking
- **Failure Notifications**: Automatic notifications on test failures
- **Status Badges**: Real-time pipeline status in README

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass in the CI/CD pipeline
6. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions, please open an issue in the GitHub repository.

---

**Note**: This is a development version. Some features may be incomplete or subject to change.

