package org.example.avatar;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;

import static org.bytedeco.opencv.global.opencv_core.addWeighted;
import static org.bytedeco.opencv.global.opencv_core.bitwise_and;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imencode;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

/**
 * Génère un avatar stylisé à partir d'une image visage BGR (crop).
 * Objectif: un rendu "cartoon" simple, local, sans API.
 */
public final class AvatarGenerator {

    private AvatarGenerator() {
    }

    public static byte[] generateAvatarPng(Mat faceBgr) {
        if (faceBgr == null || faceBgr.empty()) {
            throw new IllegalArgumentException("Image visage vide.");
        }

        Mat src = new Mat();
        resize(faceBgr, src, new Size(256, 256));

        // 1) Lissage + simplification couleurs
        Mat smooth = new Mat();
        bilateralFilter(src, smooth, 9, 75, 75);

        // 2) Détection contours
        Mat gray = new Mat();
        cvtColor(smooth, gray, COLOR_BGR2GRAY);
        medianBlur(gray, gray, 7);

        Mat edges = new Mat();
        adaptiveThreshold(gray, edges, 255,
                ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 9, 2);
        cvtColor(edges, edges, COLOR_GRAY2BGR);

        // 3) Combine: couleurs + contours
        Mat cartoon = new Mat();
        bitwise_and(smooth, edges, cartoon);

        // 4) Petite touche "warm"
        Mat warm = new Mat(cartoon.size(), cartoon.type(), new Scalar(8, 4, 0, 0));
        Mat out = new Mat();
        addWeighted(cartoon, 1.0, warm, 0.15, 0.0, out);

        return encodePng(out);
    }

    private static byte[] encodePng(Mat bgr) {
        try (BytePointer buf = new BytePointer()) {
            boolean ok = imencode(".png", bgr, buf);
            if (!ok || buf.isNull()) {
                throw new IllegalStateException("Impossible d'encoder l'avatar en PNG.");
            }
            long n = buf.limit();
            if (n <= 0) {
                // certains builds utilisent capacity() au lieu de limit()
                n = buf.capacity();
            }
            if (n <= 0) {
                throw new IllegalStateException("Impossible d'encoder l'avatar en PNG (buffer vide).");
            }
            byte[] bytes = new byte[(int) n];
            buf.get(bytes);
            return bytes;
        }
    }
}

