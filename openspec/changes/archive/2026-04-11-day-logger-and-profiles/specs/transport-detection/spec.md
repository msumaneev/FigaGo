## ADDED Requirements

### Requirement: Transport Auto-Detection
The system SHALL classify tracking points as "Transport" if the instantaneous speed consistently exceeds the profile's expected maximum speed.

#### Scenario: Switching to Transport mode
- **WHEN** the location speed exceeds the active Profile's `maxSpeed + 20%` for 5 consecutive measurements
- **THEN** the system switches the recording state to Transport and colors the new track segment green

#### Scenario: Returning to Wheelchair mode
- **WHEN** the location speed drops below or equals the active Profile's `maxSpeed + 20%` for 5 consecutive measurements
- **THEN** the system switches the recording state to Wheelchair and restores battery computation metrics

### Requirement: Independent Distance Tracking
The system SHALL NOT include distance traveled in Transport mode towards the daily wheelchair distance tally or the profile's battery consumption tracking.

#### Scenario: Excluding transport from mileage
- **WHEN** moving in Transport mode
- **THEN** the daily wheelchair mileage and battery prognosis remaining distance do not change
