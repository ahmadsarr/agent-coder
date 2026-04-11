package org.sl.coderia.core.tools.action;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.sl.coderia.core.tools.ToolPolicy;
import org.sl.coderia.core.tools.ToolSandbox;
import org.sl.coderia.core.utils.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

public class CommandAction {

    private static final boolean RTK_AVAILABLE = detectRtkAvailability();

    private static boolean detectRtkAvailability() {
        try {
            Process process = new ProcessBuilder("bash", "-lc", "command -v rtk >/dev/null 2>&1").start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }


    @Tool("Execute a shell command in the working directory and return stdout+stderr. e.g. 'mvn test', 'python -m pytest', 'ls -la'.")
    @ToolPolicy(requires = {ToolSandbox.Permission.SHELL_EXEC}, risk = ToolSandbox.RiskLevel.SAFE, timeoutMs = 15_000)
    public String execCommand(
            @P("Shell command to run, e.g. 'mvn test' or 'python -m pytest'") String command) throws IOException, InterruptedException {
        if (RTK_AVAILABLE) {
            command = prefixEachCommand(command);
        }
        System.out.println("Executing command" + (RTK_AVAILABLE ? " via rtk: " : ": ") + command);
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        int exitCode = process.waitFor();

        return """
                {"success":true, "exitCode":%d, "output":"%s"}
                """.formatted(exitCode, Utils.escapeJson(output.toString()));


    }

        private static final Pattern COMMAND_SEPARATOR = Pattern.compile("(?<=&&|\\|\\||;|\\|)|(?=&&|\\|\\||;|\\|)");

        private String prefixEachCommand(String command) {
            if (command == null || command.isBlank()) {
                return command;
            }

            StringBuilder builder = new StringBuilder();
            String[] tokens = COMMAND_SEPARATOR.split(command);
            for (String token : tokens) {
                String trimmed = token.trim();
                if (trimmed.isEmpty() || isSeparator(trimmed)) {
                    builder.append(token);
                } else {
                    builder.append(prefixRtk(token));
                }
            }
            return builder.toString();
        }

        private boolean isSeparator(String token) {
            return token.equals("&&") || token.equals("||") || token.equals(";") || token.equals("|");
        }

        private String prefixRtk(String command) {
            if (command == null || command.isBlank()) {
                return command;
            }
            String trimmed = command.trim();
            if (trimmed.startsWith("rtk ") || trimmed.startsWith("sudo rtk ") || trimmed.equals("rtk") || trimmed.equals("sudo rtk")) {
                return command;
            }
            int firstNonWhitespace = 0;
            while (firstNonWhitespace < command.length() && Character.isWhitespace(command.charAt(firstNonWhitespace))) {
                firstNonWhitespace++;
            }
            return command.substring(0, firstNonWhitespace) + "rtk " + command.substring(firstNonWhitespace);
        }


}
