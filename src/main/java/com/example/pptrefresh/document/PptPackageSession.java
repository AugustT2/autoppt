package com.example.pptrefresh.document;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.xslf.usermodel.XMLSlideShow;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 在副本上就地打开 OPC 包，避免 {@link XMLSlideShow#write(java.io.OutputStream)} 整包重打包破坏图表嵌入。
 */
public final class PptPackageSession implements AutoCloseable {

    private final OPCPackage pkg;
    private final XMLSlideShow slideShow;

    private PptPackageSession(OPCPackage pkg, XMLSlideShow slideShow) {
        this.pkg = pkg;
        this.slideShow = slideShow;
    }

    public static PptPackageSession open(Path pptxFile) throws IOException, InvalidFormatException {
        OPCPackage pkg = OPCPackage.open(pptxFile.toFile(), PackageAccess.READ_WRITE);
        return new PptPackageSession(pkg, new XMLSlideShow(pkg));
    }

    public XMLSlideShow slideShow() {
        return slideShow;
    }

    @Override
    public void close() throws IOException {
        IOException slideCloseError = null;
        try {
            slideShow.close();
        } catch (IOException e) {
            slideCloseError = e;
        }
        try {
            pkg.close();
        } catch (IOException e) {
            if (slideCloseError != null) {
                e.addSuppressed(slideCloseError);
            }
            throw e;
        }
        if (slideCloseError != null) {
            throw slideCloseError;
        }
    }
}
