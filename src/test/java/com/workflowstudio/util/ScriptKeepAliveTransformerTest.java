package com.workflowstudio.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ScriptKeepAliveTransformerTest {
    @Test
    void removesCloseStatementsAndKeepsBrowserOpenInsideIife() {
        String input = """
                const { chromium } = require('playwright');
                (async () => {
                  const browser = await chromium.launch({ headless: false });
                  const context = await browser.newContext();
                  await context.close();
                  await browser.close();
                })();
                """;

        String output = ScriptKeepAliveTransformer.apply(input);

        Assertions.assertFalse(output.contains("await context.close();"));
        Assertions.assertFalse(output.contains("await browser.close();"));
        Assertions.assertTrue(output.contains("  await new Promise(() => {});"));
        Assertions.assertTrue(output.indexOf("await new Promise(() => {});") < output.indexOf("})();"));
    }

    @Test
    void repairsLegacyKeepAliveOutsideIife() {
        String input = """
                (async () => {
                  console.log("done");
                })();
                await new Promise(() => {});
                """;

        String output = ScriptKeepAliveTransformer.apply(input);
        Assertions.assertTrue(output.contains("  await new Promise(() => {});"));
        Assertions.assertTrue(output.indexOf("await new Promise(() => {});") < output.indexOf("})();"));
    }

    @Test
    void removesOnlyLastPageCloseOccurrence() {
        String input = """
                (async () => {
                  await page.close();
                  await something();
                  await page.close();
                })();
                """;

        String output = ScriptKeepAliveTransformer.apply(input);
        int first = output.indexOf("await page.close();");
        int last = output.lastIndexOf("await page.close();");
        Assertions.assertTrue(first >= 0);
        Assertions.assertEquals(first, last);
    }

    @Test
    void removesOnlyLastContextCloseOccurrence() {
        String input = """
                (async () => {
                  await context.close();
                  await something();
                  await context.close();
                })();
                """;

        String output = ScriptKeepAliveTransformer.apply(input);
        int first = output.indexOf("await context.close();");
        int last = output.lastIndexOf("await context.close();");
        Assertions.assertTrue(first >= 0);
        Assertions.assertEquals(first, last);
    }
}
