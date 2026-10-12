/** Heading anchor id shared by the Markdown renderer and citation links. */
export function slugify(text: string): string {
  return text
    .trim()
    .toLowerCase()
    .replace(/\s+/g, '-')
    .replace(/[^\p{L}\p{N}\-_]/gu, '');
}

/** Route for a knowledge document, optionally scrolled to a heading. */
export function docHref(path: string, heading?: string): string {
  const encoded = path.split('/').map(encodeURIComponent).join('/');
  return `/knowledge/${encoded}${heading ? `#${encodeURIComponent(slugify(heading))}` : ''}`;
}

/**
 * Resolves a relative Markdown link inside a knowledge document to a knowledge route.
 * Returns null for external links or links that would leave the knowledge root.
 */
export function resolveKnowledgeLink(currentPath: string, href: string): string | null {
  if (/^[a-z][a-z0-9+.-]*:/i.test(href) || href.startsWith('//') || href.startsWith('/')) {
    return null;
  }
  const [target, hash] = href.split('#', 2);
  if (!target.endsWith('.md')) {
    return null;
  }
  const segments = currentPath.split('/').slice(0, -1);
  for (const part of target.split('/')) {
    if (part === '' || part === '.') continue;
    if (part === '..') {
      if (segments.length === 0) return null;
      segments.pop();
    } else {
      segments.push(part);
    }
  }
  const encoded = segments.map(encodeURIComponent).join('/');
  return `/knowledge/${encoded}${hash ? `#${hash}` : ''}`;
}
