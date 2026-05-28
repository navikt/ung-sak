package no.nav.ung.sak.web.app.exceptions;

import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.exception.ManglerTilgangException;
import no.nav.k9.felles.exception.VLException;
import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.FunksjonellFeil;
import no.nav.k9.felles.feil.deklarasjon.ManglerTilgangFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;
import no.nav.k9.felles.sikkerhet.abac.PepNektetTilgangException;
import no.nav.k9.felles.sikkerhet.abac.ÅrsakIkkeTilgang;
import no.nav.ung.sak.kontrakt.FeilDto;
import no.nav.ung.sak.kontrakt.FeilType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class ManglerTilgangExceptionMapperTest {


    @Test
    public void skalMappeManglerTilgangFeil() {
        Feil manglerTilgangFeil = TestFeil.FACTORY.manglerTilgangFeil();

        @SuppressWarnings("resource")
        Response response = new ManglerTilgangExceptionMapper().toResponse((ManglerTilgangException) manglerTilgangFeil.toException());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getEntity()).isInstanceOf(FeilDto.class);
        FeilDto feilDto = (FeilDto) response.getEntity();

        assertThat(feilDto.getType()).isEqualTo(FeilType.MANGLER_TILGANG_FEIL);
        assertThat(feilDto.getFeilmelding()).isEqualTo("ManglerTilgangFeilmeldingKode");
    }

    @Test
    void skal_ta_med_årsaker_i_DTO_ved_ikke_tilgang_exception_som_har_årsak() {
        PepNektetTilgangException exception = new PepNektetTilgangException(Set.of(ÅrsakIkkeTilgang.HAR_IKKE_TILGANG_TIL_KODE6_PERSON, ÅrsakIkkeTilgang.HAR_IKKE_TILGANG_TIL_HISTORISK_SAK));

        Response response = new ManglerTilgangExceptionMapper().toResponse(exception);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getEntity()).isInstanceOf(FeilDto.class);
        FeilDto feilDto = (FeilDto) response.getEntity();

        assertThat(feilDto.getType()).isEqualTo(FeilType.MANGLER_TILGANG_FEIL);
        assertThat(feilDto.getFeilmelding()).isEqualTo("Ikke tilgang");
        assertThat(feilDto.getIkkeTilgangÅrsaker()).isEqualTo(Set.of(ÅrsakIkkeTilgang.HAR_IKKE_TILGANG_TIL_KODE6_PERSON, ÅrsakIkkeTilgang.HAR_IKKE_TILGANG_TIL_HISTORISK_SAK));
    }

    interface TestFeil extends DeklarerteFeil {
        TestFeil FACTORY = FeilFactory.create(TestFeil.class); // NOSONAR ok med konstant i interface her

        @ManglerTilgangFeil(feilkode = "MANGLER_TILGANG_FEIL", feilmelding = "ManglerTilgangFeilmeldingKode", logLevel = LogLevel.WARN)
        Feil manglerTilgangFeil();
    }
}
