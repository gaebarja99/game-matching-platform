/**
 * Remove git conflict markers from a file, keeping the "theirs" block (after =======).
 * Usage: node scripts/resolve-json-merge-conflicts.mjs <file> [...]
 */
import fs from 'fs';
import path from 'path';

const pat =
  /^<<<<<<<[^\n]*\r?\n[\s\S]*?^=======\r?\n([\s\S]*?)^>>>>>>>[^\n]*\r?\n/gm;

for (const rel of process.argv.slice(2)) {
  const p = path.resolve(rel);
  let t = fs.readFileSync(p, 'utf8');
  if (!t.includes('<<<<<<<')) {
    console.log('skip (no conflicts):', rel);
    continue;
  }
  const newT = t.replace(pat, '$1');
  const n = (t.match(/^<<<<<<</gm) || []).length;
  fs.writeFileSync(p, newT, 'utf8');
  console.log('resolved:', rel, 'blocks:', n);
}
