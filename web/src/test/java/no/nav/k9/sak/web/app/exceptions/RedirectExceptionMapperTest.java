package no.nav.k9.sak.web.app.exceptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.k9.felles.exception.ManglerTilgangException;
import no.nav.k9.felles.sikkerhet.ContextPathHolder;
import no.nav.k9.sak.test.util.Whitebox;

@SuppressWarnings("deprecation")
public class RedirectExceptionMapperTest {

    private RedirectExceptionMapper exceptionMapper;

    @BeforeEach
    public void setUp() throws Exception {
        initMocks(this);

        exceptionMapper = new RedirectExceptionMapper();
        Whitebox.setInternalState(exceptionMapper, "loadBalancerUrl", "https://erstatter.nav.no");

        ContextPathHolder.instance("/k9/sak");
    }

    @Test
    @SuppressWarnings("resource")
    public void skalMappeValideringsfeil() {
        // Arrange
        String feilmelding = "feilmelding";

        // Act
        Response response = exceptionMapper.toResponse(new ManglerTilgangException("123abc", feilmelding));

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.TEMPORARY_REDIRECT.getStatusCode());
        assertThat(response.getMediaType()).isEqualTo(null);
        assertThat(response.getMetadata().get("Content-Encoding").get(0))
            .isEqualTo("UTF-8");
        assertThat(response.getMetadata().get("Location").get(0).toString())
            .isEqualTo("https://erstatter.nav.no/k9/sak/#?errorcode=" + feilmelding);
    }

}
