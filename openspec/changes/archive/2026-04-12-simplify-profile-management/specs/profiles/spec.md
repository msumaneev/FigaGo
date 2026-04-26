## MODIFIED Requirements

### Requirement: Profile Creation and Types
The system SHALL allow the user to create Profiles for different wheelchairs automatically with auto-incrementing default names.

#### Scenario: Creating a new abstract profile
- **WHEN** the user clicks the "Add (plus)" tile in the profile carousel
- **THEN** the system generates a default localized name (e.g., "New wheelchair")
- **AND** if that name already exists, the system appends an incremented number ("New wheelchair 2")
- **AND** the system creates this profile as Electric by default in the DB
- **AND** switches the active profile context to this newly created profile
- **AND** navigates to the Settings screen to let the user configure it

## ADDED Requirements

### Requirement: Profile Safe Deletion
The system SHALL allow users to delete profiles unless it is the last remaining profile in the database.

#### Scenario: Deleting the only profile
- **WHEN** there is strictly 1 profile in the database and the user opens Settings
- **THEN** the "Delete wheelchair" button is hidden or disabled

#### Scenario: Deleting a profile without history
- **GIVEN** a profile with 0 recorded tracking sessions
- **WHEN** the user clicks the "Delete wheelchair" button and confirms
- **THEN** the profile is deleted from the database
- **AND** the active profile is switched to another available profile

### Requirement: Track Reassignment on Deletion
The system SHALL prompt for conflict resolution when deleting a profile with existing sessions.

#### Scenario: Deleting a profile with history
- **GIVEN** a profile with 5 recorded tracking sessions
- **WHEN** the user attempts to delete it
- **THEN** the system shows a dialog showing the number of attached tracks (5)
- **AND** presents options: "Delete all attached tracks" or "Reassign tracks to a different wheelchair"
- **AND** if reassign is chosen, displays a dropdown of the remaining available user profiles
- **WHEN** the user confirms the reassignment
- **THEN** the tracks are updated in the database to link to the newly chosen profile before deleting the old profile
