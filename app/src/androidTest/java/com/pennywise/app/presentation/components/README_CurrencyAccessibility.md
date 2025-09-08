# Currency UI Components Accessibility Testing

This directory contains comprehensive accessibility tests for currency-related UI components in the PennyWise app.

## Overview

The accessibility tests ensure that all currency UI components comply with accessibility guidelines and provide a good experience for users with disabilities, particularly those using screen readers like TalkBack.

## Test Structure

### 1. CurrencyAccessibilityTest.kt
- **Purpose**: Compose-based accessibility tests using ComposeTestRule
- **Coverage**: 
  - Content descriptions for currency components
  - Keyboard navigation support
  - Screen reader announcements
  - Touch target size verification
  - Focus management

### 2. CurrencyEspressoAccessibilityTest.kt
- **Purpose**: Espresso-based accessibility tests with automatic accessibility checks
- **Coverage**:
  - Automatic accessibility guideline compliance
  - UI interaction accessibility
  - Color contrast verification
  - Error message accessibility

### 3. CurrencySelectionViewAccessibilityTest.kt
- **Purpose**: Tests for the custom CurrencySelectionView component
- **Coverage**:
  - Custom view accessibility features
  - Keyboard navigation implementation
  - Content description setup
  - Screen reader compatibility

### 4. CurrencyTalkBackAccessibilityTest.kt
- **Purpose**: Manual accessibility tests for TalkBack compatibility
- **Coverage**:
  - Screen reader announcement verification
  - Navigation accessibility
  - Error message announcements
  - Manual testing guidance

## Accessibility Features Tested

### Content Descriptions
- All currency components have proper content descriptions
- Currency symbols are properly announced
- Error messages are accessible

### Keyboard Navigation
- Currency selection can be navigated with keyboard
- Focus management works correctly
- Keyboard shortcuts are supported

### Screen Reader Support
- Currency information is properly announced
- Navigation is accessible to screen readers
- Error states are communicated

### Touch Targets
- All interactive elements meet minimum 48dp size requirement
- Touch targets are properly spaced

### Color Contrast
- Currency displays meet accessibility contrast requirements
- Uses Material Design colors for proper contrast

## Running the Tests

### Run All Accessibility Tests
```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pennywise.app.CurrencyAccessibilityTestSuite
```

### Run Individual Test Classes
```bash
# Compose-based tests
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pennywise.app.presentation.components.CurrencyAccessibilityTest

# Espresso accessibility tests
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pennywise.app.presentation.components.CurrencyEspressoAccessibilityTest

# Custom view tests
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pennywise.app.presentation.components.CurrencySelectionViewAccessibilityTest

# TalkBack compatibility tests
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pennywise.app.presentation.components.CurrencyTalkBackAccessibilityTest
```

## Manual Testing with TalkBack

For comprehensive accessibility testing, manual verification with TalkBack is recommended:

1. **Enable TalkBack**:
   - Go to Settings > Accessibility > TalkBack
   - Turn on TalkBack

2. **Test Currency Selection**:
   - Navigate to Settings screen
   - Find currency selection dropdown
   - Verify currency options are announced properly
   - Test selection with TalkBack gestures

3. **Test Currency Display**:
   - Navigate to Home screen
   - Verify currency amounts are announced correctly
   - Check that currency symbols are properly read

4. **Test Error Messages**:
   - Try to submit forms without required currency fields
   - Verify error messages are announced

## Accessibility Guidelines Compliance

The tests verify compliance with:

- **WCAG 2.1 AA**: Web Content Accessibility Guidelines
- **Android Accessibility Guidelines**: Google's accessibility best practices
- **Material Design Accessibility**: Material Design accessibility principles

## Key Accessibility Features Implemented

### CurrencySelectionView
- `importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES`
- Proper content descriptions
- Keyboard navigation support
- Screen reader announcements

### CurrencySelectionDropdown
- Accessible dialog implementation
- Proper button labeling
- Keyboard navigation support
- Focus management

### CurrencyChangeConfirmationDialog
- Clear dialog titles and messages
- Accessible button actions
- Proper content descriptions

## Continuous Integration

These tests are integrated into the CI/CD pipeline to ensure accessibility compliance is maintained as the codebase evolves.

## Troubleshooting

### Common Issues

1. **Tests failing on emulator**: Ensure accessibility services are enabled
2. **TalkBack not working**: Check device accessibility settings
3. **Keyboard navigation issues**: Verify focus management implementation

### Debug Tips

- Use `adb shell settings put secure enabled_accessibility_services` to enable accessibility services
- Check accessibility logs with `adb logcat | grep Accessibility`
- Use accessibility testing tools like Accessibility Scanner

## Future Enhancements

- Add automated color contrast testing
- Implement voice control testing
- Add switch control compatibility tests
- Include high contrast mode testing
