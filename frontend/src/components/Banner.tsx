import type { ReactNode } from 'react';

interface BannerProps {
  tone?: 'error' | 'warning';
  children: ReactNode;
  action?: ReactNode;
}

/** Errors are shown as banners, never as fake assistant replies (07-ui-ux.md). */
export function Banner({ tone = 'error', children, action }: BannerProps) {
  return (
    <div className={`banner banner-${tone}`} role="alert">
      <span className="banner-icon" aria-hidden="true">
        {tone === 'error' ? '⚠' : 'ℹ'}
      </span>
      <span className="banner-text">{children}</span>
      {action}
    </div>
  );
}
