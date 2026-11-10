export interface AiTextResponse {
  text: string;
  provider: "demo" | "openai";
}

export interface AiRatingTips {
  tips: string[];
  provider: "demo" | "openai";
}

export const providerLabel = (provider: "demo" | "openai"): string =>
  provider === "demo" ? "Демо-режим ИИ (ключ не настроен)" : "Ответ сгенерирован ИИ";
