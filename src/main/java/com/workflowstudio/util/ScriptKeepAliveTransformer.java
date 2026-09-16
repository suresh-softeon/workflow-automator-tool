package com.workflowstudio.util;

import java.util.ArrayList;
import java.util.List;

public final class ScriptKeepAliveTransformer {
    private static final String KEEP_ALIVE = "await new Promise(() => {});";
    private static final String BROWSER_CLOSE = "await browser.close();";
    private static final String IIFE_END = "})();";

    private ScriptKeepAliveTransformer() {
    }

    public static String apply(String script) {
        String normalized = script.replace("\r\n", "\n").stripTrailing();
        String[] rawLines = normalized.split("\n");
        List<String> lines = new ArrayList<>();

        for (String line : rawLines) {
            String trimmed = line.trim();
            if (trimmed.equals(KEEP_ALIVE) || trimmed.equals(BROWSER_CLOSE)) {
                continue;
            }
            lines.add(line);
        }

        removeLastMatchingLine(lines, "await page.close();");
        removeLastMatchingLine(lines, "await context.close();");

        int iifeEndIndex = findIifeEndIndex(lines);
        if (iifeEndIndex >= 0) {
            lines.add(iifeEndIndex, "  " + KEEP_ALIVE);
        } else {
            lines.add(KEEP_ALIVE);
        }

        return String.join(System.lineSeparator(), lines) + System.lineSeparator();
    }

    private static int findIifeEndIndex(List<String> lines) {
        for (int i = lines.size() - 1; i >= 0; i--) {
            if (lines.get(i).trim().equals(IIFE_END)) {
                return i;
            }
        }
        return -1;
    }

    private static void removeLastMatchingLine(List<String> lines, String targetLine) {
        for (int i = lines.size() - 1; i >= 0; i--) {
            if (lines.get(i).trim().equals(targetLine)) {
                lines.remove(i);
                return;
            }
        }
    }
}
