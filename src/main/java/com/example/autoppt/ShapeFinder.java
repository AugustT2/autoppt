package com.example.autoppt;

import org.apache.poi.xslf.usermodel.XSLFGroupShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;

/** 按选择窗格中的名称查找形状（含一级组合内子形状）。 */
public final class ShapeFinder {

    private ShapeFinder() {}

    public static XSLFShape find(XSLFSlide slide, String shapeName) {
        for (XSLFShape shape : slide.getShapes()) {
            XSLFShape hit = find(shape, shapeName);
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    private static XSLFShape find(XSLFShape shape, String shapeName) {
        if (shapeName.equals(shape.getShapeName())) {
            return shape;
        }
        if (shape instanceof XSLFGroupShape) {
            for (XSLFShape child : ((XSLFGroupShape) shape).getShapes()) {
                XSLFShape hit = find(child, shapeName);
                if (hit != null) {
                    return hit;
                }
            }
        }
        return null;
    }
}
