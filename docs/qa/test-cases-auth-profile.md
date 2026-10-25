# Тест-кейсы: Auth, профиль, модуль 7

| ID | Название | Шаги | Ожидание |
|----|----------|------|----------|
| AP-01 | Регистрация студента | Валидные email, ФИО, факультет, группа | 201, AR=1000, membership PENDING |
| AP-02 | Регистрация без группы из списка | groupId не из справочника | 400 |
| AP-03 | Регистрация преподавателя | department из списка | 201 |
| AP-04 | Логин | верный пароль | token + role |
| AP-05 | Логин неверный | | 401 |
| AP-06 | Профиль виден другим auth | student A открывает профиль B | ФИО, факультет, группа, АР, БУА, БОА |
| AP-07 | Аноним без токена | GET profile | 401 |
| AP-08 | Verify email | валидный токен | emailVerified=true |
| AP-09 | Без verify — мутация | submit checkpoint | 403 EMAIL_NOT_VERIFIED |
| AP-10 | Подтверждение группы teacher | approve pending | CONFIRMED, задачи появляются |
| AP-11 | Подтверждение старостой | lead approve | CONFIRMED |
| AP-12 | PENDING не получает award | teacher award | 403 / skip |
| AP-13 | История ledger формат | | дата, знак, валюта, причина, кто |
| AP-14 | Список заданий в профиле | | active + completed |
