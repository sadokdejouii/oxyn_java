package org.example.facerec;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class FaceEmbeddingCodec {

    public static final int DIM = 128;

    private FaceEmbeddingCodec() {
    }

    public static byte[] toBytes(float[] v) {
        if (v == null || v.length != DIM) {
            throw new IllegalArgumentException("Embedding invalide (attendu " + DIM + " floats).");
        }
        ByteBuffer bb = ByteBuffer.allocate(DIM * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float f : v) {
            bb.putFloat(f);
        }
        return bb.array();
    }

    public static float[] fromBytes(byte[] b) {
        if (b == null || b.length != DIM * 4) {
            throw new IllegalArgumentException("Embedding invalide (attendu " + (DIM * 4) + " bytes).");
        }
        float[] v = new float[DIM];
        ByteBuffer bb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < DIM; i++) {
            v[i] = bb.getFloat();
        }
        return v;
    }
}

