import { Children, isValidElement, type ReactNode } from 'react';
import ReactMarkdown, { type Components } from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Link } from 'react-router-dom';
import { resolveKnowledgeLink, slugify } from '../links';

function textOf(node: ReactNode): string {
  return Children.toArray(node)
    .map((child) => {
      if (typeof child === 'string' || typeof child === 'number') return String(child);
      if (isValidElement<{ children?: ReactNode }>(child)) return textOf(child.props.children);
      return '';
    })
    .join('');
}

interface MarkdownViewProps {
  markdown: string;
  /** Path of the knowledge document being rendered; enables relative .md links and heading anchors. */
  docPath?: string;
}

/** Raw HTML in Markdown is not rendered (react-markdown default). */
export function MarkdownView({ markdown, docPath }: MarkdownViewProps) {
  const heading =
    (Tag: 'h1' | 'h2' | 'h3' | 'h4') =>
    ({ children }: { children?: ReactNode }) =>
      docPath ? <Tag id={slugify(textOf(children))}>{children}</Tag> : <Tag>{children}</Tag>;

  const components: Components = {
    h1: heading('h1'),
    h2: heading('h2'),
    h3: heading('h3'),
    h4: heading('h4'),
    a: ({ href, children }) => {
      if (!href) return <span>{children}</span>;
      const internal = docPath ? resolveKnowledgeLink(docPath, href) : null;
      if (internal) return <Link to={internal}>{children}</Link>;
      if (href.startsWith('#')) return <a href={href}>{children}</a>;
      return (
        <a href={href} target="_blank" rel="noopener noreferrer">
          {children}
        </a>
      );
    },
    img: ({ alt }) => <span className="muted">[图片：{alt ?? ''}]</span>,
  };

  return (
    <div className="markdown">
      <ReactMarkdown remarkPlugins={[remarkGfm]} components={components}>
        {markdown}
      </ReactMarkdown>
    </div>
  );
}
