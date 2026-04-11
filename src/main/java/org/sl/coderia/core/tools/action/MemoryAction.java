package org.sl.coderia.core.tools.action;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.sl.coderia.core.tools.ToolPolicy;
import org.sl.coderia.core.tools.ToolSandbox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.APPEND;

public class MemoryAction {
    @Tool("Manage memory.md — persist or retrieve the current plan and state across steps.")
    @ToolPolicy(requires = {ToolSandbox.Permission.FILE_WRITE}, risk = ToolSandbox.RiskLevel.SAFE)
    public String memory(
            @P("Action: 'read' to retrieve full content, 'write' to overwrite, 'append' to add at the end") String action,
            @P("Content to write or append. Pass empty string when action is 'read'") String content) throws IOException {

        Path path = Path.of("memory.md");
        return switch (action) {
            case "read" -> Files.readString(path);
            case "write" -> {
                Files.writeString(path, content);
                yield "ok";
            }
            case "append" -> {
                Files.writeString(path, content, APPEND);
                yield "ok";
            }
            default -> "unknown action: " + action;
        };
    }

}
