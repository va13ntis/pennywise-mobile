# PennyWise - Smart Personal Finance Management

[![CI/CD Pipeline](https://github.com/your-username/pennywise-mobile/actions/workflows/ci.yml/badge.svg)](https://github.com/your-username/pennywise-mobile/actions/workflows/ci.yml)
[![PR Validation](https://github.com/your-username/pennywise-mobile/actions/workflows/pr-validation.yml/badge.svg)](https://github.com/your-username/pennywise-mobile/actions/workflows/pr-validation.yml)
[![Nightly Build](https://github.com/your-username/pennywise-mobile/actions/workflows/nightly.yml/badge.svg)](https://github.com/your-username/pennywise-mobile/actions/workflows/nightly.yml)
[![Test Coverage](https://codecov.io/gh/your-username/pennywise-mobile/branch/main/graph/badge.svg)](https://codecov.io/gh/your-username/pennywise-mobile)

PennyWise is a modern Android application built with Kotlin and Jetpack Compose that helps users manage their personal finances effectively. The app follows Clean Architecture principles and uses the latest Android development technologies.

## Features

### Core Functionality
- **Expense Tracking**: Add, categorize, and track your expenses with detailed information
- **Recurring Expenses**: Mark and track subscription and recurring payments separately
- **Split Payments**: Support for installment-based payments with automatic tracking
- **Monthly Overview**: View current month's summary with weekly breakdowns
- **Multi-Currency Support**: Track expenses in different currencies with smart currency sorting
- **Payment Methods**: Support for Cash, Credit Card, and Cheque payments
- **Bank Card Management**: Add and manage multiple bank cards for expense tracking

### User Experience
- **Device Security**: Biometric authentication (fingerprint/face) or PIN-based app lock
- **First-Run Setup**: Guided setup process for new users
- **Month Navigation**: Easily navigate between months to view historical data
- **Collapsible Sections**: Weekly expenses and recurring expenses are collapsible for better organization
- **Modern UI**: Beautiful Material 3 design with light/dark theme support
- **Offline First**: Works completely offline with local data storage using Room

### Technical Features
- **Clean Architecture**: Proper separation of concerns with Data, Domain, and Presentation layers
- **Localization**: Partial multi-language support (Russian currently implemented)
- **Type-Safe Navigation**: Compose Navigation with proper state management
- **Secure Storage**: Local SQLite database with Room persistence
- **Dependency Injection**: Hilt for clean and testable code

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: Clean Architecture (MVVM)
- **Database**: Room with Kotlin Coroutines and Flow
- **Dependency Injection**: Hilt
- **Navigation**: Jetpack Navigation Compose
- **Authentication**: AndroidX Biometric API
- **Data Persistence**: DataStore Preferences
- **Theme**: Material 3 with dynamic colors and dark mode
- **Build System**: Gradle with Kotlin DSL and KSP
- **Testing**: JUnit, Mockk, Espresso, Robolectric
- **Code Coverage**: JaCoCo

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
│   ├── model/             # Domain models (Transaction, User, BankCard, etc.)
│   ├── repository/        # Repository interfaces
│   ├── usecase/           # Business logic use cases
│   └── validation/        # Input validation and error handling
├── presentation/          # Presentation layer
│   ├── screens/           # UI screens (Home, AddExpense, Settings, etc.)
│   ├── components/        # Reusable UI components
│   ├── viewmodel/         # ViewModels with Hilt injection
│   ├── navigation/        # Navigation setup
│   ├── auth/              # Authentication management
│   ├── util/              # Utilities (CurrencyFormatter, etc.)
│   └── theme/             # Theme and styling
└── di/                     # Dependency injection modules (Hilt)
```

## Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 34
- Minimum SDK: API 26 (Android 8.0)
- Target SDK: API 34 (Android 14)
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
- **Minimum SDK**: API 26 (Android 8.0)
- **Target SDK**: API 34 (Android 14)
- **Compile SDK**: API 34
- **Kotlin**: 1.9.22
- **Compose Compiler**: 1.5.8

### Key Dependencies

- **Jetpack Compose BOM**: 2024.02.00
- **Material 3**: Latest from BOM
- **Room**: 2.6.1 with KSP
- **Navigation Compose**: 2.7.7
- **Hilt**: 2.48
- **Lifecycle**: 2.7.0
- **Coroutines**: 1.7.3
- **DataStore**: 1.0.0
- **Biometric**: 1.2.0-alpha05
- **Retrofit**: 2.9.0 (for future cloud sync)

## Architecture Overview

### Clean Architecture

The app follows Clean Architecture principles with three main layers:

1. **Presentation Layer**: Contains UI components (Composables), ViewModels, and user interactions
2. **Domain Layer**: Contains business logic, use cases, domain models, and validation
3. **Data Layer**: Contains data sources (Room), repositories, and database entities

### Single-User Approach

PennyWise is designed as a **personal finance manager** where each installation manages one user's financial data:

- **No Multi-Tenancy**: Database schema does NOT include userId fields or user-based data filtering
- **Single User Per Device**: All data belongs to the authenticated user on that device
- **Authentication Purpose**: Device security (biometric/PIN lock) and future cloud sync capabilities
- **Data Isolation**: Achieved at the device level, not database level

### Key Components

- **Hilt Dependency Injection**: All components use constructor injection with Hilt
- **Use Cases**: Implement business logic and coordinate between repositories
- **Repositories**: Abstract data sources and provide clean APIs with authentication validation
- **ViewModels**: Manage UI state with StateFlow and handle user interactions
- **Composables**: Modern UI components built with Jetpack Compose and Material 3
- **Currency System**: Smart currency sorting based on usage frequency
- **Validation Layer**: Input validation and error handling with proper error messages

## Implemented Screens

### 1. First-Run Setup Screen
- Initial app setup flow for new users
- Currency selection with smart sorting
- Locale/language selection
- Device authentication setup (biometric or PIN)

### 2. Home Screen
- Monthly expense summary with total amount
- Previous/Next month navigation
- Recurring expenses section (collapsible)
- Weekly expense breakdown (collapsible)
- Split payment installments display
- Floating action button to add new expenses
- Settings navigation

### 3. Add Expense Screen
- Date picker with calendar dialog
- Merchant/vendor name input
- Amount input with currency display
- Category selection
- Payment method selector (Cash, Credit Card, Cheque)
- Bank card selection (for card payments)
- Split payment configuration (installments)
- Recurring expense toggle
- Notes field
- Form validation with error messages

### 4. Settings Screen
- User preferences management
- Currency settings
- Language settings
- Authentication settings

### 5. Bank Cards Screen (Work in Progress)
- Manage bank cards
- Add/edit/delete cards
- Card-based expense filtering

## User Flow

1. **First Launch**: User sets up currency, locale, and device authentication
2. **Authentication**: User unlocks app with biometric or PIN (if enabled)
3. **Home Dashboard**: View current month's expenses organized by week
4. **Add Expense**: Quick expense entry with all necessary details
5. **Review**: View historical data by navigating between months

## Data Models

### Transaction
- Amount and currency
- Description and category
- Transaction type (Income/Expense)
- Date and timestamps
- Payment method (Cash, Credit Card, Cheque)
- Recurring configuration (daily, weekly, monthly, yearly)
- Split payment support (installments)
- Optional notes

### User
- Username and authentication
- Preferred currency
- Locale settings
- Device authentication preferences

### Bank Card
- Card name and last 4 digits
- Associated bank
- Card type
- Active status

### Payment Method Configuration
- Default payment method
- Custom payment method settings

### Split Payment Installment
- Linked to parent transaction
- Installment number and total count
- Individual installment amount
- Payment tracking

## Development Guidelines

### Code Style

- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add comprehensive comments for complex logic
- Follow Material Design guidelines for UI components

### Testing

- Write unit tests for use cases and repositories
- Write UI tests for critical user flows
- Use Mockk for mocking in Kotlin tests
- Use Espresso for UI testing
- Maintain high test coverage with JaCoCo

### Git Workflow

- Use feature branches for new development
- Write descriptive commit messages
- Review code before merging

## Roadmap

### Current Status (v1.0)
✅ Core expense tracking functionality  
✅ Multi-currency support  
✅ Recurring expenses  
✅ Split payments  
✅ Device authentication  
✅ Monthly and weekly views  
✅ Material 3 UI  
✅ Offline-first architecture  

### Planned Features
- 🔲 Complete multi-language support (Hebrew with RTL, more languages)
- 🔲 Budget tracking and alerts
- 🔲 Financial reports and charts
- 🔲 Data export (CSV, PDF)
- 🔲 Category customization
- 🔲 Search and filtering
- 🔲 Widgets for home screen
- 🔲 Income tracking
- 🔲 Balance calculations

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

