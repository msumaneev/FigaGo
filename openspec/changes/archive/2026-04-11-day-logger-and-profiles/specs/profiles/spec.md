## ADDED Requirements

### Requirement: Profile Creation and Types
The system SHALL allow the user to create Profiles for different wheelchairs, specifying the type as Electric or Manual.

#### Scenario: Creating an electric wheelchair profile
- **WHEN** the user creates a profile and selects Electric
- **THEN** the system requires specifying Max Mileage and LED count

#### Scenario: Creating a manual wheelchair profile
- **WHEN** the user creates a profile and selects Manual
- **THEN** the system hides battery-related inputs and only asks for Max Speed

### Requirement: Profile Switching
The system SHALL allow the user to switch the active profile from the start screen carousel.

#### Scenario: Switching active profile
- **WHEN** the user clicks on a profile tile in the top carousel
- **THEN** it becomes the active profile and updates the tracked metrics and limits on the dashboard

### Requirement: Battery Level Customization
The system SHALL allow users to manually override the calculated mileage expectation for each LED light level in an Electric profile.

#### Scenario: Adjusting LED mileage
- **WHEN** adjusting a "Passport" entry for an electric profile
- **THEN** the user can use a wheel picker to manually override the default `maxMileage/ledCount` distance for any specific LED
