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
     default String execOps(String command) throws IOException, InterruptedException {
            ProcessBuilder pb =  new ProcessBuilder("bash", "-c", command);
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
