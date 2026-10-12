import { useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { api, ApiError, type AskResponse, type HistoryTurn } from '../api';
import { AnswerCard } from '../components/AnswerCard';
import { Banner } from '../components/Banner';

/** FR-08: at most this many past turns are sent with each question. */
const MAX_HISTORY_TURNS = 10;
const LLM_NOT_CONFIGURED_TEXT = '未配置 LLM。请复制 .env.example 为 .env 并填写密钥。';

interface Turn {
  id: number;
  question: string;
  response: AskResponse;
}

function errorText(error: ApiError): string {
  if (error.code === 'LLM_NOT_CONFIGURED') return LLM_NOT_CONFIGURED_TEXT;
  return error.message;
}

export function QaPage() {
  const [question, setQuestion] = useState('');
  const [turns, setTurns] = useState<Turn[]>([]);
  const [error, setError] = useState<ApiError | null>(null);
  const nextId = useRef(1);
  const threadEnd = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  const qaStatus = useQuery({ queryKey: ['qa-status'], queryFn: api.qaStatus });
  const toc = useQuery({ queryKey: ['toc'], queryFn: api.toc });

  const ask = useMutation({
    mutationFn: ({ q, history }: { q: string; history: HistoryTurn[] }) => api.ask(q, history),
  });

  useEffect(() => {
    threadEnd.current?.scrollIntoView?.({ block: 'end' });
  }, [turns.length, ask.isPending]);

  const submit = (event?: FormEvent) => {
    event?.preventDefault();
    const q = question.trim();
    if (!q || ask.isPending) return;
    const history = turns
      .slice(-MAX_HISTORY_TURNS)
      .map((t) => ({ question: t.question, answer: t.response.answer_markdown }));
    setError(null);
    setQuestion('');
    ask.mutate(
      { q, history },
      {
        onSuccess: (response) => {
          setTurns((prev) => [...prev, { id: nextId.current++, question: q, response }]);
        },
        onError: (err) => {
          setError(err instanceof ApiError ? err : new ApiError('UNKNOWN', '请求失败，请重试。', 0));
          setQuestion((current) => current || q);
        },
        onSettled: () => inputRef.current?.focus(),
      },
    );
  };

  const onKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === 'Enter' && (event.ctrlKey || event.metaKey)) {
      event.preventDefault();
      submit();
    }
  };

  const newSession = () => {
    setTurns([]);
    setError(null);
    ask.reset();
    inputRef.current?.focus();
  };

  const llmMissing = qaStatus.data && !qaStatus.data.llm_configured;
  const knowledgeEmpty = toc.data?.empty;

  return (
    <section className="qa-page" aria-label="问答">
      {error && <Banner>{errorText(error)}</Banner>}
      {!error && llmMissing && <Banner>{LLM_NOT_CONFIGURED_TEXT}</Banner>}
      {knowledgeEmpty && (
        <Banner tone="warning">知识库还没有已发布的文档。请在 knowledge/ 添加 Markdown。</Banner>
      )}

      <div className="thread" aria-live="polite">
        {turns.length === 0 && !ask.isPending && (
          <p className="empty-hint">向知识库提问。答案只会依据已发布的 Markdown，并附上出处。</p>
        )}
        {turns.map((turn) => (
          <div key={turn.id} className="turn">
            <div className="question-bubble">
              <span className="sr-only">问题：</span>
              {turn.question}
            </div>
            <AnswerCard response={turn.response} />
          </div>
        ))}
        {ask.isPending && ask.variables && (
          <div className="turn">
            <div className="question-bubble">{ask.variables.q}</div>
          </div>
        )}
        <div ref={threadEnd} />
      </div>

      <div className="composer">
        <form onSubmit={submit} className="composer-form">
          <label htmlFor="question" className="sr-only">
            问题
          </label>
          <textarea
            id="question"
            ref={inputRef}
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            onKeyDown={onKeyDown}
            rows={3}
            maxLength={2000}
            placeholder="例如：申请韩国签证对照片有什么要求？（Ctrl+Enter 发送）"
          />
          <div className="composer-actions">
            <button type="submit" className="btn btn-primary" disabled={!question.trim() || ask.isPending}>
              发送
            </button>
            <button type="button" className="btn" onClick={newSession} disabled={ask.isPending}>
              新会话
            </button>
          </div>
        </form>
        <p className="busy-line" role="status">
          {ask.isPending ? '正在检索 / 正在生成' : ''}
        </p>
      </div>
    </section>
  );
}
