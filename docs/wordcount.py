"""Count the words actually SPOKEN in the pitch script.

Every '>' blockquote line inside a numbered section is spoken aloud and nothing
else is, so this is a real timing check rather than a guess. Q&A is excluded --
it is answered on demand, not delivered.

    python docs/wordcount.py
"""
import io
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
PATH = os.path.join(HERE, "GRAND_FINAL_PITCH_SCRIPT.md")

text = io.open(PATH, encoding="utf-8", newline="").read().split("\n## Q&A")[0]

section = None
counts = {}
order = []
for line in text.split("\n"):
    m = re.match(r"^## (§\d+ · [^—]+)", line)
    if m:
        section = m.group(1).strip()
        if section not in counts:
            counts[section] = 0
            order.append(section)
    elif line.startswith("##"):
        section = None
    if line.startswith(">") and section:
        words = re.sub(r"[*_`>—–·]", " ", line).split()
        counts[section] += len(words)

WPM = 145.0
total = sum(counts.values())

print("")
for s in order:
    n = counts[s]
    flag = "  <-- OVER 60s" if n / WPM * 60 > 62 else ""
    print("  %-28s %4d words  %4.0f sec%s" % (s, n, n / WPM * 60, flag))
print("  " + "-" * 52)
print("  %-28s %4d words  %d:%02d" % ("TOTAL", total, total // WPM, round((total / WPM % 1) * 60)))

slack = 240 - total / WPM * 60
verdict = "OK" if slack >= 0 else "OVER THE 4-MINUTE LIMIT"
print("  slack against 4:00 ....... %+.0f sec  %s" % (slack, verdict))
print("  (at a nervous 160 wpm you finish %d sec early)" % (240 - total / 160.0 * 60))
print("")
