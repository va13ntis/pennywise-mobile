# Material 3 Design Specifications - Add Expense Screen

## Color Palette

### Surface Colors
```kotlin
// Top App Bar
containerColor = MaterialTheme.colorScheme.surfaceContainerHigh

// Section Cards
containerColor = MaterialTheme.colorScheme.surfaceContainerLow
elevation = 3.dp

// Text Fields (Focused)
containerColor = MaterialTheme.colorScheme.surfaceContainerHighest

// Text Fields (Unfocused)
containerColor = MaterialTheme.colorScheme.surfaceContainerHigh

// Highlighted Info (Monthly Payment)
containerColor = MaterialTheme.colorScheme.primaryContainer
textColor = MaterialTheme.colorScheme.onPrimaryContainer
```

### Interactive Elements
```kotlin
// Primary Actions (Save Button, Selected Chips)
backgroundColor = MaterialTheme.colorScheme.primary
textColor = MaterialTheme.colorScheme.onPrimary

// Secondary Actions (Cancel Button)
borderColor = MaterialTheme.colorScheme.outline
textColor = MaterialTheme.colorScheme.onSurface

// Error States
backgroundColor = MaterialTheme.colorScheme.errorContainer
textColor = MaterialTheme.colorScheme.error
iconColor = MaterialTheme.colorScheme.error
```

## Typography Scale

```kotlin
// Section Titles (Card Headers)
style = MaterialTheme.typography.titleMedium

// Field Labels
style = MaterialTheme.typography.labelMedium

// Input Text & Body
style = MaterialTheme.typography.bodyMedium

// Supporting Text & Hints
style = MaterialTheme.typography.bodySmall

// Highlighted Amount (Monthly Payment)
style = MaterialTheme.typography.headlineSmall

// Button Text
style = MaterialTheme.typography.labelLarge
```

## Spacing System

### Card Layout
```
Horizontal Padding: 20dp (screen edges)
Vertical Padding: 8dp (top/bottom)
Card Spacing: 12dp (between cards)
Card Internal Padding: 16dp
```

### Field Spacing
```
Field Group Spacing: 12dp (within same card)
Related Elements: 8dp (chips, buttons)
Section Header to Content: 12dp
Icon to Text: 8dp (in headers), 12dp (in fields)
```

### Component Sizes
```
Text Field Height: 56dp (default Material 3)
Button Height: 56dp
Icon Button Size: 36dp (installment controls)
Currency Button: 56dp (matches text field)
Icon Size (Headers): 20dp
Icon Size (Fields): 24dp
```

## Shape System

```kotlin
// Cards
shape = RoundedCornerShape(16.dp)

// Text Fields & Buttons
shape = RoundedCornerShape(12.dp)

// Bank Card Selection Items
shape = RoundedCornerShape(8.dp)
```

## Component Specifications

### 1. SectionCard
```kotlin
Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    shape = RoundedCornerShape(16.dp)
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Optional header with icon + title
        // Content
    }
}
```

### 2. FilledTextFieldWithIcon
```kotlin
TextField(
    // Standard parameters
    leadingIcon = { Icon(...) },
    shape = RoundedCornerShape(12.dp),
    colors = TextFieldDefaults.colors(
        focusedContainerColor = surfaceContainerHighest,
        unfocusedContainerColor = surfaceContainerHigh,
        errorContainerColor = errorContainer.copy(alpha = 0.3f),
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        errorIndicatorColor = Color.Transparent
    )
)
```

### 3. FilterChip Usage
```kotlin
FilterChip(
    selected = isSelected,
    onClick = { /* action */ },
    label = { Text("Label") },
    modifier = Modifier.weight(1f) // For equal width in rows
)
```

## Screen Sections Layout

### 1. Top App Bar
```
Height: 64dp (Material 3 default)
Background: surfaceContainerHigh
Elevation: 0dp (flat)
Title: "New Expense" (titleLarge)
Navigation: Back arrow (IconButton)
```

### 2. Date Selection Card
```
Layout: Row with Icon + Column
Icon: CalendarToday (24dp, primary color)
Label: "Select Date" (labelMedium, onSurfaceVariant)
Value: Formatted date (titleMedium, onSurface)
Interaction: Tap entire card to open DatePicker
```

### 3. Transaction Details Card
```
Header: "Transaction Details" + Store icon
Content: 
  - Merchant field (FilledTextFieldWithIcon)
  - Amount field + Currency button (Row layout)
    - Amount field weight(1f)
    - Currency button size(56.dp)
```

### 4. Category Selection Card
```
Header: "Category" + Category icon
Content: ExposedDropdownMenuBox
  - TextField with dropdown arrow
  - Menu items: Food, Transport, Shopping, etc.
```

### 5. Payment Details Card
```
Header: "Payment Details" + Payment icon
Content: 3 sections (vertical spacing 16dp)
  1. Payment Type
     - Label (labelMedium)
     - FilterChips: One-time | Recurring
  2. Payment Method
     - Label (labelMedium)
     - FilterChips: Cash | Credit Card | Cheque
  3. Recurring Period (AnimatedVisibility)
     - Label (labelMedium)
     - FilterChips: Daily | Weekly | Monthly | Yearly
```

### 6. Bank Card Selection Card
```
Header: "Select Bank Card" + CreditCard icon
Content: Radio button list
  - Each item: RadioButton + Card details
  - Selected item: primaryContainer background (alpha 0.3)
  - Corner radius: 8dp
```

### 7. Split Payment Card
```
Header: "Payments Layout" + Repeat icon
Content:
  1. Installment Counter
     - Label (labelMedium)
     - Controls: [-] Button | Count | [+] Button
  2. Monthly Payment Display (if installments > 1)
     - Surface with primaryContainer background
     - Label: "Monthly Payment"
     - Amount: headlineSmall
     - Months: bodySmall
```

### 8. Notes Card
```
Header: "Notes" + Notes icon
Content: FilledTextFieldWithIcon
  - Multi-line (minLines: 3, maxLines: 5)
  - Placeholder: "Optional additional info..."
```

### 9. Bottom Bar
```
Surface with shadowElevation: 8dp
Padding: 16dp
Layout: Row with 2 buttons (equal width)
  - Cancel (OutlinedButton, weight 1f)
  - Save (Button, weight 1f)
Height: 56dp each
Gap: 12dp
```

## Animation Specifications

### Expand/Collapse Animations
```kotlin
AnimatedVisibility(
    visible = condition,
    enter = expandVertically() + fadeIn(),
    exit = shrinkVertically() + fadeOut()
)
```

### Used For:
- Recurring period options
- Bank card selection
- Installment options
- Error messages

## State Management

### Form States
```kotlin
// Input States
var merchant by remember { mutableStateOf("") }
var amount by remember { mutableStateOf("") }
var category by remember { mutableStateOf("") }
var notes by remember { mutableStateOf("") }

// Selection States
var isRecurring by remember { mutableStateOf(false) }
var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
var selectedRecurringPeriod by remember { mutableStateOf(RecurringPeriod.MONTHLY) }
var selectedDate by remember { mutableStateOf(Date()) }

// Dialog States
var showDatePicker by remember { mutableStateOf(false) }
var showInstallmentOptions by remember { mutableStateOf(false) }
var currencyExpanded by remember { mutableStateOf(false) }
var categoryExpanded by remember { mutableStateOf(false) }

// Validation States
var merchantError by remember { mutableStateOf<String?>(null) }
var amountError by remember { mutableStateOf<String?>(null) }
var categoryError by remember { mutableStateOf<String?>(null) }
var isFormValid by remember { mutableStateOf(false) }
```

### ViewModel States
```kotlin
val uiState by viewModel.uiState.collectAsState()
val selectedCurrency by viewModel.selectedCurrency.collectAsState()
val bankCards by viewModel.bankCards.collectAsState()
val defaultPaymentMethod by viewModel.defaultPaymentMethod.collectAsState()
```

## Accessibility Features

### Content Descriptions
```kotlin
// Icons
Icon(
    Icons.Default.ArrowBack,
    contentDescription = "Back"
)

// Interactive elements
IconButton(
    onClick = { /* decrease */ },
    contentDescription = stringResource(R.string.content_desc_decrease_installments)
)
```

### Semantic Properties
- All interactive elements have proper content descriptions
- Error messages announced by screen readers
- Focus management for keyboard navigation
- Proper labeling for form fields

## Dark Mode Support

All colors use Material Theme dynamic tokens:
```kotlin
// Automatically adapts to dark mode
MaterialTheme.colorScheme.surface
MaterialTheme.colorScheme.onSurface
MaterialTheme.colorScheme.primary
MaterialTheme.colorScheme.primaryContainer
// etc.
```

## Preview Configuration

```kotlin
@Preview(
    name = "Add Expense Screen - Light",
    showBackground = true,
    showSystemUi = true
)

@Preview(
    name = "Add Expense Screen - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    showSystemUi = true
)
```

## Responsive Design

- Cards expand to fill width with consistent horizontal margins
- FilterChips use weight(1f) for equal distribution
- Text fields respond to available space
- Bottom bar stays fixed at bottom
- Content scrolls with proper padding to avoid button overlap

