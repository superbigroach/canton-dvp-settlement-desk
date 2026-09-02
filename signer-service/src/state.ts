import fs from 'node:fs';
import path from 'node:path';

/**
 * Idempotency: one record per proposal (keyed by its root cid - the cid changes every time
 * another member confirms, the root does not). Written atomically; survives restarts.
 */
export interface ActionRecord {
  cid: string;
  instrument: string;
  decision: 'confirm' | 'refuse' | 'already-confirmed' | 'already-refused' | 'rejected';
  at: string;
  httpStatus?: number;
  detail?: string;
}

export interface StateFile {
  version: 1;
  acted: Record<string, ActionRecord>;
}

export class State {
  private data: StateFile = { version: 1, acted: {} };

  constructor(readonly file: string) {
    this.load();
  }

  private load(): void {
    if (!fs.existsSync(this.file)) return;
    const raw = fs.readFileSync(this.file, 'utf8');
    if (!raw.trim()) return;
    const parsed = JSON.parse(raw) as Partial<StateFile>;
    if (parsed.version !== 1 || typeof parsed.acted !== 'object' || parsed.acted === null) {
      throw new Error(`state file ${this.file} has an unknown shape; refusing to guess`);
    }
    this.data = { version: 1, acted: { ...parsed.acted } };
  }

  get(key: string): ActionRecord | undefined {
    return this.data.acted[key];
  }

  has(key: string): boolean {
    return key in this.data.acted;
  }

  size(): number {
    return Object.keys(this.data.acted).length;
  }

  record(key: string, rec: ActionRecord): void {
    this.data.acted[key] = rec;
    this.flush();
  }

  private flush(): void {
    const dir = path.dirname(this.file);
    fs.mkdirSync(dir, { recursive: true });
    const tmp = `${this.file}.${process.pid}.tmp`;
    fs.writeFileSync(tmp, JSON.stringify(this.data, null, 2) + '\n', { mode: 0o600 });
    fs.renameSync(tmp, this.file);
  }
}
