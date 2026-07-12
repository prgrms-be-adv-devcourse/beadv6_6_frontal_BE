package com.biddy.recommendation.util;

import java.util.List;

public class VectorTextUtils {

    private VectorTextUtils() {
    }

    public static String toText(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        return sb.append("]").toString();
    }

    public static float[] fromText(String text) {
        String[] parts = text.replace("[", "").replace("]", "").split(",");
        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i]);
        }
        return vector;
    }

    public static float[] average(List<float[]> vectors) {
        int dimension = vectors.get(0).length;
        float[] result = new float[dimension];
        for (float[] vector : vectors) {
            for (int i = 0; i < dimension; i++) {
                result[i] += vector[i];
            }
        }
        for (int i = 0; i < dimension; i++) {
            result[i] /= vectors.size();
        }
        return result;
    }
}
