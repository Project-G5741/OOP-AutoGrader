package com.eiu.capstone.backend.grading.testcase.worker;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Isolated testcase worker entry. IPC uses the process stdin/stdout streams
 * retained here before any later {@code System.setOut} redirect.
 */
public final class WorkerMain {

    private WorkerMain() {}

    public static void main(String[] args) throws Exception {
        PrintStream ipcOut = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        InputStream ipcIn = System.in;
        String mode = args != null && args.length > 0 ? args[0] : "";
        switch (mode) {
            case "--self-check" -> ipcOut.println("{\"ok\":true}");
            case "--dump-env" -> dumpEnv(ipcOut, args);
            case "--hang" -> hang();
            case "--child-hang" -> childHang(ipcOut);
            case "--stderr-flood" -> {
                floodStderr();
                ipcIn.transferTo(OutputStream.nullOutputStream());
            }
            default -> ipcIn.transferTo(OutputStream.nullOutputStream());
        }
    }

    private static void dumpEnv(PrintStream ipcOut, String[] args) {
        String[] names = args.length > 1
                ? java.util.Arrays.copyOfRange(args, 1, args.length)
                : new String[] {"JWT_SECRET", "DB_PASSWORD"};
        StringBuilder json = new StringBuilder("{");
        for (int i = 0; i < names.length; i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append('"').append(names[i]).append("\":");
            json.append(System.getenv(names[i]) != null);
        }
        json.append('}');
        ipcOut.println(json);
    }

    private static void hang() {
        while (true) {
            Thread.onSpinWait();
        }
    }

    private static void childHang(PrintStream ipcOut) throws Exception {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        ProcessBuilder child = windows
                ? new ProcessBuilder("ping", "-t", "127.0.0.1")
                : new ProcessBuilder("sleep", "3600");
        child.redirectErrorStream(true);
        Process process = child.start();
        ipcOut.println("{\"childPid\":" + process.pid() + "}");
        process.waitFor();
    }

    private static void floodStderr() throws Exception {
        byte[] chunk = new byte[4096];
        java.util.Arrays.fill(chunk, (byte) 'x');
        for (int i = 0; i < 50; i++) {
            System.err.write(chunk);
        }
        System.err.flush();
    }
}
