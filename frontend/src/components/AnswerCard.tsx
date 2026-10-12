import { Link } from 'react-router-dom';
import type { AnswerStatus, AskResponse, Citation } from '../api';
import { docHref } from '../links';
import { MarkdownView } from './MarkdownView';

const BADGES: Record<AnswerStatus, { label: string; icon: string }> = {
  grounded: { label: '有依据', icon: '✓' },
  insufficient: { label: '依据不足', icon: '?' },
  refused: { label: '已拒绝', icon: '⊘' },
};

function citationLabel(c: Citation): string {
  if (c.title) return c.heading && c.heading !== c.title ? `${c.title} · ${c.heading}` : c.title;
  return c.heading ? `${c.file}#${c.heading}` : c.file;
}

export function AnswerCard({ response }: { response: AskResponse }) {
  const badge = BADGES[response.answer_status] ?? BADGES.insufficient;

  return (
    <article className={`answer-card status-${response.answer_status}`} aria-label="答案">
      <span className={`badge badge-${response.answer_status}`}>
        <span aria-hidden="true">{badge.icon}</span> {badge.label}
      </span>
      <MarkdownView markdown={response.answer_markdown} />

      {response.citations.length > 0 && (
        <section className="answer-section">
          <h3 className="section-label">出处</h3>
          <ul className="link-list">
            {response.citations.map((c) => (
              <li key={`${c.file}#${c.heading}`}>
                <Link to={docHref(c.file, c.heading)}>{citationLabel(c)}</Link>
                <span className="muted file-hint"> {c.file}</span>
              </li>
            ))}
          </ul>
        </section>
      )}

      {response.related.length > 0 && (
        <section className="answer-section">
          <h3 className="section-label">你可能想看：</h3>
          <ul className="link-list">
            {response.related.map((r) => (
              <li key={r.file}>
                <Link to={docHref(r.file)}>{r.title || r.file}</Link>
              </li>
            ))}
          </ul>
        </section>
      )}

      {response.retrieval.length > 0 && (
        <details className="retrieval">
          <summary>检索摘要（命中 {response.retrieval.length} 个片段）</summary>
          <ul>
            {response.retrieval.map((h, i) => (
              <li key={`${h.file}#${h.heading}#${i}`}>
                <code>{h.file}</code> · {h.heading} <span className="muted">（{h.score.toFixed(2)}）</span>
              </li>
            ))}
          </ul>
        </details>
      )}
    </article>
  );
}
