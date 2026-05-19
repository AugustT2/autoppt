#!/usr/bin/env python3
"""Validate refreshed pptx chart embedding consistency (exit 1 on failure)."""
import re
import sys
import zipfile
import xml.etree.ElementTree as ET
from io import BytesIO

NS_C = {"c": "http://schemas.openxmlformats.org/drawingml/2006/chart"}
NS_M = {"m": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}


def load_shared_strings(xlsx_bytes: bytes) -> list[str]:
    z = zipfile.ZipFile(BytesIO(xlsx_bytes))
    if "xl/sharedStrings.xml" not in z.namelist():
        z.close()
        return []
    root = ET.fromstring(z.read("xl/sharedStrings.xml"))
    z.close()
    out = []
    for si in root.findall("m:si", NS_M):
        t = si.find(".//m:t", NS_M)
        out.append(t.text if t is not None and t.text else "")
    return out


def sheet_cell_map(xlsx_bytes: bytes, shared: list[str]) -> dict[str, str]:
    z = zipfile.ZipFile(BytesIO(xlsx_bytes))
    root = ET.fromstring(z.read("xl/worksheets/sheet1.xml"))
    z.close()
    cells = {}
    for c in root.findall(".//m:sheetData/m:row/m:c", NS_M):
        ref = c.get("r")
        v = c.find("m:v", NS_M)
        if v is None or v.text is None:
            continue
        if c.get("t") == "s" and shared:
            cells[ref] = shared[int(v.text)]
        else:
            cells[ref] = v.text
    return cells


def formula_span_rows(formula: str) -> int | None:
    m = re.search(r"\$(\w+)\$(\d+):\$(\w+)\$(\d+)", formula)
    if not m:
        return 1 if re.search(r"\$(\w+)\$(\d+)$", formula) else None
    return int(m.group(4)) - int(m.group(2)) + 1


def validate_chart(chart_xml: bytes, xlsx_bytes: bytes, chart_name: str) -> list[str]:
    issues = []
    shared = load_shared_strings(xlsx_bytes)
    cells = sheet_cell_map(xlsx_bytes, shared)
    root = ET.fromstring(chart_xml)

    for el in root.iter():
        tag = el.tag.split("}")[-1]
        if tag not in ("strRef", "numRef"):
            continue
        f_el = el.find("c:f", NS_C)
        if f_el is None or not f_el.text:
            continue
        formula = f_el.text
        cache = el.find("c:strCache", NS_C)
        if cache is None:
            cache = el.find("c:numCache", NS_C)
        if cache is None:
            continue
        pc_el = cache.find("c:ptCount", NS_C)
        pt_count = int(pc_el.get("val")) if pc_el is not None else -1
        pts = cache.findall("c:pt", NS_C)
        if pt_count != len(pts):
            issues.append(f"{chart_name} {tag} ptCount={pt_count} pts={len(pts)} f={formula}")

        span = formula_span_rows(formula)
        if span is not None and pt_count > 0 and span != pt_count:
            issues.append(
                f"{chart_name} {tag} formula rows={span} ptCount={pt_count} f={formula}"
            )

        # cache vs embedded sheet (strRef only, range refs)
        if tag == "strRef" and ":" in formula:
            m = re.search(r"\$A\$(\d+):\$A\$(\d+)", formula)
            if m:
                r1, r2 = int(m.group(1)), int(m.group(2))
                for i, pt in enumerate(pts):
                    row = r1 + i
                    cell_val = cells.get(f"A{row}")
                    cache_val = pt.find("c:v", NS_C)
                    cv = cache_val.text if cache_val is not None else None
                    if cell_val and cv and cell_val != cv:
                        issues.append(
                            f"{chart_name} A{row} sheet={cell_val!r} cache={cv!r}"
                        )
    return issues


def validate_allocation_orientation(chart_xml: bytes) -> list[str]:
    """Bar chart: first series title should be asset name, categories should be quarters."""
    issues = []
    root = ET.fromstring(chart_xml)
    first_tx = None
    first_cat = None
    for el in root.iter():
        tag = el.tag.split("}")[-1]
        if tag == "strRef":
            f = el.find("c:f", NS_C)
            cache = el.find("c:strCache", NS_C)
            if f is None or cache is None:
                continue
            pts = [p.find("c:v", NS_C).text for p in cache.findall("c:pt", NS_C)]
            if f.text and f.text.endswith("$1") and first_tx is None:
                first_tx = pts[0] if pts else None
            if f.text and ":$A$" in f.text and first_cat is None:
                first_cat = pts[0] if pts else None
    if first_tx and first_tx.startswith("2024"):
        issues.append(f"allocation: series title looks like quarter {first_tx!r}, expected asset name")
    if first_cat and not re.match(r"20\d{2}Q\d", first_cat or ""):
        issues.append(f"allocation: category looks like {first_cat!r}, expected quarter like 2024Q2")
    return issues


def main() -> int:
    pptx = sys.argv[1] if len(sys.argv) > 1 else "samples/20260430-偏债混-M1-refreshed.pptx"
    z = zipfile.ZipFile(pptx)
    issues = []
    if z.testzip() is not None:
        issues.append("zip corrupted")
    emb1 = z.read("ppt/embeddings/Microsoft_Excel_Sheet1.xlsx")
    emb2 = z.read("ppt/embeddings/Microsoft_Excel_Sheet2.xlsx")
    issues.extend(validate_chart(z.read("ppt/charts/chart1.xml"), emb1, "chart1"))
    issues.extend(validate_chart(z.read("ppt/charts/chart2.xml"), emb2, "chart2"))
    issues.extend(validate_allocation_orientation(z.read("ppt/charts/chart1.xml")))
    z.close()
    if issues:
        print("FAIL", pptx)
        for i in issues:
            print(" ", i)
        return 1
    print("OK", pptx)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
