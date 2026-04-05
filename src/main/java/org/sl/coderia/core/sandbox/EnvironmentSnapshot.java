package org.sl.coderia.core.sandbox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EnvironmentSnapshot {

    private final Path workingDir;
    private final String projectTree;
    private final String gitStatus;
    private final String buildTool;
    private final String runtimeInfo;

    private EnvironmentSnapshot(Path workingDir) throws IOException {
        this.workingDir  = workingDir;
        this.projectTree = buildTree();
        this.gitStatus   = readGitStatus();
        this.buildTool   = detectBuildTool();
        this.runtimeInfo = readRuntimeInfo();
    }

    public static EnvironmentSnapshot of(Path workingDir) throws IOException {
        return new EnvironmentSnapshot(workingDir);
    }

    public static EnvironmentSnapshot ofCurrent() throws IOException {
        String cwd = System.getProperty("user.dir");
        return new EnvironmentSnapshot(cwd.isEmpty() ? Path.of(".") : Path.of(cwd));
    }

    public String render() {
        return """
                ## Environment
                - Working dir : %s
                - Build tool  : %s
                - Runtime     : %s

                ## Git status
                %s

                ## Project structure
                %s
                """.formatted(workingDir, buildTool, runtimeInfo, gitStatus, projectTree);
    }

    private String buildTree() throws IOException {
        try (Stream<Path> paths = Files.walk(workingDir)) {
            return paths
                    .filter(this::isRelevant)
                    .sorted()
                    .map(p -> {
                        int depth = workingDir.relativize(p).getNameCount() - 1;
                        String indent = "  ".repeat(depth);
                        String name = p.getFileName().toString();
                        return indent + (Files.isDirectory(p) ? name + "/" : name);
                    })
                    .collect(Collectors.joining("\n"));
        }
    }

    private String readGitStatus() {
        return exec("git status --short --branch")
                .orElse("(not a git repository)");
    }

    private String detectBuildTool() {
        if (Files.exists(workingDir.resolve("pom.xml")))        return "Maven";
        if (Files.exists(workingDir.resolve("build.gradle")))   return "Gradle (Groovy)";
        if (Files.exists(workingDir.resolve("build.gradle.kts"))) return "Gradle (Kotlin)";
        if (Files.exists(workingDir.resolve("package.json")))   return "npm/Node";
        if (Files.exists(workingDir.resolve("Makefile")))       return "Make";
        return "unknown";
    }

    private String readRuntimeInfo() {
        return "Java " + System.getProperty("java.version")
                + " | " + System.getProperty("os.name")
                + " " + System.getProperty("os.arch");
    }

    private boolean isRelevant(Path path) {
        String rel = workingDir.relativize(path).toString();
        return Stream.of(".git", "target", "build", "node_modules", ".idea", ".gradle")
                .noneMatch(rel::startsWith);
    }

    private Optional<String> exec(String command) {
        try {
            Process p = new ProcessBuilder("bash", "-c", command)
                    .directory(workingDir.toFile())
                    .redirectErrorStream(true)
                    .start();
            String out = new String(p.getInputStream().readAllBytes()).strip();
            p.waitFor(5, TimeUnit.SECONDS);
            return Optional.of(out);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}