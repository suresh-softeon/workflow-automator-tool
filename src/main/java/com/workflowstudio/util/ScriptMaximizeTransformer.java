package com.workflowstudio.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transforms a generated Playwright script so the browser launches maximized.
 * Injects args: ['--start-maximized'] into the launch call and viewport: null
 * into the newContext call so the browser fills the full screen on playback.
 */
public final class ScriptMaximizeTransformer {

    // Matches .launch({ ... }) — [^}]* also spans newlines (not a closing brace)
    private static final Pattern LAUNCH_PATTERN = Pattern.compile(
            "(\\.launch\\(\\{)([^}]*)(\\}\\))");

    // Matches .newContext() with no arguments
    private static final Pattern NEW_CONTEXT_EMPTY = Pattern.compile(
            "\\.newContext\\(\\s*\\)");

    // Matches .newContext({ ... })
    private static final Pattern NEW_CONTEXT_WITH_OPTS = Pattern.compile(
            "(\\.newContext\\(\\{)([^}]*)(\\}\\))");

    private ScriptMaximizeTransformer() {
    }

    public static String apply(String script) {
        script = injectLaunchMaximize(script);
        script = injectContextViewport(script);
        return script;
    }

    private static String injectLaunchMaximize(String script) {
        Matcher m = LAUNCH_PATTERN.matcher(script);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String inner = m.group(2);
            if (inner.contains("--start-maximized")) {
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group()));
                continue;
            }
            String trimmed = inner.stripTrailing();
            String separator = (trimmed.isBlank() || trimmed.endsWith(",")) ? " " : ", ";
            String newInner = trimmed + separator + "args: ['--start-maximized']";
            m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + newInner + m.group(3)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String injectContextViewport(String script) {
        // Replace empty newContext() first
        script = NEW_CONTEXT_EMPTY.matcher(script).replaceAll(".newContext({ viewport: null })");

        // Process newContext({...})
        Matcher m = NEW_CONTEXT_WITH_OPTS.matcher(script);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String inner = m.group(2);
            if (inner.contains("viewport:")) {
                // Replace existing viewport value (object or null) with null
                inner = inner.replaceAll("viewport\\s*:\\s*(\\{[^}]*\\}|null)", "viewport: null");
            }
            if (!inner.contains("viewport:")) {
                String trimmed = inner.stripTrailing();
                String separator = (trimmed.isBlank() || trimmed.endsWith(",")) ? " " : ", ";
                inner = trimmed + separator + "viewport: null";
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + inner + m.group(3)));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
