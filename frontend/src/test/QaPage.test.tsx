import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { App } from '../App';
import type { AskResponse } from '../api';
import { mockApi, renderWithProviders } from './render';

const baseRoutes = {
  'GET /api/qa/status': { body: { llm_configured: true } },
  'GET /api/knowledge/toc': { body: { empty: false, items: [], others: [] } },
  'GET /api/index/status': {
    body: { state: 'READY', builtAt: null, documentCount: 3, chunkCount: 9, stale: false, lastError: null },
  },
};

const grounded: AskResponse = {
  answer_status: 'grounded',
  answer_markdown: '照片须为**白底**彩色。',
  citations: [{ file: 'korea-visa-application-documents.md', heading: '照片', title: '韩国签证材料', score: 3.1 }],
  related: [],
  retrieval: [{ file: 'korea-visa-application-documents.md', heading: '照片', score: 3.1 }],
};

describe('QaPage', () => {
  it('does not allow submitting an empty question', async () => {
    mockApi(baseRoutes);
    renderWithProviders(<App />);

    const send = screen.getByRole('button', { name: '发送' });
    expect(send).toBeDisabled();
    await userEvent.type(screen.getByLabelText('问题'), '   ');
    expect(send).toBeDisabled();
  });

  it('shows progress, clears input and renders a grounded answer with citation link', async () => {
    let release!: () => void;
    const gate = new Promise<void>((resolve) => (release = resolve));
    const fetchMock = mockApi({
      ...baseRoutes,
      'POST /api/qa/ask': async () => {
        await gate;
        return { body: grounded };
      },
    });
    renderWithProviders(<App />);

    const input = screen.getByLabelText('问题');
    await userEvent.type(input, '照片有什么要求？');
    await userEvent.click(screen.getByRole('button', { name: '发送' }));

    expect(input).toHaveValue('');
    expect(await screen.findByText('正在检索 / 正在生成')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '发送' })).toBeDisabled();

    release();
    expect(await screen.findByText('有依据')).toBeInTheDocument();
    expect(screen.getByText('白底')).toBeInTheDocument();
    const link = screen.getByRole('link', { name: '韩国签证材料 · 照片' });
    expect(link).toHaveAttribute('href', '/knowledge/korea-visa-application-documents.md#%E7%85%A7%E7%89%87');
    expect(screen.getByText(/检索摘要（命中 1 个片段）/)).toBeInTheDocument();

    const askCall = fetchMock.mock.calls.find(([, init]) => init?.method === 'POST');
    expect(JSON.parse(String(askCall?.[1]?.body))).toEqual({ question: '照片有什么要求？', history: [] });
  });

  it('sends previous turns as history and clears them on new session', async () => {
    const bodies: unknown[] = [];
    mockApi({
      ...baseRoutes,
      'POST /api/qa/ask': (_url, init) => {
        bodies.push(JSON.parse(String(init?.body)));
        return { body: grounded };
      },
    });
    renderWithProviders(<App />);
    const input = screen.getByLabelText('问题');

    await userEvent.type(input, '照片有什么要求？');
    await userEvent.click(screen.getByRole('button', { name: '发送' }));
    await screen.findByText('有依据');

    await userEvent.type(input, '这个要彩色吗{Control>}{Enter}{/Control}');
    await waitFor(() => expect(bodies).toHaveLength(2));
    expect(bodies[1]).toEqual({
      question: '这个要彩色吗',
      history: [{ question: '照片有什么要求？', answer: grounded.answer_markdown }],
    });

    await waitFor(() => expect(screen.getAllByText('有依据')).toHaveLength(2));
    await userEvent.click(screen.getByRole('button', { name: '新会话' }));
    expect(screen.queryByText('有依据')).not.toBeInTheDocument();
  });

  it('renders insufficient answers with related documents', async () => {
    mockApi({
      ...baseRoutes,
      'POST /api/qa/ask': {
        body: {
          answer_status: 'insufficient',
          answer_markdown: '当前知识库里没有足够依据回答这个问题。',
          citations: [],
          related: [{ file: 'how-to-ask.md', title: '如何提问才能得到可靠答案' }],
          retrieval: [],
        },
      },
    });
    renderWithProviders(<App />);

    await userEvent.type(screen.getByLabelText('问题'), '申请费多少？');
    await userEvent.click(screen.getByRole('button', { name: '发送' }));

    expect(await screen.findByText('依据不足')).toBeInTheDocument();
    expect(screen.getByText('你可能想看：')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '如何提问才能得到可靠答案' })).toHaveAttribute(
      'href',
      '/knowledge/how-to-ask.md',
    );
    expect(screen.queryByText('出处')).not.toBeInTheDocument();
  });

  it('shows configuration errors as a banner, not as an answer, and restores the question', async () => {
    mockApi({
      ...baseRoutes,
      'GET /api/qa/status': { body: { llm_configured: true } },
      'POST /api/qa/ask': {
        status: 503,
        body: { code: 'LLM_NOT_CONFIGURED', message: 'server text' },
      },
    });
    renderWithProviders(<App />);

    const input = screen.getByLabelText('问题');
    await userEvent.type(input, '照片有什么要求？');
    await userEvent.click(screen.getByRole('button', { name: '发送' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('未配置 LLM。请复制 .env.example 为 .env 并填写密钥。');
    expect(screen.queryByLabelText('答案')).not.toBeInTheDocument();
    expect(input).toHaveValue('照片有什么要求？');
  });

  it('warns up front when no LLM is configured and when knowledge is empty', async () => {
    mockApi({
      ...baseRoutes,
      'GET /api/qa/status': { body: { llm_configured: false } },
      'GET /api/knowledge/toc': { body: { empty: true, items: [], others: [] } },
    });
    renderWithProviders(<App />);

    expect(await screen.findByText('未配置 LLM。请复制 .env.example 为 .env 并填写密钥。')).toBeInTheDocument();
    expect(await screen.findByText('知识库还没有已发布的文档。请在 knowledge/ 添加 Markdown。')).toBeInTheDocument();
  });

  it('offers rebuild when the index is stale', async () => {
    let rebuilt = false;
    mockApi({
      ...baseRoutes,
      'GET /api/index/status': () => ({
        body: { state: 'READY', builtAt: null, documentCount: 3, chunkCount: 9, stale: !rebuilt, lastError: null },
      }),
      'POST /api/index/rebuild': () => {
        rebuilt = true;
        return { body: { state: 'READY', builtAt: null, documentCount: 3, chunkCount: 9, stale: false, lastError: null } };
      },
    });
    renderWithProviders(<App />);

    expect(await screen.findByText(/索引尚未重建/)).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: '重建索引' }));
    await waitFor(() => expect(screen.queryByText(/索引尚未重建/)).not.toBeInTheDocument());
  });
});
