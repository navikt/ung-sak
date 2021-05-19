package no.nav.k9.sak.web.app.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import no.nav.k9.felles.exception.ManglerTilgangException;
import no.nav.k9.felles.exception.VLException;
import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.FunksjonellFeil;
import no.nav.k9.felles.feil.deklarasjon.ManglerTilgangFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;
import no.nav.k9.sak.kontrakt.FeilDto;
import no.nav.k9.sak.kontrakt.FeilType;

public class GeneralRestExceptionMapperTest {


    @Test
    public void skalMappeManglerTilgangFeil() {
        Feil manglerTilgangFeil = TestFeil.FACTORY.manglerTilgangFeil();

        Response response = new ManglerTilgangExceptionMapper().toResponse((ManglerTilgangException) manglerTilgangFeil.toException());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getEntity()).isInstanceOf(FeilDto.class);
        FeilDto feilDto = (FeilDto) response.getEntity();

        assertThat(feilDto.getType()).isEqualTo(FeilType.MANGLER_TILGANG_FEIL);
        assertThat(feilDto.getFeilmelding()).isEqualTo("ManglerTilgangFeilmeldingKode");
    }

    @Test
    public void skalMappeFunksjonellFeil() {
        Feil funksjonellFeil = TestFeil.FACTORY.funksjonellFeil();

        Response response = new VLExceptionMapper().toResponse((VLException) funksjonellFeil.toException());

        assertThat(response.getEntity()).isInstanceOf(FeilDto.class);
        FeilDto feilDto = (FeilDto) response.getEntity();

        assertThat(feilDto.getFeilmelding()).contains("FUNK_FEIL");
        assertThat(feilDto.getFeilmelding()).contains("en funksjonell feilmelding");
        assertThat(feilDto.getFeilmelding()).contains("et løsningsforslag");
    }

    @Test
    public void skalMappeVLException() {
        VLException vlException = TestFeil.FACTORY.tekniskFeil().toException();

        Response response = new VLExceptionMapper().toResponse(vlException);

        assertThat(response.getEntity()).isInstanceOf(FeilDto.class);
        FeilDto feilDto = (FeilDto) response.getEntity();

        assertThat(feilDto.getFeilmelding()).contains("TEK_FEIL");
        assertThat(feilDto.getFeilmelding()).contains("en teknisk feilmelding");
    }

    @Test
    public void skalMappeGenerellFeil() {
        String feilmelding = "en helt generell feil";

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

        @ManglerTilgangFeil(feilkode = "MANGLER_TILGANG_FEIL", feilmelding = "ManglerTilgangFeilmeldingKode", logLevel = LogLevel.WARN)
        Feil manglerTilgangFeil();
    }
}
