You are a senior Android developer. I want you to scaffold a native Android app in Kotlin using Jetpack Compose. The app is called "PennyWise" — a personal expense and budget manager.

The app should work fully offline with a local SQLite database (using Room). Cloud backups (Google Drive or OneDrive) will be added later, so structure the code cleanly with good architecture.

App Requirements:

1. Use Jetpack Compose for UI
2. Use Room for local data storage
3. Follow Clean Architecture (recommended) or MVVM
4. Use Material 3 components and dark/light theme switching
5. Add localization support for English, Hebrew (RTL), and Russian
6. Include the following screens:
   - Login (basic local auth)
   - Register
   - Home (current month summary view with collapsed weeks, recurring expenses pinned on top)
   - New Expense Form (fields listed below)
   - Settings (for future cloud backup)

Data Model for Expenses:
- id: Int
- date: LocalDate (parsed from either dd/MM/yyyy or ISO format)
- merchant: String (בית העסק)
- amount: Double (סכום החיוב)
- isRecurring: Boolean (derived from תשלום = "קבוע")
- notes: String? (פרטים)

**ARCHITECTURAL DECISION - SINGLE-USER APPROACH:**
This is a personal finance app with ONE user per installation. The database schema does NOT use multi-user/multi-tenant design. There are NO userId fields in any entities, NO foreign key relationships to a users table, and NO user-based data filtering in queries. Authentication is used solely for security, backup, and sync purposes - NOT for data isolation. All data in the app belongs to the single authenticated user on that device.

UI Form (New Expense):
- Date picker
- Text field for merchant name
- Numeric input for amount
- Payment type selector (recurring or one-time)
- Optional notes field
- Save button

Other Features:
- App starts on current month’s overview
- Expenses grouped by week
- Subscriptions (recurring) pinned at top, collapsed view by default
- Simple Room DAO with methods for filtering by month, userId, and recurrence
- Multilingual support using resource strings (with RTL layout for Hebrew)
- README with instructions for building in Android Studio and using Room

Output:
- Full Android project scaffold with one working screen (Home or NewExpense)
- Folder structure: ui, data, model, viewmodel, repository
- build.gradle.kts for Material 3, Room, Compose, etc.

This is a native Kotlin-only Android app with offline-first storage and designed for future sync and export.
