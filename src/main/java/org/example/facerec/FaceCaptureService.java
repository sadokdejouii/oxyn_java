package org.example.facerec;

import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;

/**
 * Capture webcam + détection visage (cascade) + crop en 96x96 BGR.
 *
     * Ressource :
     * - d'abord /opencv/haarcascade_frontalface_default.xml (si fournie par l'app)
     * - sinon fallback sur la ressource packagée par JavaCV/OpenCV (org/bytedeco/opencv/data/haarcascades/...)
 */
public final class FaceCaptureService {

    private static final OpenCVFrameConverter.ToMat CONVERTER = new OpenCVFrameConverter.ToMat();
    private static volatile CascadeClassifier cascade;
    private static volatile Path extractedCascadePath;

    private FaceCaptureService() {
    }

    public static Mat captureSingleFace96x96Bgr(int cameraIndex, Duration timeout) throws Exception {
        if (timeout == null) {
            timeout = Duration.ofSeconds(12);
        }
        CascadeClassifier cc = ensureCascadeLoaded();

        try (OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(cameraIndex)) {
            grabber.start();
            Instant end = Instant.now().plus(timeout);
            while (Instant.now().isBefore(end)) {
                Frame f = grabber.grab();
                if (f == null) {
                    continue;
                }
                Mat bgr = CONVERTER.convert(f);
                if (bgr == null || bgr.empty()) {
                    continue;
                }

                Mat gray = new Mat();
                cvtColor(bgr, gray, COLOR_BGR2GRAY);

                RectVector faces = new RectVector();
                cc.detectMultiScale(gray, faces);
                if (faces.size() <= 0) {
                    continue;
                }

                Rect r = largestRect(faces);
                Mat face = new Mat(bgr, r).clone();
                Mat face96 = new Mat();
                resize(face, face96, new org.bytedeco.opencv.opencv_core.Size(96, 96));
                return face96;
            }
            throw new IOException("Aucun visage détecté (timeout).");
        }
    }

    private static Rect largestRect(RectVector v) {
        Rect best = v.get(0);
        long bestArea = (long) best.width() * best.height();
        for (long i = 1; i < v.size(); i++) {
            Rect r = v.get(i);
            long area = (long) r.width() * r.height();
            if (area > bestArea) {
                best = r;
                bestArea = area;
            }
        }
        return best;
    }

    private static CascadeClassifier ensureCascadeLoaded() throws IOException {
        CascadeClassifier cached = cascade;
        if (cached != null) {
            return cached;
        }
        synchronized (FaceCaptureService.class) {
            cached = cascade;
            if (cached != null) {
                return cached;
            }
            Path xml = ensureCascadeExtracted();
            CascadeClassifier cc = new CascadeClassifier(xml.toAbsolutePath().toString());
            if (cc.empty()) {
                throw new IOException("Impossible de charger la cascade Haar (fichier invalide).");
            }
            cascade = cc;
            return cc;
        }
    }

    private static Path ensureCascadeExtracted() throws IOException {
        Path cached = extractedCascadePath;
        if (cached != null && Files.exists(cached)) {
            return cached;
        }
        synchronized (FaceCaptureService.class) {
            cached = extractedCascadePath;
            if (cached != null && Files.exists(cached)) {
                return cached;
            }
            InputStream in = FaceCaptureService.class.getResourceAsStream("/opencv/haarcascade_frontalface_default.xml");
            if (in == null) {
                // Fallbacks JavaCV/OpenCV (les chemins peuvent varier selon packaging)
                String[] candidates = new String[]{
                        "org/bytedeco/opencv/data/haarcascades/haarcascade_frontalface_default.xml",
                        "org/bytedeco/opencv/opencv/data/haarcascades/haarcascade_frontalface_default.xml",
                        "opencv/data/haarcascades/haarcascade_frontalface_default.xml",
                        "haarcascades/haarcascade_frontalface_default.xml"
                };
                for (String p : candidates) {
                    in = FaceCaptureService.class.getClassLoader().getResourceAsStream(p);
                    if (in != null) {
                        break;
                    }
                }
            }
            if (in == null) {
                throw new IOException("Cascade introuvable: /opencv/haarcascade_frontalface_default.xml (ou ressource JavaCV haarcascades manquante).");
            }
            try (InputStream closeMe = in) {
                Path tmp = Files.createTempFile("oxyn-haarcascade-", ".xml");
                tmp.toFile().deleteOnExit();
                Files.copy(closeMe, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                extractedCascadePath = tmp;
                return tmp;
            }
        }
    }
}

