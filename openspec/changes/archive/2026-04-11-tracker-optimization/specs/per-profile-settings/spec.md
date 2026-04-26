## ADDED Requirements

### Requirement: Wheelchair Icon Selection
Паспорт профиля (ProfilePassportScreen) SHALL предоставлять пользователю UI для выбора векторной иконки коляски из загруженного набора.

#### Scenario: Selecting profile icon
- **WHEN** пользователь открывает экран редактирования профиля
- **THEN** рядом с названием отображается элемент (Dropdown/Выпадающий список), позволяющий выбрать иконку (например: "Стандартная (ручная)", "Permobil F", "Permobil M", "Optimus")
- **AND** при выборе новой иконки, её `iconId` (R.drawable...) сохраняется в соответствующее поле `ProfileEntity`
