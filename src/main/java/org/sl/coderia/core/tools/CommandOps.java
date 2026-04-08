package org.sl.coderia.core.tools;

import org.sl.coderia.core.utils.TerminalIO;
import org.sl.coderia.core.utils.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface CommandOps {
    Path WORKSPACE_ROOT = Paths.get("").toAbsolutePath().normalize();
     Set<String> allowsCommands = new HashSet<>(
            List.of(
                    "ls", "cat", "echo", "pwd", "git",
                    "find", "grep", "head", "tail", "wc",
                    "mvn", "gradle", "java", "javac",
                    "curl", "wget",
                    "ps", "env", "which", "whoami"
            )
    );
     default String execOps(String command) throws IOException, InterruptedException {
            TerminalIO io = TerminalIO.getInstance();
            long startedAt = System.nanoTime();
            String commandId = Integer.toHexString((command == null ? "" : command).hashCode());
            io.printLine("[TOOL][execCommand][" + commandId + "] start cwd=" + WORKSPACE_ROOT + " cmd=" + compact(command, 140));
            ProcessBuilder pb =  new ProcessBuilder("bash", "-c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            int lineCount = 0;

            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                lineCount++;
            }

            int exitCode = process.waitFor();
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000L;
            io.printLine(
                    "[TOOL][execCommand][" + commandId + "] done exit=" + exitCode
                            + " durationMs=" + durationMs
                            + " lines=" + lineCount
                            + " chars=" + output.length()
                            + " outputPreview=" + compact(output.toString(), 220)
            );
            return """
                    {"success":true, "exitCode":%d, "output":"%s"}
                    """.formatted(exitCode, Utils.escapeJson(output.toString()));


    }
    private List<String> tokenizeCommand(String raw) {
        List<String> parts = new ArrayList<>();
        if (raw == null) {
            return parts;
        }
        String command = raw.trim();
        if (command.isEmpty()) {
            return parts;
        }

        StringBuilder current = new StringBuilder();
        boolean inSingle = false;
        boolean inDouble = false;

        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);

            if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
                continue;
            }
            if (c == '"' && !inSingle) {
                inDouble = !inDouble;
                continue;
            }

            if (!inSingle && !inDouble && Character.isWhitespace(c)) {
                if (!current.isEmpty()) {
                    parts.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }

            if (c == '\\' && i + 1 < command.length() && (inSingle || inDouble)) {
                i++;
                current.append(command.charAt(i));
                continue;
            }

            current.append(c);
        }

        if (inSingle || inDouble) {
            throw new IllegalArgumentException("Unclosed quote in command");
        }
        if (!current.isEmpty()) {
            parts.add(current.toString());
        }
        return parts;
    }

    private String compact(String value, int maxLen) {
        if (value == null) {
            return "<null>";
        }
        String normalized = value.replace("\n", "\\n").replace("\r", "\\r").trim();
        if (normalized.length() <= maxLen) {
            return normalized;
        }
        return normalized.substring(0, maxLen) + "...";
    }
    
    
}
