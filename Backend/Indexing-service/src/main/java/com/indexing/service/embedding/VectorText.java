package com.indexing.service.embedding;

// Converts vectors to and from the text format pgvector understands, such as "[0.1,0.2,0.3]".
// Every class that sends a vector to PostgreSQL uses this one helper.
public final class VectorText {

    private VectorText() {
    }

// ------ Turns a float array into text, for example "[1.0,2.5,-0.25]" ------
    public static String toLiteral(float[] values) {
        StringBuilder text = new StringBuilder("[");

        for (int i = 0; i<values.length; i++) {
            // NaN and infinity are not valid numbers in a vector, so we refuse them early.
            if (!Float.isFinite(values[i])) {
                throw new IllegalArgumentException("Embedding contains a value that is not a finite number.");
            }
            if (i > 0) {
                text.append(',');
            }

            text.append(values[i]);
        }
        return text.append(']').toString();
    }
}
