package org.sl.coderia.core.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public class TerminalIO {

    protected static volatile TerminalIO instance;
    private final BufferedReader reader;

    private TerminalIO() {
        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }

    public static TerminalIO getInstance() {
        if (instance == null) {
            synchronized (TerminalIO.class) {
                if (instance == null) {
                    instance = new TerminalIO();
                }
            }
        }
        return instance;
    }

    public String read(String prompt) throws IOException {
        if (prompt != null && !prompt.isEmpty()) {
            System.out.print(prompt);
            System.out.flush();
        }
        return reader.readLine();
    }

    public boolean requestApproval(String prompt,Object... args) throws IOException {
        return "y".equalsIgnoreCase(read(prompt+String.join(" ", Arrays.toString(args))+" (y/n): "));
    }

    public synchronized void printLine(String text) {
        System.out.println(text);
    }

    public void close() throws IOException {
        reader.close();
    }
}