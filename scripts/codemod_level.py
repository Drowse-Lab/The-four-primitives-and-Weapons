#!/usr/bin/env python3
import re
from pathlib import Path

root = Path('src')
java_files = list(root.rglob('*.java'))
changed = []
pattern = re.compile(r'([A-Za-z0-9_]+\.)level\b(?!\s*\()')
# Avoid replacing in import or package lines
for p in java_files:
    s = p.read_text(encoding='utf-8')
    original = s
    # skip import/package lines by operating on whole file but pattern won't match those
    s2 = pattern.sub(r"\1level()", s)
    if s2 != original:
        bak = p.with_suffix(p.suffix + '.bak')
        bak.write_text(original, encoding='utf-8')
        p.write_text(s2, encoding='utf-8')
        changed.append(str(p))

print('Modified %d files' % len(changed))
for f in changed:
    print(f)
