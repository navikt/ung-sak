package no.nav.ung.sak.web.server.caching;

import jakarta.ws.rs.core.CacheControl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ExtendedCacheControlTest {
    @Test
    public void testExtensionValues() {
        final var cc = new ExtendedCacheControl();
        assertThat(cc.isImmutable()).isFalse();
        assertThat(cc.getStaleWhileRevalidate()).isZero();
        assertThat(cc.getStaleIfError()).isZero();
        cc.setImmutable(true);
        assertThat(cc.isImmutable()).isTrue();
        cc.setStaleWhileRevalidate(123);
        assertThat(cc.getStaleWhileRevalidate()).isEqualTo(123);
        cc.setStaleIfError(999);
        assertThat(cc.getStaleIfError()).isEqualTo(999);
        assertThat(cc.isImmutable()).isTrue();
    }

    @Test
    public void testNoDefaultExtra() {
        final var cc = new ExtendedCacheControl();
        cc.setImmutable(false);
        cc.setStaleIfError(0);
        cc.setStaleWhileRevalidate(0);
        assertThat(cc.isImmutable()).isFalse();
        assertThat(cc.getStaleWhileRevalidate()).isZero();
        assertThat(cc.getStaleIfError()).isZero();
        assertThat(cc.getCacheExtension()).isEmpty();
    }

    @Test
    public void testStringOutput() {
        final var cc = new ExtendedCacheControl();
        // Test that the extended one outputs the same as the base
        final var unExtended = new CacheControl();
        assertThat(cc.toString()).isEqualTo(unExtended.toString());
        cc.setMaxAge(123);
        unExtended.setMaxAge(123);
        assertThat(cc.toString()).isEqualTo(unExtended.toString());
        cc.setMustRevalidate(true);
        unExtended.setMustRevalidate(true);
        assertThat(cc.toString()).isEqualTo(unExtended.toString());
        cc.setImmutable(true);
        assertThat(cc.toString()).startsWith(unExtended.toString());
    }
}
