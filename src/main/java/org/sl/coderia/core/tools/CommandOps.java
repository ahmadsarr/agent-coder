package org.sl.coderia.core.tools;

import org.sl.coderia.core.utils.TerminalIO;
import org.sl.coderia.core.utils.Utils;

import java.io.BufferedReader;
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
     default String execOps(String command) {
        try {
            List<String> args = tokenizeCommand(command);
            if (args.isEmpty()) {
                return """
                    {"success":false, "error":"Empty command"}
                    """;

            }
            String cmdName = args.get(0);
            boolean isApproval = allowsCommands.contains(cmdName) || TerminalIO.getInstance().requestApproval("Can I execute this command: " + command);
            if (!isApproval) {
                return """
                    {"success":false, "error":"Command not allowed: %s"}
                    """.formatted(cmdName);
            }

            TerminalIO.getInstance().printLine("[TOOL] Executing command: " + command);

            ProcessBuilder pb = new ProcessBuilder(args);
            pb.directory(WORKSPACE_ROOT.toFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                TerminalIO.getInstance().printLine("[COMMAND] " + line); // stream to terminal
            }

            int exitCode = process.waitFor();
            return """
                    {"success":true, "exitCode":%d, "output":"%s"}
                    """.formatted(exitCode, Utils.escapeJson(output.toString()));

        } catch (Exception e) {
            return """
                    {"success":false, "error":"%s"}
                    """.formatted(Utils.escapeJson(e.getMessage()));
        }
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
    
    
}
