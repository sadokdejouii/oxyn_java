package org.example.totp;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.image.BufferedImage;

public final class QrCode {

    private QrCode() {
    }

    public static Image toFxImage(String text, int size) throws WriterException {
        int s = Math.max(160, size);
        QRCodeWriter w = new QRCodeWriter();
        BitMatrix m = w.encode(text, BarcodeFormat.QR_CODE, s, s);
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                img.setRGB(x, y, m.get(x, y) ? 0x000000 : 0xFFFFFF);
            }
        }
        return SwingFXUtils.toFXImage(img, null);
    }
}

