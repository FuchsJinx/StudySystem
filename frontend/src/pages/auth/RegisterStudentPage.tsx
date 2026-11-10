import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api, ApiError } from "../../shared/api/client";
import type { Faculty, Group } from "../../shared/api/types";
import { useAuth } from "../../shared/auth/AuthContext";
import { Alert, Button, Field, Input, Select } from "../../shared/ui";
import { AuthLayout } from "./AuthLayout";

export function RegisterStudentPage() {
  const { registerStudent } = useAuth();
  const navigate = useNavigate();
  const [faculties, setFaculties] = useState<Faculty[]>([]);
  const [groups, setGroups] = useState<Group[]>([]);
  const [form, setForm] = useState({
    fullName: "",
    email: "",
    password: "",
    confirmPassword: "",
    facultyId: "",
    groupId: "",
  });
  const [error, setError] = useState<string | null>(null);
  const [details, setDetails] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [catalogError, setCatalogError] = useState(false);

  useEffect(() => {
    api<Faculty[]>("/catalog/faculties")
      .then(setFaculties)
      .catch(() => setCatalogError(true));
  }, []);

  useEffect(() => {
    if (!form.facultyId) {
      setGroups([]);
      return;
    }
    api<Group[]>(`/catalog/faculties/${form.facultyId}/groups`)
      .then(setGroups)
      .catch(() => setGroups([]));
  }, [form.facultyId]);

  const update = (key: keyof typeof form, value: string) => {
    setForm((current) => ({ ...current, [key]: value }));
  };

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);
    setDetails([]);
    if (form.password !== form.confirmPassword) {
      setError("Пароли не совпадают");
      return;
    }
    setLoading(true);
    try {
      await registerStudent({
        email: form.email.trim(),
        password: form.password,
        fullName: form.fullName.trim(),
        facultyId: Number(form.facultyId),
        groupId: Number(form.groupId),
      });
      navigate("/", { replace: true });
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
        setDetails(err.details);
      } else {
        setError("Не удалось зарегистрироваться. Попробуйте ещё раз.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout
      title="Регистрация студента"
      subtitle="Укажите факультет и группу из справочника"
      footer={
        <div className="auth-links">
          <span>Уже есть аккаунт?</span>
          <Link to="/login">Войти</Link>
          <span className="auth-links__sep">·</span>
          <Link to="/register/teacher">Я преподаватель</Link>
        </div>
      }
    >
      <form className="form" onSubmit={submit}>
        {catalogError ? (
          <Alert variant="error" title="Справочники недоступны">
            Не удалось загрузить список факультетов. Обновите страницу позже.
          </Alert>
        ) : null}
        {error ? (
          <Alert variant="error" title="Не удалось зарегистрироваться">
            {error}
            {details.length > 0 ? (
              <ul className="alert__list">
                {details.map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
            ) : null}
          </Alert>
        ) : null}

        <Field label="ФИО" htmlFor="student-name" required>
          <Input
            id="student-name"
            placeholder="Иванов Иван Иванович"
            value={form.fullName}
            onChange={(event) => update("fullName", event.target.value)}
            required
          />
        </Field>
        <Field label="Email" htmlFor="student-email" required>
          <Input
            id="student-email"
            type="email"
            autoComplete="email"
            placeholder="you@university.ru"
            value={form.email}
            onChange={(event) => update("email", event.target.value)}
            required
          />
        </Field>
        <div className="form-row">
          <Field label="Пароль" htmlFor="student-password" hint="Минимум 8 символов" required>
            <Input
              id="student-password"
              type="password"
              autoComplete="new-password"
              value={form.password}
              onChange={(event) => update("password", event.target.value)}
              minLength={8}
              required
            />
          </Field>
          <Field label="Повторите пароль" htmlFor="student-password2" required>
            <Input
              id="student-password2"
              type="password"
              autoComplete="new-password"
              value={form.confirmPassword}
              onChange={(event) => update("confirmPassword", event.target.value)}
              minLength={8}
              required
            />
          </Field>
        </div>
        <div className="form-row">
          <Field label="Факультет" htmlFor="student-faculty" required>
            <Select
              id="student-faculty"
              value={form.facultyId}
              onChange={(event) => {
                update("facultyId", event.target.value);
                update("groupId", "");
              }}
              required
            >
              <option value="">Выберите факультет</option>
              {faculties.map((faculty) => (
                <option key={faculty.id} value={faculty.id}>
                  {faculty.name}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Группа" htmlFor="student-group" required>
            <Select
              id="student-group"
              value={form.groupId}
              onChange={(event) => update("groupId", event.target.value)}
              disabled={!form.facultyId}
              required
            >
              <option value="">{form.facultyId ? "Выберите группу" : "Сначала факультет"}</option>
              {groups.map((group) => (
                <option key={group.id} value={group.id}>
                  {group.name}
                </option>
              ))}
            </Select>
          </Field>
        </div>

        <p className="form-note">
          После регистрации мы отправим письмо для подтверждения email. Принадлежность к группе
          подтвердит преподаватель или староста.
        </p>

        <Button type="submit" loading={loading} className="button--block">
          Зарегистрироваться
        </Button>
      </form>
    </AuthLayout>
  );
}
