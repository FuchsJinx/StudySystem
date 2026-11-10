import { useCallback, useEffect, useMemo, useState } from "react";
import { api, ApiError } from "../shared/api/client";
import type {
  AdminStats,
  AdminUser,
  DepartmentAdmin,
  FacultyAdmin,
  GroupAdmin,
  ImportReport,
  Semester,
  SemesterActivation,
  Setting,
} from "../shared/api/adminTypes";
import type { Role } from "../shared/api/types";
import { formatDate, formatDateTime, roleLabels } from "../shared/format";
import {
  Alert,
  Button,
  Card,
  ConfirmDialog,
  EmptyState,
  Field,
  Input,
  Modal,
  PageHeader,
  PageLoader,
  Select,
  StatCard,
} from "../shared/ui";

type AdminTab = "catalogs" | "import" | "semesters" | "settings" | "users";
type CatalogKind = "faculty" | "group" | "department";

interface EditCatalogState {
  kind: CatalogKind;
  id: number;
  name: string;
  facultyId?: number;
}

interface ConfirmState {
  title: string;
  description: string;
  action: () => Promise<void>;
  danger?: boolean;
}

const TABS: Array<{ id: AdminTab; label: string }> = [
  { id: "catalogs", label: "Справочники" },
  { id: "import", label: "Импорт" },
  { id: "semesters", label: "Семестры" },
  { id: "settings", label: "Настройки" },
  { id: "users", label: "Пользователи" },
];

export function AdminPage() {
  const [tab, setTab] = useState<AdminTab>("catalogs");
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [confirm, setConfirm] = useState<ConfirmState | null>(null);
  const [confirmLoading, setConfirmLoading] = useState(false);
  const [edit, setEdit] = useState<EditCatalogState | null>(null);
  const [editSaving, setEditSaving] = useState(false);

  const [faculties, setFaculties] = useState<FacultyAdmin[]>([]);
  const [groups, setGroups] = useState<GroupAdmin[]>([]);
  const [departments, setDepartments] = useState<DepartmentAdmin[]>([]);
  const [newFaculty, setNewFaculty] = useState("");
  const [newDepartment, setNewDepartment] = useState("");
  const [newGroup, setNewGroup] = useState({ name: "", facultyId: "" });

  const [file, setFile] = useState<File | null>(null);
  const [report, setReport] = useState<ImportReport | null>(null);
  const [importing, setImporting] = useState(false);

  const [semesters, setSemesters] = useState<Semester[]>([]);
  const [semesterForm, setSemesterForm] = useState({ name: "", startAt: "", endAt: "" });

  const [settings, setSettings] = useState<Setting[]>([]);
  const [settingsSaving, setSettingsSaving] = useState(false);

  const [users, setUsers] = useState<AdminUser[]>([]);
  const [roleFilter, setRoleFilter] = useState<"" | Role>("");

  const showError = (err: unknown, fallback: string) => {
    setError(err instanceof ApiError ? err.message : fallback);
    setNotice(null);
  };

  const loadStats = useCallback(() => {
    api<AdminStats>("/admin/stats").then(setStats).catch((err) => showError(err, "Не удалось загрузить статистику"));
  }, []);

  const loadCatalogs = useCallback(() => {
    setLoading(true);
    Promise.all([
      api<FacultyAdmin[]>("/admin/faculties"),
      api<GroupAdmin[]>("/admin/groups"),
      api<DepartmentAdmin[]>("/admin/departments"),
    ])
      .then(([facultyList, groupList, departmentList]) => {
        setFaculties(facultyList);
        setGroups(groupList);
        setDepartments(departmentList);
      })
      .catch((err) => showError(err, "Не удалось загрузить справочники"))
      .finally(() => setLoading(false));
  }, []);

  const loadSemesters = useCallback(() => {
    api<Semester[]>("/admin/semesters")
      .then(setSemesters)
      .catch((err) => showError(err, "Не удалось загрузить семестры"));
  }, []);

  const loadSettings = useCallback(() => {
    api<Setting[]>("/admin/settings")
      .then(setSettings)
      .catch((err) => showError(err, "Не удалось загрузить настройки"));
  }, []);

  const loadUsers = useCallback((role: "" | Role) => {
    const query = role ? `?role=${role}` : "";
    api<AdminUser[]>(`/admin/users${query}`)
      .then(setUsers)
      .catch((err) => showError(err, "Не удалось загрузить пользователей"));
  }, []);

  useEffect(() => {
    loadStats();
  }, [loadStats]);

  useEffect(() => {
    setError(null);
    setNotice(null);
    if (tab === "catalogs") loadCatalogs();
    if (tab === "semesters") loadSemesters();
    if (tab === "settings") loadSettings();
    if (tab === "users") loadUsers(roleFilter);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab, loadCatalogs, loadSemesters, loadSettings]);

  useEffect(() => {
    if (tab === "users") loadUsers(roleFilter);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [roleFilter]);

  const afterMutation = (message: string) => {
    setNotice(message);
    setError(null);
    loadStats();
  };

  const createFaculty = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!newFaculty.trim()) return;
    try {
      await api("/admin/faculties", { method: "POST", body: { name: newFaculty.trim() } });
      setNewFaculty("");
      loadCatalogs();
      afterMutation("Факультет добавлен");
    } catch (err) {
      showError(err, "Не удалось добавить факультет");
    }
  };

  const createGroup = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!newGroup.name.trim() || !newGroup.facultyId) return;
    try {
      await api("/admin/groups", {
        method: "POST",
        body: { name: newGroup.name.trim(), facultyId: Number(newGroup.facultyId) },
      });
      setNewGroup({ name: "", facultyId: "" });
      loadCatalogs();
      afterMutation("Группа добавлена");
    } catch (err) {
      showError(err, "Не удалось добавить группу");
    }
  };

  const createDepartment = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!newDepartment.trim()) return;
    try {
      await api("/admin/departments", { method: "POST", body: { name: newDepartment.trim() } });
      setNewDepartment("");
      loadCatalogs();
      afterMutation("Кафедра добавлена");
    } catch (err) {
      showError(err, "Не удалось добавить кафедру");
    }
  };

  const requestDelete = (kind: CatalogKind, id: number, name: string) => {
    const path = kind === "faculty" ? "faculties" : kind === "group" ? "groups" : "departments";
    setConfirm({
      title: `Удалить «${name}»?`,
      description: "Действие необратимо. Удаление возможно, только если нет связанных записей.",
      danger: true,
      action: async () => {
        await api(`/admin/${path}/${id}`, { method: "DELETE" });
        loadCatalogs();
        afterMutation("Запись удалена");
      },
    });
  };

  const saveEdit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!edit) return;
    const path = edit.kind === "faculty" ? "faculties" : edit.kind === "group" ? "groups" : "departments";
    const body =
      edit.kind === "group"
        ? { name: edit.name.trim(), facultyId: edit.facultyId }
        : { name: edit.name.trim() };
    setEditSaving(true);
    try {
      await api(`/admin/${path}/${edit.id}`, { method: "PUT", body });
      setEdit(null);
      loadCatalogs();
      afterMutation("Изменения сохранены");
    } catch (err) {
      showError(err, "Не удалось сохранить изменения");
    } finally {
      setEditSaving(false);
    }
  };

  const uploadImport = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!file) return;
    setImporting(true);
    setReport(null);
    try {
      const form = new FormData();
      form.append("file", file);
      const result = await api<ImportReport>("/admin/import/students", { method: "POST", form });
      setReport(result);
      setNotice(null);
      setError(null);
      loadStats();
    } catch (err) {
      showError(err, "Не удалось импортировать студентов");
    } finally {
      setImporting(false);
    }
  };

  const createSemester = async (event: React.FormEvent) => {
    event.preventDefault();
    try {
      await api("/admin/semesters", {
        method: "POST",
        body: {
          name: semesterForm.name.trim(),
          startAt: semesterForm.startAt,
          endAt: semesterForm.endAt,
        },
      });
      setSemesterForm({ name: "", startAt: "", endAt: "" });
      loadSemesters();
      afterMutation("Семестр создан");
    } catch (err) {
      showError(err, "Не удалось создать семестр");
    }
  };

  const requestActivate = (semester: Semester) => {
    setConfirm({
      title: `Активировать «${semester.name}»?`,
      description:
        "Всем студентам будет начислен семестровый бонус АР с ограничением 1000. Повторная активация запрещена.",
      action: async () => {
        const result = await api<SemesterActivation>(`/admin/semesters/${semester.id}/activate`, {
          method: "POST",
        });
        loadSemesters();
        afterMutation(
          `Семестр активирован. Бонус получили ${result.affectedStudents} студентов, всего ${result.totalBonus} АР.`,
        );
      },
    });
  };

  const saveSettings = async (event: React.FormEvent) => {
    event.preventDefault();
    setSettingsSaving(true);
    try {
      const values = Object.fromEntries(settings.map((setting) => [setting.key, setting.value]));
      const updated = await api<Setting[]>("/admin/settings", {
        method: "PATCH",
        body: { values },
      });
      setSettings(updated);
      afterMutation("Настройки сохранены");
    } catch (err) {
      showError(err, "Не удалось сохранить настройки");
    } finally {
      setSettingsSaving(false);
    }
  };

  const runConfirm = async () => {
    if (!confirm) return;
    setConfirmLoading(true);
    try {
      await confirm.action();
      setConfirm(null);
    } catch (err) {
      showError(err, "Не удалось выполнить действие");
      setConfirm(null);
    } finally {
      setConfirmLoading(false);
    }
  };

  const statCards = useMemo(() => {
    if (!stats) return null;
    return (
      <div className="grid grid--4">
        <StatCard label="Студенты" value={stats.students} hint={`${stats.pendingMemberships} ждут подтверждения`} icon="users" tone="green" />
        <StatCard label="Преподаватели" value={stats.teachers} hint={`${stats.departments} кафедры`} icon="user" tone="blue" />
        <StatCard label="Группы" value={stats.groups} hint={`${stats.faculties} факультета`} icon="book" tone="gold" />
        <StatCard label="Награды в ленте" value={stats.awards} hint={`${stats.checkpoints} задач · ${stats.activities} активностей · ${stats.polls} опросов`} icon="award" tone="red" />
      </div>
    );
  }, [stats]);

  return (
    <>
      <PageHeader
        eyebrow="Модуль 8"
        title="Панель администратора"
        subtitle="Справочники, импорт студентов, семестры, настройки механик и пользователи"
      />

      {statCards}
      {error ? <Alert variant="error" title="Ошибка">{error}</Alert> : null}
      {notice ? <Alert variant="success" title="Готово">{notice}</Alert> : null}

      <div className="admin-tabs" role="tablist">
        {TABS.map((item) => (
          <button
            key={item.id}
            type="button"
            role="tab"
            aria-selected={tab === item.id}
            className={`admin-tab ${tab === item.id ? "admin-tab--active" : ""}`}
            onClick={() => setTab(item.id)}
          >
            {item.label}
          </button>
        ))}
      </div>

      {tab === "catalogs" ? (
        loading ? (
          <PageLoader label="Загружаем справочники…" />
        ) : (
          <div className="admin-catalogs">
            <Card>
              <h2>Факультеты</h2>
              <form className="inline-form" onSubmit={createFaculty}>
                <Input
                  aria-label="Новый факультет"
                  placeholder="Название факультета"
                  value={newFaculty}
                  onChange={(event) => setNewFaculty(event.target.value)}
                />
                <Button type="submit" size="sm">Добавить</Button>
              </form>
              {faculties.length === 0 ? (
                <EmptyState title="Факультетов нет" />
              ) : (
                <ul className="admin-list">
                  {faculties.map((faculty) => (
                    <li key={faculty.id}>
                      <div>
                        <strong>{faculty.name}</strong>
                        <small>{faculty.groupsCount} групп</small>
                      </div>
                      <div className="admin-list__actions">
                        <button
                          type="button"
                          className="text-link text-link--button"
                          onClick={() => setEdit({ kind: "faculty", id: faculty.id, name: faculty.name })}
                        >
                          Изменить
                        </button>
                        <button
                          type="button"
                          className="text-link text-link--button text-danger"
                          onClick={() => requestDelete("faculty", faculty.id, faculty.name)}
                        >
                          Удалить
                        </button>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </Card>

            <Card>
              <h2>Группы</h2>
              <form className="inline-form" onSubmit={createGroup}>
                <Input
                  aria-label="Новая группа"
                  placeholder="Название группы"
                  value={newGroup.name}
                  onChange={(event) => setNewGroup({ ...newGroup, name: event.target.value })}
                />
                <Select
                  aria-label="Факультет группы"
                  value={newGroup.facultyId}
                  onChange={(event) => setNewGroup({ ...newGroup, facultyId: event.target.value })}
                >
                  <option value="">Факультет</option>
                  {faculties.map((faculty) => (
                    <option key={faculty.id} value={faculty.id}>{faculty.name}</option>
                  ))}
                </Select>
                <Button type="submit" size="sm">Добавить</Button>
              </form>
              {groups.length === 0 ? (
                <EmptyState title="Групп нет" />
              ) : (
                <ul className="admin-list">
                  {groups.map((group) => (
                    <li key={group.id}>
                      <div>
                        <strong>{group.name}</strong>
                        <small>{group.facultyName}</small>
                      </div>
                      <div className="admin-list__actions">
                        <button
                          type="button"
                          className="text-link text-link--button"
                          onClick={() =>
                            setEdit({
                              kind: "group", id: group.id, name: group.name, facultyId: group.facultyId,
                            })
                          }
                        >
                          Изменить
                        </button>
                        <button
                          type="button"
                          className="text-link text-link--button text-danger"
                          onClick={() => requestDelete("group", group.id, group.name)}
                        >
                          Удалить
                        </button>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </Card>

            <Card>
              <h2>Кафедры</h2>
              <form className="inline-form" onSubmit={createDepartment}>
                <Input
                  aria-label="Новая кафедра"
                  placeholder="Название кафедры"
                  value={newDepartment}
                  onChange={(event) => setNewDepartment(event.target.value)}
                />
                <Button type="submit" size="sm">Добавить</Button>
              </form>
              {departments.length === 0 ? (
                <EmptyState title="Кафедр нет" />
              ) : (
                <ul className="admin-list">
                  {departments.map((department) => (
                    <li key={department.id}>
                      <div>
                        <strong>{department.name}</strong>
                        <small>{department.teachersCount} преподавателей</small>
                      </div>
                      <div className="admin-list__actions">
                        <button
                          type="button"
                          className="text-link text-link--button"
                          onClick={() =>
                            setEdit({ kind: "department", id: department.id, name: department.name })
                          }
                        >
                          Изменить
                        </button>
                        <button
                          type="button"
                          className="text-link text-link--button text-danger"
                          onClick={() => requestDelete("department", department.id, department.name)}
                        >
                          Удалить
                        </button>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </Card>
          </div>
        )
      ) : null}

      {tab === "import" ? (
        <Card>
          <h2>Импорт студентов</h2>
          <p className="muted">
            CSV или Excel с колонками <code>email, fullName, facultyName, groupName, passwordOptional</code>.
            Студенты создаются с подтверждённым email и подтверждённой группой. Отсутствующие факультеты и
            группы создаются автоматически.
          </p>
          <form className="form" onSubmit={uploadImport}>
            <Field label="Файл" htmlFor="import-file" hint="CSV, XLSX или XLS">
              <input
                id="import-file"
                type="file"
                className="file-input"
                accept=".csv,.xlsx,.xls"
                onChange={(event) => setFile(event.target.files?.[0] ?? null)}
              />
            </Field>
            <div>
              <Button type="submit" loading={importing} disabled={!file} icon="upload">
                Импортировать
              </Button>
            </div>
          </form>
          {report ? (
            <div className="import-report">
              <div className="grid grid--3">
                <StatCard label="Создано" value={report.created} tone="green" />
                <StatCard label="Пропущено" value={report.skipped} tone="gold" />
                <StatCard label="Ошибок" value={report.errors.length} tone="red" />
              </div>
              {report.errors.length > 0 ? (
                <div className="table-wrap">
                  <table className="table">
                    <thead><tr><th>Строка</th><th>Ошибка</th></tr></thead>
                    <tbody>
                      {report.errors.map((importError, index) => (
                        <tr key={`${importError.row}-${index}`}>
                          <td>{importError.row}</td>
                          <td>{importError.message}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <Alert variant="success">Ошибок нет</Alert>
              )}
            </div>
          ) : null}
        </Card>
      ) : null}

      {tab === "semesters" ? (
        <div className="dashboard-grid">
          <Card>
            <h2>Новый семестр</h2>
            <form className="form" onSubmit={createSemester}>
              <Field label="Название" htmlFor="semester-name" required>
                <Input
                  id="semester-name"
                  value={semesterForm.name}
                  onChange={(event) => setSemesterForm({ ...semesterForm, name: event.target.value })}
                  placeholder="Весна 2027"
                  required
                />
              </Field>
              <div className="form-row">
                <Field label="Начало" htmlFor="semester-start" required>
                  <Input
                    id="semester-start"
                    type="date"
                    value={semesterForm.startAt}
                    onChange={(event) => setSemesterForm({ ...semesterForm, startAt: event.target.value })}
                    required
                  />
                </Field>
                <Field label="Окончание" htmlFor="semester-end" required>
                  <Input
                    id="semester-end"
                    type="date"
                    value={semesterForm.endAt}
                    onChange={(event) => setSemesterForm({ ...semesterForm, endAt: event.target.value })}
                    required
                  />
                </Field>
              </div>
              <Button type="submit">Создать семестр</Button>
            </form>
          </Card>
          <Card>
            <h2>Семестры</h2>
            {semesters.length === 0 ? (
              <EmptyState title="Семестров нет" />
            ) : (
              <ul className="admin-list">
                {semesters.map((semester) => (
                  <li key={semester.id}>
                    <div>
                      <strong>{semester.name}</strong>
                      <small>
                        {formatDate(semester.startAt)} — {formatDate(semester.endAt)}
                        {semester.activatedAt ? ` · активирован ${formatDateTime(semester.activatedAt)}` : ""}
                      </small>
                    </div>
                    {semester.active ? (
                      <span className="badge badge--status-accepted">Активен</span>
                    ) : semester.activatedAt ? (
                      <span className="badge">Активирован ранее</span>
                    ) : (
                      <Button size="sm" onClick={() => requestActivate(semester)}>Активировать</Button>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </Card>
        </div>
      ) : null}

      {tab === "settings" ? (
        <Card>
          <h2>Настройки механик</h2>
          <p className="muted">Изменения применяются к фоновым задачам и проверкам бюджета.</p>
          <form className="form" onSubmit={saveSettings}>
            <div className="settings-grid">
              {settings.map((setting, index) => (
                <Field
                  key={setting.key}
                  label={`${setting.title} (${setting.unit})`}
                  htmlFor={`setting-${setting.key}`}
                  hint={setting.description}
                >
                  <Input
                    id={`setting-${setting.key}`}
                    type="number"
                    min={setting.min}
                    max={setting.max}
                    value={setting.value}
                    onChange={(event) => {
                      const value = event.target.value;
                      setSettings((current) =>
                        current.map((item, itemIndex) =>
                          itemIndex === index ? { ...item, value } : item,
                        ),
                      );
                    }}
                  />
                </Field>
              ))}
            </div>
            <div>
              <Button type="submit" loading={settingsSaving}>Сохранить настройки</Button>
            </div>
          </form>
        </Card>
      ) : null}

      {tab === "users" ? (
        <Card>
          <div className="section-head">
            <h2>Пользователи</h2>
            <div className="month-picker">
              <Field label="Роль" htmlFor="users-role">
                <Select
                  id="users-role"
                  value={roleFilter}
                  onChange={(event) => setRoleFilter(event.target.value as "" | Role)}
                >
                  <option value="">Все роли</option>
                  <option value="STUDENT">Студенты</option>
                  <option value="TEACHER">Преподаватели</option>
                  <option value="ADMIN">Администраторы</option>
                </Select>
              </Field>
            </div>
          </div>
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>ФИО</th>
                  <th>Email</th>
                  <th>Роль</th>
                  <th>Группа / кафедра</th>
                  <th>Email</th>
                  <th>Статус</th>
                </tr>
              </thead>
              <tbody>
                {users.map((user) => (
                  <tr key={user.id}>
                    <td>{user.fullName}</td>
                    <td>{user.email}</td>
                    <td>{roleLabels[user.role]}</td>
                    <td>{user.groupName ?? user.departmentName ?? "—"}</td>
                    <td>{user.emailVerified ? "Подтверждён" : "Не подтверждён"}</td>
                    <td>
                      {user.membershipStatus === "PENDING" ? (
                        <span className="badge badge--membership-pending">Ожидает</span>
                      ) : user.membershipStatus === "CONFIRMED" ? (
                        <span className="badge badge--status-accepted">Подтверждён</span>
                      ) : (
                        <span className="muted">—</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      ) : null}

      <Modal
        open={edit !== null}
        title="Изменить запись"
        onClose={() => !editSaving && setEdit(null)}
        footer={
          <>
            <Button variant="ghost" onClick={() => setEdit(null)} disabled={editSaving}>Отмена</Button>
            <Button form="edit-catalog-form" type="submit" loading={editSaving}>Сохранить</Button>
          </>
        }
      >
        {edit ? (
          <form id="edit-catalog-form" className="form" onSubmit={saveEdit}>
            <Field label="Название" htmlFor="edit-name" required>
              <Input
                id="edit-name"
                value={edit.name}
                onChange={(event) => setEdit({ ...edit, name: event.target.value })}
                required
              />
            </Field>
            {edit.kind === "group" ? (
              <Field label="Факультет" htmlFor="edit-faculty" required>
                <Select
                  id="edit-faculty"
                  value={edit.facultyId ?? ""}
                  onChange={(event) => setEdit({ ...edit, facultyId: Number(event.target.value) })}
                  required
                >
                  {faculties.map((faculty) => (
                    <option key={faculty.id} value={faculty.id}>{faculty.name}</option>
                  ))}
                </Select>
              </Field>
            ) : null}
          </form>
        ) : null}
      </Modal>

      <ConfirmDialog
        open={confirm !== null}
        title={confirm?.title ?? ""}
        description={confirm?.description ?? ""}
        danger={confirm?.danger}
        loading={confirmLoading}
        confirmLabel="Подтвердить"
        onConfirm={runConfirm}
        onClose={() => !confirmLoading && setConfirm(null)}
      />
    </>
  );
}
