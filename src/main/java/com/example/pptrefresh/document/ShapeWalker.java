package com.example.pptrefresh.document;

import org.apache.poi.xslf.usermodel.XSLFGroupShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class ShapeWalker {

    private ShapeWalker() {}

    public static void walkDepthFirst(XSLFSlide slide, Consumer<XSLFShape> visitor) {
        for (XSLFShape shape : slide.getShapes()) {
            visit(shape, visitor);
        }
    }

    private static void visit(XSLFShape shape, Consumer<XSLFShape> visitor) {
        visitor.accept(shape);
        if (shape instanceof XSLFGroupShape) {
            XSLFGroupShape group = (XSLFGroupShape) shape;
            for (XSLFShape child : group.getShapes()) {
                visit(child, visitor);
            }
        }
    }

    public static List<XSLFShape> collectDepthFirst(XSLFSlide slide) {
        List<XSLFShape> out = new ArrayList<>();
        walkDepthFirst(slide, out::add);
        return out;
    }
}
