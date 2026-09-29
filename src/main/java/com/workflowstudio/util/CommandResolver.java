package com.workflowstudio.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class CommandResolver {
    private static final Set<String> WINDOWS_CMD_TOOLS = Set.of("npm", "npx");

    private CommandResolver() {
    }

    public static List<String> resolve(List<String> command) {
        if (command == null || command.isEmpty() || !isWindows()) {
            return command;
        }

        String executable = command.get(0);
        if (!WINDOWS_CMD_TOOLS.contains(executable) || executable.endsWith(".cmd")) {
            return command;
        }

        List<String> resolved = new ArrayList<>(command);
        resolved.set(0, executable + ".cmd");
        return resolved;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("win");
    }
}
