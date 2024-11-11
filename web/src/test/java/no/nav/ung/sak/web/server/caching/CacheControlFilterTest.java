package no.nav.ung.sak.web.server.caching;

import static org.assertj.core.api.Assertions.assertThat;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CacheControlFilterTest {

    @Test
    public void filter_setter_response_header() throws IOException {
        final ContainerRequestContext req = mock(ContainerRequestContext.class);
        when(req.getMethod()).thenReturn("GET");
        final ContainerResponseContext res = mock(ContainerResponseContext.class);
        final var responseHeaders = new MultivaluedHashMap<String, Object>();
        when(res.getHeaders()).thenReturn(responseHeaders);
        final CacheControlFilter ccf = new CacheControlFilter(
            333,
            false,
            true,
            true,
            true,
            false,
            444,
           555
        );

        ccf.filter(req, res);
        final var cacheControlObjList = responseHeaders.get("Cache-Control");
        assertThat(cacheControlObjList).hasSize(1);
        final var cacheControlObj = cacheControlObjList.getFirst();
        assertThat(cacheControlObj).isInstanceOf(ExtendedCacheControl.class);
        final ExtendedCacheControl ecc = (ExtendedCacheControl) cacheControlObj;
        assertThat(ecc.getMaxAge()).isEqualTo(333);
        assertThat(ecc.isNoStore()).isFalse();
        assertThat(ecc.isPrivate()).isTrue();
        assertThat(ecc.getStaleWhileRevalidate()).isEqualTo(444);
        assertThat(ecc.getStaleIfError()).isEqualTo(555);
    }
}
