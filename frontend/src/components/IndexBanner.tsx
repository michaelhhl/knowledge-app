import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, ApiError } from '../api';
import { Banner } from './Banner';

export function useRebuildIndex() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: api.rebuildIndex,
    onSettled: () => queryClient.invalidateQueries(),
  });
}

/** FR-06 and the "stale index" anti-scenario: warn when knowledge changed after the last build. */
export function IndexBanner() {
  const status = useQuery({ queryKey: ['index-status'], queryFn: api.indexStatus, refetchInterval: 30_000 });
  const rebuild = useRebuildIndex();

  const rebuildButton = (
    <button type="button" className="btn btn-small" onClick={() => rebuild.mutate()} disabled={rebuild.isPending}>
      {rebuild.isPending ? '正在重建…' : '重建索引'}
    </button>
  );

  if (rebuild.error) {
    return <Banner action={rebuildButton}>{(rebuild.error as ApiError).message}</Banner>;
  }
  if (status.error) {
    return <Banner>{(status.error as ApiError).message}</Banner>;
  }
  const data = status.data;
  if (!data) {
    return null;
  }
  if (data.state === 'FAILED' || data.state === 'NOT_READY') {
    return <Banner action={rebuildButton}>{data.lastError ?? '索引尚未建立，请先重建索引。'}</Banner>;
  }
  if (data.stale) {
    return (
      <Banner tone="warning" action={rebuildButton}>
        知识库文件已变更，索引尚未重建，答案可能引用旧内容。
      </Banner>
    );
  }
  return null;
}
