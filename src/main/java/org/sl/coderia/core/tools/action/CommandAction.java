package org.sl.coderia.core.tools.action;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.sl.coderia.core.tools.ToolPolicy;
import org.sl.coderia.core.tools.ToolSandbox;
import org.sl.coderia.core.utils.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CommandAction {


    @Tool("Execute a shell command in the working directory and return stdout+stderr. e.g. 'mvn test', 'python -m pytest', 'ls -la'.")
    @ToolPolicy(requires = {ToolSandbox.Permission.SHELL_EXEC}, risk = ToolSandbox.RiskLevel.SAFE, timeoutMs = 15_000)
    public String execCommand(
            @P("Shell command to run, e.g. 'mvn test' or 'python -m pytest'") String command) throws IOException, InterruptedException {
        System.out.println("Executing command: " + command);
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

}
