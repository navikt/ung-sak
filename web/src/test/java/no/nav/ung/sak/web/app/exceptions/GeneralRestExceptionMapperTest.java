package no.nav.ung.sak.web.app.exceptions;

import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.exception.VLException;
import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.FunksjonellFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;
import no.nav.ung.sak.kontrakt.FeilDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GeneralRestExceptionMapperTest {

    @Test
    public void skalMappeFunksjonellFeil() {
        Feil funksjonellFeil = TestFeil.FACTORY.funksjonellFeil();

        @SuppressWarnings("resource")
        Response response = new VLExceptionMapper().toResponse(funksjonellFeil.toException());

        assertThat(response.getEntity()).isInstanceOf(FeilDto.class);
        FeilDto feilDto = (FeilDto) response.getEntity();

        assertThat(feilDto.getFeilmelding()).contains("FUNK_FEIL");
        assertThat(feilDto.getFeilmelding()).contains("en funksjonell feilmelding");
        assertThat(feilDto.getFeilmelding()).contains("et løsningsforslag");
    }

    @Test
    public void skalMappeVLException() {
        VLException vlException = TestFeil.FACTORY.tekniskFeil().toException();

        @SuppressWarnings("resource")
        Response response = new VLExceptionMapper().toResponse(vlException);

        assertThat(response.getEntity()).isInstanceOf(FeilDto.class);
        FeilDto feilDto = (FeilDto) response.getEntity();

        assertThat(feilDto.getFeilmelding()).contains("TEK_FEIL");
        assertThat(feilDto.getFeilmelding()).contains("en teknisk feilmelding");
    }

    @Test
    public void skalMappeGenerellFeil() {
        String feilmelding = "en helt generell feil";

        @SuppressWarnings("resource")
        Response response = new ThrowableExceptionMapper().toResponse(new RuntimeException(feilmelding));

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getEntity()).isInstanceOf(FeilDto.class);
        FeilDto feilDto = (FeilDto) response.getEntity();

        assertThat(feilDto.getFeilmelding()).contains(feilmelding);
    }

    interface TestFeil extends DeklarerteFeil {
        TestFeil FACTORY = FeilFactory.create(TestFeil.class); // NOSONAR ok med konstant i interface her

        @FunksjonellFeil(feilkode = "FUNK_FEIL", feilmelding = "en funksjonell feilmelding", løsningsforslag = "et løsningsforslag", logLevel = LogLevel.WARN)
        Feil funksjonellFeil();

        @TekniskFeil(feilkode = "TEK_FEIL", feilmelding = "en teknisk feilmelding", logLevel = LogLevel.WARN)
        Feil tekniskFeil();

    }
}
