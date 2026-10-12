import { describe, expect, it } from 'vitest';
import { docHref, resolveKnowledgeLink, slugify } from '../links';

describe('links', () => {
  it('slugifies Chinese and mixed headings', () => {
    expect(slugify('照片')).toBe('照片');
    expect(slugify('可确认工资入账的材料（最近 6 个月）')).toBe('可确认工资入账的材料最近-6-个月');
    expect(slugify(' Hello World! ')).toBe('hello-world');
  });

  it('builds document routes with encoded heading anchors', () => {
    expect(docHref('it/vpn.md')).toBe('/knowledge/it/vpn.md');
    expect(docHref('a b.md', '安装 客户端')).toBe('/knowledge/a%20b.md#%E5%AE%89%E8%A3%85-%E5%AE%A2%E6%88%B7%E7%AB%AF');
  });

  it('resolves relative Markdown links inside the knowledge root only', () => {
    expect(resolveKnowledgeLink('it/vpn.md', '../how-to-ask.md')).toBe('/knowledge/how-to-ask.md');
    expect(resolveKnowledgeLink('it/vpn.md', './setup.md#步骤')).toBe('/knowledge/it/setup.md#步骤');
    expect(resolveKnowledgeLink('vpn.md', '../../etc/passwd.md')).toBeNull();
    expect(resolveKnowledgeLink('vpn.md', 'https://example.com/a.md')).toBeNull();
    expect(resolveKnowledgeLink('vpn.md', 'image.png')).toBeNull();
  });
});
