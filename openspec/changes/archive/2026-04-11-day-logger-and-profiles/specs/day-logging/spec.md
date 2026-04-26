## ADDED Requirements

### Requirement: Continuous Background Logging
The system SHALL record GPS locations continuously in the background using `FusedLocationProviderClient` with hardware batching, without requiring manual screen wake locks.

#### Scenario: Day logging in background
- **WHEN** the application starts tracking for the day and the screen turns off
- **THEN** it receives batched location updates passively every 15 seconds without being killed by the Android OS

### Requirement: Day Aggregation in History and Maps
The system SHALL aggregate all track segments recorded during a single calendar day under one History entry and draw them concurrently on a single map.

#### Scenario: Rendering a day with multiple segments
- **WHEN** the user opens the History map for a specific day
- **THEN** the map displays the track split into multiple isolated polylines where time gaps occurred, rather than a single continuous line

### Requirement: Metric SI Operations
The system SHALL perform all domain calculations and database operations in the metric system (Meters, KM, m/s).

#### Scenario: Handling unit setting toggles
- **WHEN** the user configures the display preference to Miles
- **THEN** data is still stored as KM in the database but transformed to Miles strictly on the ViewModel before rendering on the UI
