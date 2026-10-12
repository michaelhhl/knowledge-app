import type { ReactElement } from 'react';
import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { vi } from 'vitest';

type Handler = (url: string, init?: RequestInit) => { status?: number; body: unknown } | Promise<{ status?: number; body: unknown }>;

/** Routes fetch calls by "METHOD path" (query string ignored). */
export function mockApi(routes: Record<string, Handler | { status?: number; body: unknown }>) {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    const key = `${init?.method ?? 'GET'} ${url.split('?')[0]}`;
    const route = routes[key];
    if (!route) {
      return new Response(JSON.stringify({ code: 'NOT_FOUND', message: `no mock for ${key}` }), { status: 404 });
    }
    const result = typeof route === 'function' ? await route(url, init) : route;
    return new Response(JSON.stringify(result.body), {
      status: result.status ?? 200,
      headers: { 'Content-Type': 'application/json' },
    });
  });
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

export function renderWithProviders(ui: ReactElement, path = '/') {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[path]}>{ui}</MemoryRouter>
    </QueryClientProvider>,
  );
}
