import { useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Navigate, NavLink, useLocation, useParams } from 'react-router-dom';
import { api, ApiError, type TocItem } from '../api';
import { docHref } from '../links';
import { Banner } from '../components/Banner';
import { MarkdownView } from '../components/MarkdownView';
import { useRebuildIndex } from '../components/IndexBanner';

function TocList({ items, current }: { items: TocItem[]; current: string }) {
  return (
    <ul className="toc-list">
      {items.map((item) => (
        <li key={item.path}>
          <NavLink to={docHref(item.path)} aria-current={item.path === current ? 'page' : undefined}>
            {item.title}
          </NavLink>
        </li>
      ))}
    </ul>
  );
}

export function KnowledgePage() {
  const params = useParams();
  const location = useLocation();
  const currentPath = params['*'] ? decodeURIComponent(params['*']) : '';

  const toc = useQuery({ queryKey: ['toc'], queryFn: api.toc });
  const doc = useQuery({
    queryKey: ['doc', currentPath],
    queryFn: () => api.doc(currentPath),
    enabled: currentPath !== '',
  });
  const rebuild = useRebuildIndex();

  useEffect(() => {
    if (!doc.data || !location.hash) return;
    const id = decodeURIComponent(location.hash.slice(1));
    document.getElementById(id)?.scrollIntoView?.({ block: 'start' });
  }, [doc.data, location.hash]);

  if (!currentPath && toc.data) {
    const first = toc.data.items[0] ?? toc.data.others[0];
    if (first) return <Navigate to={docHref(first.path)} replace />;
  }

  return (
    <section className="knowledge-page" aria-label="知识库">
      <aside className="toc" aria-label="知识目录">
        <div className="toc-header">
          <h2 className="section-label">目录</h2>
          <button
            type="button"
            className="btn btn-small"
            onClick={() => rebuild.mutate()}
            disabled={rebuild.isPending}
          >
            {rebuild.isPending ? '正在重建…' : '重建索引'}
          </button>
        </div>
        {toc.data && (
          <>
            <TocList items={toc.data.items} current={currentPath} />
            {toc.data.others.length > 0 && (
              <>
                <h2 className="section-label toc-subhead">其他已发布文档</h2>
                <TocList items={toc.data.others} current={currentPath} />
              </>
            )}
          </>
        )}
      </aside>

      <div className="article">
        {toc.error && <Banner>{(toc.error as ApiError).message}</Banner>}
        {toc.data?.empty && <p className="empty-hint">知识库还没有已发布的文档。请在 knowledge/ 添加 Markdown。</p>}
        {doc.isLoading && <p className="muted">正在加载…</p>}
        {doc.error && <Banner>{(doc.error as ApiError).message}</Banner>}
        {doc.data && (
          <>
            <p className="doc-meta muted">
              <code>{doc.data.path}</code>
              {doc.data.updated && <> · 更新于 {doc.data.updated}</>}
              {doc.data.tags.length > 0 && <> · 标签：{doc.data.tags.join('、')}</>}
            </p>
            <MarkdownView markdown={doc.data.markdown} docPath={doc.data.path} />
          </>
        )}
      </div>
    </section>
  );
}
