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
        if (args != null && args.length == 1 && "--self-check".equals(args[0])) {
            ipcOut.println("{\"ok\":true}");
            return;
        }
        ipcIn.transferTo(OutputStream.nullOutputStream());
    }
}
