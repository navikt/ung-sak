package no.nav.k9.sak.web.app.tjenester.opplæringsinstitusjon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;
import no.nav.k9.sak.kontrakt.opplæringspenger.godkjentopplaeringsinstitusjon.GodkjentOpplæringsinstitusjonDto;
import no.nav.k9.sak.kontrakt.opplæringspenger.godkjentopplaeringsinstitusjon.GodkjentOpplæringsinstitusjonIdDto;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon.GodkjentOpplæringsinstitusjonTjeneste;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.godkjentopplaeringsinstitusjon.GodkjentOpplæringsinstitusjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.godkjentopplaeringsinstitusjon.GodkjentOpplæringsinstitusjonPeriode;

public class GodkjentOpplæringsinstitusjonRestTjenesteTest {

    private GodkjentOpplæringsinstitusjonTjeneste godkjentOpplæringsinstitusjonTjeneste;

    private GodkjentOpplæringsinstitusjonRestTjeneste restTjeneste;

    @BeforeEach
    public void setup() {
        godkjentOpplæringsinstitusjonTjeneste = mock(GodkjentOpplæringsinstitusjonTjeneste.class);
        restTjeneste = new GodkjentOpplæringsinstitusjonRestTjeneste(godkjentOpplæringsinstitusjonTjeneste);
    }

    @Test
    public void skalHenteMedUuid() {
        UUID uuid = UUID.randomUUID();
        GodkjentOpplæringsinstitusjonPeriode periode = new GodkjentOpplæringsinstitusjonPeriode(null, null);
        GodkjentOpplæringsinstitusjon entity = new GodkjentOpplæringsinstitusjon(uuid, "navn", List.of(periode));
        when(godkjentOpplæringsinstitusjonTjeneste.hentMedUuid(uuid)).thenReturn(Optional.of(entity));

        Response response = restTjeneste.hentMedUuid(new GodkjentOpplæringsinstitusjonIdDto(uuid));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        GodkjentOpplæringsinstitusjonDto result = (GodkjentOpplæringsinstitusjonDto) response.getEntity();
        assertThat(result).isNotNull();
        assertThat(result.getUuid()).isEqualTo(entity.getUuid());
        assertThat(result.getNavn()).isEqualTo(entity.getNavn());
        assertThat(result.getPerioder()).hasSize(1);
        assertThat(result.getPerioder().get(0).getFom()).isEqualTo(periode.getPeriode().getFomDato());
        assertThat(result.getPerioder().get(0).getTom()).isEqualTo(periode.getPeriode().getTomDato());
    }

    @Test
    public void hentMedUuidTomtResultat() {
        Response response = restTjeneste.hentMedUuid(new GodkjentOpplæringsinstitusjonIdDto(UUID.randomUUID()));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(204);
        assertThat(response.getEntity()).isNull();
    }

    @Test
    public void skalHenteAlle() {
        GodkjentOpplæringsinstitusjon entity1 = new GodkjentOpplæringsinstitusjon(UUID.randomUUID(), "her", null, null);
        GodkjentOpplæringsinstitusjon entity2 = new GodkjentOpplæringsinstitusjon(UUID.randomUUID(), "der", null, null);
        when(godkjentOpplæringsinstitusjonTjeneste.hentAlle()).thenReturn(Arrays.asList(entity1, entity2));

        Response response = restTjeneste.hentAlle();
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        List<GodkjentOpplæringsinstitusjonDto> result = (List<GodkjentOpplæringsinstitusjonDto>) response.getEntity();
        assertThat(result).hasSize(2);
    }

    @Test
    public void skalHenteAktivMedUuid() {
        UUID uuid = UUID.randomUUID();
        Periode idag = new Periode(LocalDate.now(), LocalDate.now());
        GodkjentOpplæringsinstitusjonPeriode periode = new GodkjentOpplæringsinstitusjonPeriode(idag.getFom(), idag.getTom());
        GodkjentOpplæringsinstitusjon entity = new GodkjentOpplæringsinstitusjon(uuid, "navn", List.of(periode));
        when(godkjentOpplæringsinstitusjonTjeneste.hentAktivMedUuid(uuid, idag)).thenReturn(Optional.of(entity));

        Response response = restTjeneste.hentAktivMedUuid(new GodkjentOpplæringsinstitusjonIdDto(uuid), idag);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        GodkjentOpplæringsinstitusjonDto result = (GodkjentOpplæringsinstitusjonDto) response.getEntity();
        assertThat(result).isNotNull();
        assertThat(result.getUuid()).isEqualTo(entity.getUuid());
        assertThat(result.getNavn()).isEqualTo(entity.getNavn());
        assertThat(result.getPerioder()).hasSize(1);
        assertThat(result.getPerioder().get(0).getFom()).isEqualTo(periode.getPeriode().getFomDato());
        assertThat(result.getPerioder().get(0).getTom()).isEqualTo(periode.getPeriode().getTomDato());
    }

    @Test
    public void hentAktivMedUuidTomtResultat() {
        Response response = restTjeneste.hentAktivMedUuid(new GodkjentOpplæringsinstitusjonIdDto(UUID.randomUUID()), new Periode(LocalDate.now(), LocalDate.now()));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(204);
        assertThat(response.getEntity()).isNull();
    }

    @Test
    public void skalHenteAktive() {
        Periode idag = new Periode(LocalDate.now(), LocalDate.now());
        GodkjentOpplæringsinstitusjon entity1 = new GodkjentOpplæringsinstitusjon(UUID.randomUUID(), "her", null, null);
        GodkjentOpplæringsinstitusjon entity2 = new GodkjentOpplæringsinstitusjon(UUID.randomUUID(), "der", null, null);
        when(godkjentOpplæringsinstitusjonTjeneste.hentAktive(idag)).thenReturn(Arrays.asList(entity1, entity2));

        Response response = restTjeneste.hentAktive(idag);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        List<GodkjentOpplæringsinstitusjonDto> result = (List<GodkjentOpplæringsinstitusjonDto>) response.getEntity();
        assertThat(result).hasSize(2);
    }

    @Test
    public void erAktiv() {
        UUID uuid = UUID.randomUUID();
        Periode idag = new Periode(LocalDate.now(), LocalDate.now());
        GodkjentOpplæringsinstitusjon entity = new GodkjentOpplæringsinstitusjon(uuid, "navn", idag.getFom(), idag.getTom());
        when(godkjentOpplæringsinstitusjonTjeneste.hentAktivMedUuid(uuid, idag)).thenReturn(Optional.of(entity));

        Boolean response = restTjeneste.erAktiv(new GodkjentOpplæringsinstitusjonIdDto(uuid), idag);
        assertThat(response).isNotNull();
        assertThat(response).isTrue();
    }
}
