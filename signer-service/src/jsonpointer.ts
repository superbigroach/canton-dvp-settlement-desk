/** RFC 6901 JSON pointer lookup. "" (or undefined) returns the document itself. */
export function resolvePointer(doc: unknown, pointer: string | undefined): unknown {
  if (pointer === undefined || pointer === '') return doc;
  if (!pointer.startsWith('/')) {
    throw new Error(`JSON pointer must start with '/': ${pointer}`);
  }
  let cur: unknown = doc;
  for (const raw of pointer.slice(1).split('/')) {
    const key = raw.replace(/~1/g, '/').replace(/~0/g, '~');
    if (Array.isArray(cur)) {
      const idx = key === '-' ? cur.length - 1 : Number(key);
      if (!Number.isInteger(idx) || idx < 0 || idx >= cur.length) {
        throw new Error(`pointer ${pointer}: index '${key}' out of range`);
      }
      cur = cur[idx];
    } else if (cur !== null && typeof cur === 'object') {
      if (!(key in (cur as Record<string, unknown>))) {
        throw new Error(`pointer ${pointer}: no key '${key}'`);
      }
      cur = (cur as Record<string, unknown>)[key];
    } else {
      throw new Error(`pointer ${pointer}: cannot descend into ${typeof cur} at '${key}'`);
    }
  }
  return cur;
}
