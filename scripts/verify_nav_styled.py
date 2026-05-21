from pathlib import Path
import zipfile
import re

samples = Path(__file__).resolve().parent.parent / "samples"
ppt = next(samples.glob("*M1.pptx"))
if "backup" in ppt.name or "llm" in ppt.name:
    ppt = next(samples.glob("*nav-styled*"), ppt)
slide = zipfile.ZipFile(ppt).read("ppt/slides/slide1.xml").decode("utf-8")
chart = zipfile.ZipFile(ppt).read("ppt/charts/chart2.xml").decode("utf-8")
print("file:", ppt.name)
print("title removed:", "累计收益率走势" not in slide)
m = re.search(r'autoTitleDeleted val="(\d)"', chart)
print("autoTitleDeleted:", m.group(1) if m else "?")
m2 = re.search(r"<c:valAx>.*?formatCode=\"([^\"]+)\"", chart, re.DOTALL)
print("val axis format:", m2.group(1) if m2 else "?")
