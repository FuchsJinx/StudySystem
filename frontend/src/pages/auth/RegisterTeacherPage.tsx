import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api, ApiError } from "../../shared/api/client";
import type { Department } from "../../shared/api/types";
import { useAuth } from "../../shared/auth/AuthContext";
import { Alert, Button, Field, Input, Select } from "../../shared/ui";
import { AuthLayout } from "./AuthLayout";

export function RegisterTeacherPage() {
  const { registerTeacher } = useAuth();
  const navigate = useNavigate();
  const [departments, setDepartments] = useState<Department[]>([]);
  const [form, setForm] = useState({
    fullName: "",
    email: "",
    password: "",
    confirmPassword: "",
    departmentId: "",
  });
  const [error, setError] = useState<string | null>(null);
  const [details, setDetails] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    api<Department[]>("/catalog/departments")
      .then(setDepartments)
      .catch(() => setDepartments([]));
  }, []);

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
      await registerTeacher({
        email: form.email.trim(),
        password: form.password,
        fullName: form.fullName.trim(),
        departmentId: Number(form.departmentId),
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
      title="Регистрация преподавателя"
      subtitle="Выберите кафедру из справочника"
      footer={
        <div className="auth-links">
          <span>Уже есть аккаунт?</span>
          <Link to="/login">Войти</Link>
          <span className="auth-links__sep">·</span>
          <Link to="/register">Я студент</Link>
        </div>
      }
    >
      <form className="form" onSubmit={submit}>
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

        <Field label="ФИО" htmlFor="teacher-name" required>
          <Input
            id="teacher-name"
            placeholder="Петров Пётр Петрович"
            value={form.fullName}
            onChange={(event) => update("fullName", event.target.value)}
            required
          />
        </Field>
        <Field label="Email" htmlFor="teacher-email" required>
          <Input
            id="teacher-email"
            type="email"
            autoComplete="email"
            placeholder="teacher@university.ru"
            value={form.email}
            onChange={(event) => update("email", event.target.value)}
            required
          />
        </Field>
        <Field label="Кафедра" htmlFor="teacher-department" required>
          <Select
            id="teacher-department"
            value={form.departmentId}
            onChange={(event) => update("departmentId", event.target.value)}
            required
          >
            <option value="">Выберите кафедру</option>
            {departments.map((department) => (
              <option key={department.id} value={department.id}>
                {department.name}
              </option>
            ))}
          </Select>
        </Field>
        <div className="form-row">
          <Field label="Пароль" htmlFor="teacher-password" hint="Минимум 8 символов" required>
            <Input
              id="teacher-password"
              type="password"
              autoComplete="new-password"
              value={form.password}
              onChange={(event) => update("password", event.target.value)}
              minLength={8}
              required
            />
          </Field>
          <Field label="Повторите пароль" htmlFor="teacher-password2" required>
            <Input
              id="teacher-password2"
              type="password"
              autoComplete="new-password"
              value={form.confirmPassword}
              onChange={(event) => update("confirmPassword", event.target.value)}
              minLength={8}
              required
            />
          </Field>
        </div>
        <p className="form-note">
          После подтверждения email вы сможете создавать задания и активности для учебных групп.
        </p>

        <Button type="submit" loading={loading} className="button--block">
          Зарегистрироваться
        </Button>
      </form>
    </AuthLayout>
  );
}
