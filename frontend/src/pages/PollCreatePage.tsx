import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api, ApiError } from "../shared/api/client";
import type { PollQuestionType, PollSummary } from "../shared/api/pollTypes";
import type { Group } from "../shared/api/types";
import { toDateTimeInputValue, toIsoFromLocalInput } from "../shared/format";
import {
  Alert,
  Button,
  Card,
  Field,
  Input,
  PageHeader,
  Select,
  Textarea,
} from "../shared/ui";

interface QuestionDraft {
  text: string;
  type: PollQuestionType;
  options: string[];
}

function defaultEndsAt(): string {
  const date = new Date();
  date.setDate(date.getDate() + 7);
  date.setHours(20, 0, 0, 0);
  return toDateTimeInputValue(date);
}

function newQuestion(): QuestionDraft {
  return { text: "", type: "SINGLE", options: ["", ""] };
}

export function PollCreatePage() {
  const navigate = useNavigate();
  const [groups, setGroups] = useState<Group[]>([]);
  const [form, setForm] = useState({
    title: "",
    description: "",
    endsAt: defaultEndsAt(),
    isAnonymous: false,
    groupIds: [] as number[],
  });
  const [questions, setQuestions] = useState<QuestionDraft[]>([newQuestion()]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    api<Group[]>("/catalog/groups").then(setGroups).catch(() => setGroups([]));
  }, []);

  const updateQuestion = (index: number, patch: Partial<QuestionDraft>) => {
    setQuestions((current) =>
      current.map((question, questionIndex) =>
        questionIndex === index ? { ...question, ...patch } : question,
      ),
    );
  };

  const updateOption = (questionIndex: number, optionIndex: number, value: string) => {
    setQuestions((current) =>
      current.map((question, index) =>
        index === questionIndex
          ? {
              ...question,
              options: question.options.map((option, currentOptionIndex) =>
                currentOptionIndex === optionIndex ? value : option,
              ),
            }
          : question,
      ),
    );
  };

  const addOption = (questionIndex: number) => {
    setQuestions((current) =>
      current.map((question, index) =>
        index === questionIndex ? { ...question, options: [...question.options, ""] } : question,
      ),
    );
  };

  const removeOption = (questionIndex: number, optionIndex: number) => {
    setQuestions((current) =>
      current.map((question, index) => {
        if (index !== questionIndex || question.options.length <= 2) return question;
        return { ...question, options: question.options.filter((_, i) => i !== optionIndex) };
      }),
    );
  };

  const toggleGroup = (id: number) => {
    setForm((current) => ({
      ...current,
      groupIds: current.groupIds.includes(id)
        ? current.groupIds.filter((groupId) => groupId !== id)
        : [...current.groupIds, id],
    }));
  };

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);
    if (form.groupIds.length === 0) {
      setError("Выберите хотя бы одну группу");
      return;
    }
    const cleanedQuestions = questions.map((question) => ({
      text: question.text.trim(),
      type: question.type,
      options: question.options.map((option) => option.trim()).filter(Boolean),
    }));
    if (cleanedQuestions.some((question) => !question.text)) {
      setError("Заполните тексты всех вопросов");
      return;
    }
    if (cleanedQuestions.some((question) => question.options.length < 2)) {
      setError("В каждом вопросе нужно минимум два варианта ответа");
      return;
    }
    setLoading(true);
    try {
      const created = await api<PollSummary>("/polls", {
        method: "POST",
        body: {
          title: form.title.trim(),
          description: form.description.trim(),
          endsAt: toIsoFromLocalInput(form.endsAt),
          targetGroupIds: form.groupIds,
          isAnonymous: form.isAnonymous,
          questions: cleanedQuestions,
        },
      });
      navigate(`/polls/${created.id}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Не удалось создать опрос");
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <PageHeader
        eyebrow="Модуль 5"
        title="Новый опрос"
        subtitle="Вопросы с одним или несколькими вариантами ответа, анонимность и целевые группы"
      />

      <form className="form poll-create" onSubmit={submit}>
        {error ? <Alert variant="error" title="Проверьте форму">{error}</Alert> : null}

        <Card>
          <h2>Основное</h2>
          <Field label="Название опроса" htmlFor="poll-title" required>
            <Input
              id="poll-title"
              value={form.title}
              onChange={(event) => setForm({ ...form, title: event.target.value })}
              placeholder="Формат итогового зачёта"
              maxLength={255}
              required
            />
          </Field>
          <Field label="Описание" htmlFor="poll-description">
            <Textarea
              id="poll-description"
              value={form.description}
              onChange={(event) => setForm({ ...form, description: event.target.value })}
              maxLength={5000}
            />
          </Field>
          <div className="form-row">
            <Field label="Дата и время окончания" htmlFor="poll-ends" required>
              <Input
                id="poll-ends"
                type="datetime-local"
                value={form.endsAt}
                onChange={(event) => setForm({ ...form, endsAt: event.target.value })}
                required
              />
            </Field>
            <div className="field">
              <span className="field__label">Анонимность</span>
              <label className="checkbox-row">
                <input
                  type="checkbox"
                  checked={form.isAnonymous}
                  onChange={(event) => setForm({ ...form, isAnonymous: event.target.checked })}
                />
                <span>
                  <strong>Анонимный опрос</strong>
                  <small>Преподаватель видит только агрегированные ответы</small>
                </span>
              </label>
            </div>
          </div>
          <div className="field">
            <span className="field__label">Группы<span className="field__required"> *</span></span>
            <div className="group-picker">
              {groups.map((group) => (
                <label
                  key={group.id}
                  className={`group-picker__item ${form.groupIds.includes(group.id) ? "group-picker__item--active" : ""}`}
                >
                  <input
                    type="checkbox"
                    checked={form.groupIds.includes(group.id)}
                    onChange={() => toggleGroup(group.id)}
                  />
                  <span>{group.name}</span>
                  <small>{group.facultyName}</small>
                </label>
              ))}
            </div>
          </div>
        </Card>

        <Card>
          <div className="section-head">
            <div>
              <h2>Вопросы</h2>
              <p className="muted">Минимум два варианта ответа в каждом вопросе.</p>
            </div>
            <Button
              type="button"
              variant="secondary"
              onClick={() => setQuestions((current) => [...current, newQuestion()])}
            >
              + Вопрос
            </Button>
          </div>

          {questions.map((question, questionIndex) => (
            <div className="poll-question-editor" key={questionIndex}>
              <div className="poll-question-editor__head">
                <strong>Вопрос {questionIndex + 1}</strong>
                {questions.length > 1 ? (
                  <button
                    type="button"
                    className="text-link text-link--button"
                    onClick={() => setQuestions((current) => current.filter((_, index) => index !== questionIndex))}
                  >
                    Удалить
                  </button>
                ) : null}
              </div>
              <Field label="Текст вопроса" htmlFor={`question-${questionIndex}`} required>
                <Input
                  id={`question-${questionIndex}`}
                  value={question.text}
                  onChange={(event) => updateQuestion(questionIndex, { text: event.target.value })}
                  placeholder="Предпочтительный формат зачёта"
                  maxLength={1024}
                  required
                />
              </Field>
              <Field label="Тип ответа" htmlFor={`question-type-${questionIndex}`}>
                <Select
                  id={`question-type-${questionIndex}`}
                  value={question.type}
                  onChange={(event) =>
                    updateQuestion(questionIndex, { type: event.target.value as PollQuestionType })
                  }
                >
                  <option value="SINGLE">Один вариант</option>
                  <option value="MULTIPLE">Несколько вариантов</option>
                </Select>
              </Field>
              <div className="poll-options-editor">
                {question.options.map((option, optionIndex) => (
                  <div className="poll-option-row" key={optionIndex}>
                    <Input
                      value={option}
                      onChange={(event) => updateOption(questionIndex, optionIndex, event.target.value)}
                      placeholder={`Вариант ${optionIndex + 1}`}
                      aria-label={`Вопрос ${questionIndex + 1}, вариант ${optionIndex + 1}`}
                      maxLength={512}
                    />
                    {question.options.length > 2 ? (
                      <button
                        type="button"
                        className="icon-button"
                        aria-label="Удалить вариант"
                        onClick={() => removeOption(questionIndex, optionIndex)}
                      >
                        ×
                      </button>
                    ) : null}
                  </div>
                ))}
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={() => addOption(questionIndex)}
                >
                  + Вариант
                </Button>
              </div>
            </div>
          ))}
        </Card>

        <div className="form-actions">
          <Button type="submit" loading={loading}>Создать опрос</Button>
        </div>
      </form>
    </>
  );
}
