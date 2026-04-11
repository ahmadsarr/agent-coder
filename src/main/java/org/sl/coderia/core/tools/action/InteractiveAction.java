package org.sl.coderia.core.tools.action;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.NoArgsConstructor;
import org.sl.coderia.core.tools.ToolPolicy;
import org.sl.coderia.core.tools.ToolSandbox.RiskLevel;
import org.sl.coderia.core.utils.TerminalIO;

import java.io.IOException;

@NoArgsConstructor
public class InteractiveAction {


    @Tool("Print a message to the terminal as an agent status update.")
    @ToolPolicy(risk = RiskLevel.SAFE)
    public String print(
            @P("Message to display") String message) {
            TerminalIO.getInstance().printLine("[AGENT] " + message);
            return "ok";
    }

    @Tool("Ask the human a question and wait for their response. Use when you need clarification, a decision, or missing information before proceeding.")
    @ToolPolicy(risk = RiskLevel.SAFE, timeoutMs = 1_000_000)
    public String askHuman(
            @P("Clear and concise question to ask the user") String question) throws IOException {
        return TerminalIO.getInstance().read("YOU:" + question);
    }

}
