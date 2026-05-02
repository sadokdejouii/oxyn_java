package org.example.facerec;

public final class FaceSimilarity {

    private FaceSimilarity() {
    }

    public static double cosineDistance(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) {
            throw new IllegalArgumentException("Vecteurs incompatibles.");
        }
        double dot = 0.0;
        double na = 0.0;
        double nb = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            na += (double) a[i] * a[i];
            nb += (double) b[i] * b[i];
        }
        if (na == 0.0 || nb == 0.0) {
            return 1.0;
        }
        double cos = dot / (Math.sqrt(na) * Math.sqrt(nb));
        return 1.0 - cos;
    }
}

