# -*- coding: utf-8 -*-
"""
补丁样例 PPT 下方折线图。优先调用 POI（与 ChartDataWriter 一致，避免手写 XML 损坏文件）：
  mvn -q compile exec:java -Dexec.mainClass=com.example.pptrefresh.sample.PatchNavChartSample
"""
from __future__ import annotations

import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def main() -> None:
    mvn = "mvn.cmd" if sys.platform == "win32" else "mvn"
    cmd = [
        mvn,
        "-q",
        "compile",
        "exec:java",
        "-Dexec.mainClass=com.example.pptrefresh.sample.PatchNavChartSample",
    ]
    print("运行:", " ".join(cmd))
    subprocess.run(cmd, cwd=ROOT, check=True)


if __name__ == "__main__":
    main()
