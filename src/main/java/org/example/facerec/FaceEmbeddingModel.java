package org.example.facerec;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_dnn.Net;
import org.bytedeco.javacpp.indexer.FloatIndexer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.bytedeco.opencv.global.opencv_dnn.readNetFromTorch;
import static org.bytedeco.opencv.global.opencv_dnn.blobFromImage;
import static org.bytedeco.opencv.global.opencv_core.CV_32F;

/**
 * Modèle embedding 128-dim via OpenCV DNN.
 *
 * Attendus dans les ressources :
 * - /opencv/openface_nn4.small2.v1.t7  (modèle Torch OpenFace 128D)
 *
 * NB: le fichier modèle n'est pas fourni par OpenCV ; il doit être ajouté au projet.
 */
public final class FaceEmbeddingModel {

    @SuppressWarnings("resource")
    private static volatile Net net;
    private static volatile Path extractedModelPath;

    private FaceEmbeddingModel() {
    }

    public static float[] embedFaceBgr96(Mat faceBgr96) throws IOException {
        if (faceBgr96 == null || faceBgr96.empty()) {
            throw new IllegalArgumentException("Image visage vide.");
        }
        Net n = ensureNetLoaded();

        // OpenFace est entraîné en RGB (souvent). On convertit via swapRB=true.
        Mat blob = blobFromImage(faceBgr96, 1.0 / 255.0, new org.bytedeco.opencv.opencv_core.Size(96, 96),
                new org.bytedeco.opencv.opencv_core.Scalar(0.0, 0.0, 0.0, 0.0), true, false, CV_32F);
        n.setInput(blob);
        Mat out = n.forward();

        if (out == null || out.empty() || out.total() < FaceEmbeddingCodec.DIM) {
            throw new IOException("Sortie modèle invalide.");
        }

        float[] v = readEmbedding128(out);
        v = l2Normalize(v);
        sanityCheckEmbedding(v);
        return v;
    }

    private static float[] readEmbedding128(Mat out) throws IOException {
        float[] v = new float[FaceEmbeddingCodec.DIM];
        try (FloatIndexer idx = out.createIndexer()) {
            int rank = idx.rank();
            long[] sizes = new long[rank];
            for (int d = 0; d < rank; d++) {
                sizes[d] = idx.size(d);
            }

            // Selon versions, la sortie peut être [1,128] ou [1,1,1,128].
            try {
                if (rank == 2 && idx.size(0) >= 1 && idx.size(1) >= FaceEmbeddingCodec.DIM) {
                    for (int i = 0; i < FaceEmbeddingCodec.DIM; i++) {
                        v[i] = idx.get(0, i);
                    }
                    return v;
                }
                if (rank == 3) {
                    // Ex: [1,128,1] ou [1,1,128]
                    if (idx.size(0) >= 1 && idx.size(1) >= FaceEmbeddingCodec.DIM && idx.size(2) >= 1) {
                        for (int i = 0; i < FaceEmbeddingCodec.DIM; i++) {
                            v[i] = idx.get(0, i, 0);
                        }
                        return v;
                    }
                    if (idx.size(0) >= 1 && idx.size(1) >= 1 && idx.size(2) >= FaceEmbeddingCodec.DIM) {
                        for (int i = 0; i < FaceEmbeddingCodec.DIM; i++) {
                            v[i] = idx.get(0, 0, i);
                        }
                        return v;
                    }
                }
                if (rank == 4 && idx.size(0) >= 1 && idx.size(1) >= 1 && idx.size(2) >= 1 && idx.size(3) >= FaceEmbeddingCodec.DIM) {
                    for (int i = 0; i < FaceEmbeddingCodec.DIM; i++) {
                        v[i] = idx.get(0, 0, 0, i);
                    }
                    return v;
                }

                // Fallback: lecture linéaire (dernier recours)
                long total = 1;
                for (int d = 0; d < rank; d++) {
                    total *= Math.max(1, idx.size(d));
                }
                if (total < FaceEmbeddingCodec.DIM) {
                    throw new IOException("Sortie modèle invalide (dimensions inattendues).");
                }

                // Cas fréquent observé sur certaines builds : total = 36 * 128 (ex. 4608).
                // On moyenne par blocs de 128 pour obtenir un embedding 128D.
                if (total == FaceEmbeddingCodec.DIM) {
                    for (int i = 0; i < FaceEmbeddingCodec.DIM; i++) {
                        v[i] = idx.get(i);
                    }
                    return v;
                }
                if (total % FaceEmbeddingCodec.DIM == 0) {
                    int blocks = (int) (total / FaceEmbeddingCodec.DIM);
                    for (int i = 0; i < FaceEmbeddingCodec.DIM; i++) {
                        double sum = 0.0;
                        for (int b = 0; b < blocks; b++) {
                            sum += idx.get((long) b * FaceEmbeddingCodec.DIM + i);
                        }
                        v[i] = (float) (sum / blocks);
                    }
                    return v;
                }

                // Sinon: on prend les 128 premières valeurs (dernier recours).
                for (int i = 0; i < FaceEmbeddingCodec.DIM; i++) {
                    v[i] = idx.get(i);
                }
                return v;
            } catch (RuntimeException ex) {
                // Certains indexers jettent des erreurs natives peu parlantes (ex: "4608").
                long total = 1;
                for (int d = 0; d < rank; d++) {
                    total *= Math.max(1, idx.size(d));
                }
                StringBuilder sb = new StringBuilder();
                sb.append("Erreur lecture sortie modèle. ")
                        .append("rank=").append(rank)
                        .append(", total=").append(total)
                        .append(", sizes=[");
                for (int i = 0; i < sizes.length; i++) {
                    if (i > 0) sb.append(',');
                    sb.append(sizes[i]);
                }
                sb.append("]. Cause: ").append(ex.getClass().getSimpleName())
                        .append(": ").append(ex.getMessage());
                throw new IOException(sb.toString(), ex);
            }
        }
    }

    private static float[] l2Normalize(float[] v) {
        double s = 0.0;
        for (float f : v) {
            s += (double) f * f;
        }
        double n = Math.sqrt(s);
        if (n == 0.0) {
            return v;
        }
        for (int i = 0; i < v.length; i++) {
            v[i] = (float) (v[i] / n);
        }
        return v;
    }

    private static void sanityCheckEmbedding(float[] v) throws IOException {
        double mean = 0.0;
        for (float f : v) {
            if (!Float.isFinite(f)) {
                throw new IOException("Embedding invalide (NaN/Inf).");
            }
            mean += f;
        }
        mean /= v.length;
        double var = 0.0;
        for (float f : v) {
            double d = f - mean;
            var += d * d;
        }
        var /= v.length;
        if (var < 1e-6) {
            throw new IOException("Embedding invalide (quasi constant). Vérifiez la détection/crop visage et le modèle.");
        }
    }

    private static Net ensureNetLoaded() throws IOException {
        Net cached = net;
        if (cached != null) {
            return cached;
        }
        synchronized (FaceEmbeddingModel.class) {
            cached = net;
            if (cached != null) {
                return cached;
            }
            Path model = ensureModelExtracted();
            Net n = readNetFromTorch(model.toAbsolutePath().toString());
            net = n;
            return n;
        }
    }

    private static Path ensureModelExtracted() throws IOException {
        Path cached = extractedModelPath;
        if (cached != null && Files.exists(cached)) {
            return cached;
        }
        synchronized (FaceEmbeddingModel.class) {
            cached = extractedModelPath;
            if (cached != null && Files.exists(cached)) {
                return cached;
            }
            try (InputStream in = FaceEmbeddingModel.class.getResourceAsStream("/opencv/openface_nn4.small2.v1.t7")) {
                if (in == null) {
                    throw new IOException("Modèle introuvable: /opencv/openface_nn4.small2.v1.t7 (à ajouter dans src/main/resources/opencv/)");
                }
                Path tmp = Files.createTempFile("oxyn-openface-", ".t7");
                tmp.toFile().deleteOnExit();
                Files.copy(in, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                extractedModelPath = tmp;
                return tmp;
            }
        }
    }
}

