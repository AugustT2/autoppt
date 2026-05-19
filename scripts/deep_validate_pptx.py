#!/usr/bin/env python3
"""Deep OOXML checks for pptx corruption indicators."""
import re
import sys
import zipfile
import xml.etree.ElementTree as ET
from io import BytesIO

NS = {
    "c": "http://schemas.openxmlformats.org/drawingml/2006/chart",
    "a": "http://schemas.openxmlformats.org/drawingml/2006/main",
    "p": "http://schemas.openxmlformats.org/presentationml/2006/main",
    "r": "http://schemas.openxmlformats.org/officeDocument/2006/relationships",
}


def issues_for(pptx: str) -> list[str]:
    out = []
    z = zipfile.ZipFile(pptx)
    bad = z.testzip()
    if bad:
        out.append(f"zip corrupt entry: {bad}")

    # [Content_Types].xml
    ct = z.read("[Content_Types].xml").decode("utf-8")
    names = set(z.namelist())

    # relationship targets exist
    for rel_path in names:
        if not rel_path.endswith(".rels"):
            continue
        base = rel_path.rsplit("/_rels/", 1)[0] + "/"
        if "/_rels/" not in rel_path:
            continue
        root = ET.fromstring(z.read(rel_path))
        for rel in root:
            tid = rel.get("Id")
            target = rel.get("Target")
            typ = rel.get("Type", "")
            if not target or target.startswith("http"):
                continue
            if target.startswith("/"):
                resolved = target.lstrip("/")
            else:
                resolved = (base + target).replace("\\", "/")
                while "/../" in resolved:
                    parts = resolved.split("/")
                    np = []
                    for p in parts:
                        if p == ".." and np:
                            np.pop()
                        elif p != "..":
                            np.append(p)
                    resolved = "/".join(np)
            if resolved not in names and not resolved.startswith(".."):
                out.append(f"broken rel {rel_path} {tid} -> {target} ({resolved})")

    # chart externalData + embedding
    for chart in sorted(n for n in names if re.match(r"ppt/charts/chart\d+\.xml", n)):
        xml = z.read(chart).decode("utf-8")
        if "externalData" not in xml:
            out.append(f"{chart}: missing externalData")
        # numRef without cache
        for m in re.finditer(r"<c:numRef>(.*?)</c:numRef>", xml, re.S):
            block = m.group(1)
            if "<c:numCache>" not in block and "<c:numLit>" not in block:
                out.append(f"{chart}: numRef without cache/lit")

    # slide duplicate ids
    slide = z.read("ppt/slides/slide1.xml")
    root = ET.fromstring(slide)
    ids = []
    for el in root.iter():
        for k, v in el.attrib.items():
            if k.endswith("id}") and "id" in k.lower():
                ids.append(v)
    dup = {x for x in ids if ids.count(x) > 1}
    if dup:
        out.append(f"slide1 duplicate shape ids: {list(dup)[:5]}")

    # txBody: r without t
    for p_el in root.iter():
        if not p_el.tag.endswith("}p"):
            continue
        runs = [c for c in p_el if c.tag.endswith("}r")]
        for r_el in runs:
            t = r_el.find("a:t", NS)
            if t is None:
                out.append("slide1: a:r without a:t")

    z.close()
    return out


def compare_sizes(a: str, b: str):
    za, zb = zipfile.ZipFile(a), zipfile.ZipFile(b)
    for n in sorted(set(za.namelist()) | set(zb.namelist())):
        sa = len(za.read(n)) if n in za.namelist() else -1
        sb = len(zb.read(n)) if n in zb.namelist() else -1
        if sa != sb and ("chart" in n or "embed" in n or "slide1" in n):
            print(f"  size diff {n}: {sa} -> {sb}")
    za.close()
    zb.close()


if __name__ == "__main__":
    src = "samples/20260430-偏债混-M1.pptx"
    out = sys.argv[1] if len(sys.argv) > 1 else "samples/20260430-偏债混-M1-refreshed.pptx"
    print("===", out, "===")
    iss = issues_for(out)
    print("issues:", iss or "none")
    print("=== compare to source ===")
    compare_sizes(src, out)
