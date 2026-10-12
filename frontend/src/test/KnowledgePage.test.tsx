import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { App } from '../App';
import { mockApi, renderWithProviders } from './render';

const indexStatus = {
  body: { state: 'READY', builtAt: null, documentCount: 2, chunkCount: 4, stale: false, lastError: null },
};

const toc = {
  body: {
    empty: false,
    items: [{ title: '产品是什么', path: 'what-is-this-app.md' }],
    others: [{ title: 'VPN 连接', path: 'it/vpn.md' }],
  },
};

describe('KnowledgePage', () => {
  it('lists the table of contents and renders the selected document with heading anchors', async () => {
    mockApi({
      'GET /api/index/status': indexStatus,
      'GET /api/knowledge/toc': toc,
      'GET /api/knowledge/doc': (url) => {
        const path = new URL(url, 'http://x').searchParams.get('path');
        return {
          body: {
            path,
            id: 'kb-vpn',
            title: 'VPN 连接',
            tags: ['it'],
            updated: '2026-10-04',
            markdown: '# VPN 连接\n\n## 安装客户端\n\n见 [产品介绍](../what-is-this-app.md)。\n\n<script>alert(1)</script>',
          },
        };
      },
    });
    const { container } = renderWithProviders(<App />, '/knowledge/it/vpn.md');

    expect(await screen.findByRole('heading', { level: 2, name: '安装客户端' })).toHaveAttribute('id', '安装客户端');
    expect(screen.getByRole('link', { name: '产品是什么' })).toHaveAttribute('href', '/knowledge/what-is-this-app.md');
    expect(screen.getByText('其他已发布文档')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'VPN 连接' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('link', { name: '产品介绍' })).toHaveAttribute('href', '/knowledge/what-is-this-app.md');
    expect(screen.getByText(/更新于 2026-10-04/)).toBeInTheDocument();
    expect(container.querySelector('script')).toBeNull();
  });

  it('opens the first table-of-contents entry by default', async () => {
    mockApi({
      'GET /api/index/status': indexStatus,
      'GET /api/knowledge/toc': toc,
      'GET /api/knowledge/doc': {
        body: { path: 'what-is-this-app.md', id: 'a', title: '产品是什么', tags: [], updated: null, markdown: '# 产品是什么\n\n## 一句话\n\n问答应用。' },
      },
    });
    renderWithProviders(<App />, '/knowledge');

    expect(await screen.findByRole('heading', { name: '一句话' })).toBeInTheDocument();
  });

  it('shows the empty-knowledge message', async () => {
    mockApi({
      'GET /api/index/status': indexStatus,
      'GET /api/knowledge/toc': { body: { empty: true, items: [], others: [] } },
    });
    renderWithProviders(<App />, '/knowledge');

    expect(await screen.findByText('知识库还没有已发布的文档。请在 knowledge/ 添加 Markdown。')).toBeInTheDocument();
  });

  it('shows document errors as a banner', async () => {
    mockApi({
      'GET /api/index/status': indexStatus,
      'GET /api/knowledge/toc': toc,
      'GET /api/knowledge/doc': { status: 404, body: { code: 'DOC_NOT_FOUND', message: '文档不存在或未发布。' } },
    });
    renderWithProviders(<App />, '/knowledge/missing.md');

    expect(await screen.findByRole('alert')).toHaveTextContent('文档不存在或未发布。');
  });
});
