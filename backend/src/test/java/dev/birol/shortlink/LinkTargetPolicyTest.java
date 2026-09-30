package dev.birol.shortlink;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LinkTargetPolicyTest {
    @Test
    void acceptsPublicHttpsUrl() {
        assertEquals("https://example.com/path?q=1", LinkTargetPolicy.normalize(" https://example.com/path?q=1 "));
    }

    @Test
    void rejectsLocalAndNonWebTargets() {
        assertThrows(IllegalArgumentException.class, () -> LinkTargetPolicy.normalize("http://localhost/admin"));
        assertThrows(IllegalArgumentException.class, () -> LinkTargetPolicy.normalize("http://192.168.1.2"));
        assertThrows(IllegalArgumentException.class, () -> LinkTargetPolicy.normalize("javascript:alert(1)"));
    }
}
