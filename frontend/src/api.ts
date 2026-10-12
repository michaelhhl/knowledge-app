export type AnswerStatus = 'grounded' | 'insufficient' | 'refused';

export interface Citation {
  file: string;
  heading: string;
  title?: string;
  score: number;
}

export interface Related {
  file: string;
  title: string;
}

export interface RetrievalHit {
  file: string;
  heading: string;
  score: number;
}

export interface AskResponse {
  answer_status: AnswerStatus;
  answer_markdown: string;
  citations: Citation[];
  related: Related[];
  retrieval: RetrievalHit[];
}

export interface HistoryTurn {
  question: string;
  answer: string;
}

export interface TocItem {
  title: string;
  path: string;
}

export interface Toc {
  empty: boolean;
  items: TocItem[];
  others: TocItem[];
}

export interface DocumentView {
  path: string;
  id: string;
  title: string;
  tags: string[];
  updated: string | null;
  markdown: string;
}

export interface IndexStatus {
  state: 'NOT_READY' | 'BUILDING' | 'READY' | 'FAILED';
  builtAt: string | null;
  documentCount: number;
  chunkCount: number;
  stale: boolean;
  lastError: string | null;
}

export interface QaStatus {
  llm_configured: boolean;
}

export class ApiError extends Error {
  readonly code: string;
  readonly status: number;

  constructor(code: string, message: string, status: number) {
    super(message);
    this.code = code;
    this.status = status;
  }
}

export const NETWORK_ERROR = 'NETWORK_ERROR';

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(url, init);
  } catch {
    throw new ApiError(NETWORK_ERROR, '无法连接服务，请确认后端已启动。', 0);
  }
  const text = await response.text();
  let body: unknown = null;
  if (text) {
    try {
      body = JSON.parse(text);
    } catch {
      body = null;
    }
  }
  if (!response.ok) {
    const err = (body ?? {}) as { code?: string; message?: string };
    throw new ApiError(
      err.code ?? 'HTTP_ERROR',
      err.message ?? `请求失败（HTTP ${response.status}）。`,
      response.status,
    );
  }
  return body as T;
}

export const api = {
  toc: () => request<Toc>('/api/knowledge/toc'),
  doc: (path: string) => request<DocumentView>(`/api/knowledge/doc?path=${encodeURIComponent(path)}`),
  ask: (question: string, history: HistoryTurn[]) =>
    request<AskResponse>('/api/qa/ask', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ question, history }),
    }),
  qaStatus: () => request<QaStatus>('/api/qa/status'),
  indexStatus: () => request<IndexStatus>('/api/index/status'),
  rebuildIndex: () => request<IndexStatus>('/api/index/rebuild', { method: 'POST' }),
};
