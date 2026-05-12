package com.example.autoppt;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.openxmlformats.schemas.presentationml.x2006.main.CTPresentation;
import org.openxmlformats.schemas.presentationml.x2006.main.CTSlideSize;
import org.openxmlformats.schemas.presentationml.x2006.main.STSlideSizeType;

/**
 * 宽屏 cx/cy 却标记为 {@code screen4x3} 时，部分 PowerPoint 会拒绝打开。改为 {@code screen16x9}。
 */
public final class PresentationSlideSizeFix {

    private PresentationSlideSizeFix() {}

    public static void fixWideScreenType(XMLSlideShow ppt) {
        CTPresentation cp = ppt.getCTPresentation();
        if (cp == null || !cp.isSetSldSz()) {
            return;
        }
        CTSlideSize sz = cp.getSldSz();
        long cx = sz.getCx();
        long cy = sz.getCy();
        if (cy <= 0) {
            return;
        }
        double ratio = (double) cx / (double) cy;
        if (ratio > 1.5) {
            sz.setType(STSlideSizeType.Enum.forString("screen16x9"));
        }
    }
}
