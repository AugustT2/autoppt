from pathlib import Path
import zipfile
import re

samples = Path(__file__).resolve().parent.parent / "samples"
for name in ["20260430-偏债混-M1-nav-styled.pptx", "20260430-偏债混-M1.pptx"]:
    ppt = samples / name
    if not ppt.exists():
        continue
    x = zipfile.ZipFile(ppt).read("ppt/charts/chart2.xml").decode("utf-8")
    rot = re.search(r'rot="(-?\d+)"', x)
    sz = re.search(r'defRPr sz="(\d+)"', x)
    no_multi = re.search(r'noMultiLvlLbl val="(\d)"', x)
    m = re.search(r"<c:cat>.*?<c:strCache>.*?</c:strCache>", x, re.DOTALL)
    ds = re.findall(r"<c:v>([^<]+)</c:v>", m.group()) if m else []
    print("file:", name)
    print("  rot:", rot.group(1) if rot else "-")
    print("  font sz:", sz.group(1) if sz else "-")
    print("  noMultiLvlLbl:", no_multi.group(1) if no_multi else "-")
    print("  dates:", ds[:2], "...", ds[-1] if ds else "-")
