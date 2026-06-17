package com.clearn.worker.sandbox;

import java.nio.charset.StandardCharsets;

final class ProcessFlooder {
    private ProcessFlooder() {
    }

    public static void main(String[] args) throws Exception {
        byte[] block = "x".repeat(8192).getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < 256; i++) {
            System.out.write(block);
            System.err.write(block);
        }
        System.out.flush();
        System.err.flush();
    }
}
