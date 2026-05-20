package fr.robie.messageflow.formatter;

import org.junit.jupiter.api.DisplayName;

@DisplayName("FunctionalPlaceholderResolver Tests")
class FunctionalPlaceholderResolverTest {
//    private FunctionalPlaceholderResolver resolver;
//    private Placeholder localPlaceholders;
//
//    @BeforeEach
//    void setUp() {
//        resolver = new FunctionalPlaceholderResolver();
//        localPlaceholders = Placeholder.builder().build();
//        GlobalPlaceholder.clear();
//    }
//
//    @AfterEach
//    void tearDown() {
//        GlobalPlaceholder.clear();
//    }
//
//    @Test
//    @DisplayName("canResolve returns false when no global placeholders are registered")
//    void testCanResolveNoGlobalPlaceholders() {
//        String text = "Hello %world%";
//        boolean result = resolver.canResolve(text, null, localPlaceholders);
//        assertFalse(result);
//    }
//
//    @Test
//    @DisplayName("canResolve returns true when global placeholder exists in text")
//    void testCanResolveWithGlobalPlaceholder() {
//        GlobalPlaceholder.register("world", "Earth");
//        String text = "Hello %world%";
//        boolean result = resolver.canResolve(text, null, localPlaceholders);
//        assertTrue(result);
//    }
//
//    @Test
//    @DisplayName("canResolve returns false when global placeholder exists but not in text")
//    void testCanResolveGlobalNotInText() {
//        GlobalPlaceholder.register("world", "Earth");
//        String text = "Hello there";
//        boolean result = resolver.canResolve(text, null, localPlaceholders);
//        assertFalse(result);
//    }
//
//    @Test
//    @DisplayName("resolve replaces static global placeholders")
//    void testResolveStaticPlaceholder() {
//        GlobalPlaceholder.register("server", "MyServer");
//        String text = "Server: %server%";
//        String result = resolver.resolve(text, null, localPlaceholders);
//        assertEquals("Server: MyServer", result);
//    }
//
//    @Test
//    @DisplayName("resolve replaces multiple static global placeholders")
//    void testResolveMultiplePlaceholders() {
//        GlobalPlaceholder.register("server", "MyServer");
//        GlobalPlaceholder.register("version", "1.0");
//        String text = "%server% v%version%";
//        String result = resolver.resolve(text, null, localPlaceholders);
//        assertEquals("MyServer v1.0", result);
//    }
//
//    @Test
//    @DisplayName("resolve evaluates dynamic global placeholders")
//    void testResolveDynamicPlaceholder() {
//        GlobalPlaceholder.register("timestamp", () -> "12345");
//        String text = "Time: %timestamp%";
//        String result = resolver.resolve(text, null, localPlaceholders);
//        assertEquals("Time: 12345", result);
//    }
//
//    @Test
//    @DisplayName("resolve evaluates player-specific global placeholders with null player")
//    void testResolvePlayerPlaceholderNullPlayer() {
//        GlobalPlaceholder.register("default_val", "DefaultValue");
//        String text = "Value: %default_val%";
//        String result = resolver.resolve(text, null, localPlaceholders);
//        assertEquals("Value: DefaultValue", result);
//    }
//
//    @Test
//    @DisplayName("resolve returns text unchanged when no placeholders match")
//    void testResolveNoMatch() {
//        GlobalPlaceholder.register("server", "MyServer");
//        String text = "Hello %world%";
//        String result = resolver.resolve(text, null, localPlaceholders);
//        assertEquals("Hello %world%", result);
//    }
//
//    @Test
//    @DisplayName("resolve returns empty string when text is empty")
//    void testResolveEmptyText() {
//        String text = "";
//        String result = resolver.resolve(text, null, localPlaceholders);
//        assertEquals("", result);
//    }
//
//    @Test
//    @DisplayName("resolve returns text unchanged when no global placeholders registered")
//    void testResolveNoGlobalRegistered() {
//        String text = "Hello %world%";
//        String result = resolver.resolve(text, null, localPlaceholders);
//        assertEquals("Hello %world%", result);
//    }
//
//    @Test
//    @DisplayName("resolve returns original placeholder when resolution fails silently")
//    void testResolveExceptionInDynamic() {
//        GlobalPlaceholder.register("error", () -> {
//            throw new RuntimeException("Test error");
//        });
//        String text = "Value: %error%";
//        String result = resolver.resolve(text, null, localPlaceholders);
//        // When Global Placeholder resolution fails due to exception, get() returns empty
//        // So FunctionalPlaceholderResolver returns the original placeholder text
//        assertEquals("Value: %error%", result);
//    }
//
//    @Test
//    @DisplayName("resolve handles mixed content with multiple placeholder types")
//    void testResolveMixedContent() {
//        GlobalPlaceholder.register("static", "Static");
//        GlobalPlaceholder.register("dynamic", () -> "Dynamic");
//        String text = "Static: %static%, Dynamic: %dynamic%";
//        String result = resolver.resolve(text, null, localPlaceholders);
//        assertEquals("Static: Static, Dynamic: Dynamic", result);
//    }
//
//    @Test
//    @DisplayName("resolve handles special regex characters in replacement")
//    void testResolveSpecialCharacters() {
//        GlobalPlaceholder.register("special", "\\$1.$2");
//        String text = "Value: %special%";
//        String result = resolver.resolve(text, null, localPlaceholders);
//        assertEquals("Value: \\$1.$2", result);
//    }
//
//    @Test
//    @DisplayName("resolve handles percent signs in placeholder values")
//    void testResolvePercentInValue() {
//        GlobalPlaceholder.register("percent", "100%");
//        String text = "Complete: %percent%";
//        String result = resolver.resolve(text, null, localPlaceholders);
//        assertEquals("Complete: 100%", result);
//    }
//
//    @Test
//    @DisplayName("resolve handles adjacent placeholders")
//    void testResolveAdjacentPlaceholders() {
//        GlobalPlaceholder.register("a", "A");
//        GlobalPlaceholder.register("b", "B");
//        String text = "%a%%b%";
//        String result = resolver.resolve(text, null, localPlaceholders);
//        assertEquals("AB", result);
//    }
//
//    @Test
//    @DisplayName("resolve returns original text for single percent signs")
//    void testResolveSinglePercent() {
//        GlobalPlaceholder.register("key", "value");
//        String text = "50% complete";
//        String result = resolver.resolve(text, null, localPlaceholders);
//        assertEquals("50% complete", result);
//    }
//
//    @Test
//    @DisplayName("resolve handles three or more consecutive percent signs")
//    void testResolveTriplePercent() {
//        GlobalPlaceholder.register("test", "value");
//        String text = "Text %%% more";
//        String result = resolver.resolve(text, null, localPlaceholders);
//        assertEquals("Text %%% more", result);
//    }
//
//    @Test
//    @DisplayName("resolve handles whitespace in placeholder keys")
//    void testResolveWhitespaceInKey() {
//        GlobalPlaceholder.register("key_with_space", "value");
//        String text = "Test %key_with_space%";
//        String result = resolver.resolve(text, null, localPlaceholders);
//        assertEquals("Test value", result);
//    }
//
//    @Test
//    @DisplayName("resolve handles empty placeholder value")
//    void testResolveEmptyValue() {
//        GlobalPlaceholder.register("empty", "");
//        String text = "Before%empty%After";
//        String result = resolver.resolve(text, null, localPlaceholders);
//        assertEquals("BeforeAfter", result);
//    }
//
//    @Test
//    @DisplayName("canResolve returns false for malformed placeholder without delimiters")
//    void testCanResolveMalformedPlaceholder() {
//        GlobalPlaceholder.register("test", "value");
//        String text = "Text test value";
//        boolean result = resolver.canResolve(text, null, localPlaceholders);
//        assertFalse(result);
//    }
//
//    @Test
//    @DisplayName("resolve updates value on each call for dynamic placeholders")
//    void testResolveDynamicMultipleCalls() {
//        final int[] callCount = {0};
//        GlobalPlaceholder.register("counter", () -> String.valueOf(++callCount[0]));
//
//        String result1 = resolver.resolve("Count: %counter%", null, localPlaceholders);
//        String result2 = resolver.resolve("Count: %counter%", null, localPlaceholders);
//
//        assertEquals("Count: 1", result1);
//        assertEquals("Count: 2", result2);
//    }
//
//    @Test
//    @DisplayName("resolve handles newlines in placeholder values")
//    void testResolveNewlines() {
//        GlobalPlaceholder.register("multiline", "line1\nline2");
//        String text = "Text: %multiline% end";
//        String result = resolver.resolve(text, null, localPlaceholders);
//        assertEquals("Text: line1\nline2 end", result);
//    }
//
//    @Test
//    @DisplayName("resolve preserves order of multiple placeholders")
//    void testResolvePreservesOrder() {
//        GlobalPlaceholder.register("first", "1");
//        GlobalPlaceholder.register("second", "2");
//        GlobalPlaceholder.register("third", "3");
//        String text = "%first%-%second%-%third%";
//        String result = resolver.resolve(text, null, localPlaceholders);
//        assertEquals("1-2-3", result);
//    }
//
//    @Test
//    @DisplayName("canResolve returns true for multiple placeholders in text")
//    void testCanResolveMultiplePlaceholders() {
//        GlobalPlaceholder.register("p1", "v1");
//        GlobalPlaceholder.register("p2", "v2");
//        String text = "%p1% and %p2%";
//        boolean result = resolver.canResolve(text, null, localPlaceholders);
//        assertTrue(result);
//    }
}
