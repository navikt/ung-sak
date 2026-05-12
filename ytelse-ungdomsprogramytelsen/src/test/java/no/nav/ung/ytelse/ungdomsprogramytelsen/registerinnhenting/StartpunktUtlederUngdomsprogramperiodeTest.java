package no.nav.ung.ytelse.ungdomsprogramytelsen.registerinnhenting;

import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPerioder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StartpunktUtlederUngdomsprogramperiodeTest {

    private static final Long ELDSTE_ID = 1L;
    private static final Long NYESTE_ID = 2L;
    private static final LocalDate FOM = LocalDate.of(2024, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2024, 12, 31);

    private UngdomsprogramPeriodeRepository repository;
    private StartpunktUtlederUngdomsprogramperiode utleder;
    private BehandlingReferanse ref;

    @BeforeEach
    void setUp() {
        repository = mock(UngdomsprogramPeriodeRepository.class);
        utleder = new StartpunktUtlederUngdomsprogramperiode(repository);
        ref = mock(BehandlingReferanse.class);
    }

    @Test
    void ingen_endringer_gir_udefinert() {
        var eldste = grunnlagMedForlengetPeriode(false);
        var nyeste = grunnlagMedForlengetPeriode(false);
        when(repository.hentGrunnlagBasertPåId(ELDSTE_ID)).thenReturn(Optional.of(eldste));
        when(repository.hentGrunnlagBasertPåId(NYESTE_ID)).thenReturn(Optional.of(nyeste));

        var resultat = utleder.utledStartpunkt(ref, NYESTE_ID, ELDSTE_ID);

        assertThat(resultat).isEqualTo(StartpunktType.UDEFINERT);
    }

    @Test
    void forlenget_periode_innvilget_første_gang_gir_init_perioder() {
        var eldste = grunnlagMedForlengetPeriode(false);
        var nyeste = grunnlagMedForlengetPeriode(true);
        when(repository.hentGrunnlagBasertPåId(ELDSTE_ID)).thenReturn(Optional.of(eldste));
        when(repository.hentGrunnlagBasertPåId(NYESTE_ID)).thenReturn(Optional.of(nyeste));

        var resultat = utleder.utledStartpunkt(ref, NYESTE_ID, ELDSTE_ID);

        assertThat(resultat).isEqualTo(StartpunktType.INIT_PERIODER);
    }

    @Test
    void forlenget_periode_allerede_innvilget_ingen_endring_gir_udefinert() {
        var eldste = grunnlagMedForlengetPeriode(true);
        var nyeste = grunnlagMedForlengetPeriode(true);
        when(repository.hentGrunnlagBasertPåId(ELDSTE_ID)).thenReturn(Optional.of(eldste));
        when(repository.hentGrunnlagBasertPåId(NYESTE_ID)).thenReturn(Optional.of(nyeste));

        var resultat = utleder.utledStartpunkt(ref, NYESTE_ID, ELDSTE_ID);

        assertThat(resultat).isEqualTo(StartpunktType.UDEFINERT);
    }

    @Test
    void forlenget_periode_tilbaketrukket_ignoreres_og_gir_udefinert() {
        // Forretningsregel: forlenget periode kan ikke fjernes etter innvilgelse.
        // Startpunktet skal ikke trigges selv om verdien mot formodning endres fra true til false.
        var eldste = grunnlagMedForlengetPeriode(true);
        var nyeste = grunnlagMedForlengetPeriode(false);
        when(repository.hentGrunnlagBasertPåId(ELDSTE_ID)).thenReturn(Optional.of(eldste));
        when(repository.hentGrunnlagBasertPåId(NYESTE_ID)).thenReturn(Optional.of(nyeste));

        var resultat = utleder.utledStartpunkt(ref, NYESTE_ID, ELDSTE_ID);

        assertThat(resultat).isEqualTo(StartpunktType.UDEFINERT);
    }

    @Test
    void endrede_perioder_uten_forlenget_periode_gir_init_perioder() {
        var eldste = grunnlagMedForlengetPeriodeOgPeriode(false, FOM, TOM);
        var nyeste = grunnlagMedForlengetPeriodeOgPeriode(false, FOM, TOM.plusDays(30));
        when(repository.hentGrunnlagBasertPåId(ELDSTE_ID)).thenReturn(Optional.of(eldste));
        when(repository.hentGrunnlagBasertPåId(NYESTE_ID)).thenReturn(Optional.of(nyeste));

        var resultat = utleder.utledStartpunkt(ref, NYESTE_ID, ELDSTE_ID);

        assertThat(resultat).isEqualTo(StartpunktType.INIT_PERIODER);
    }

    private UngdomsprogramPeriodeGrunnlag grunnlagMedForlengetPeriode(boolean harForlengetPeriode) {
        return grunnlagMedForlengetPeriodeOgPeriode(harForlengetPeriode, FOM, TOM);
    }

    private UngdomsprogramPeriodeGrunnlag grunnlagMedForlengetPeriodeOgPeriode(boolean harForlengetPeriode, LocalDate fom, LocalDate tom) {
        var grunnlag = mock(UngdomsprogramPeriodeGrunnlag.class);
        when(grunnlag.harForlengetPeriode()).thenReturn(harForlengetPeriode);
        var periode = mock(UngdomsprogramPeriode.class);
        when(periode.getPeriode()).thenReturn(no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        var perioder = mock(UngdomsprogramPerioder.class);
        when(perioder.getPerioder()).thenReturn(Set.of(periode));
        when(grunnlag.getUngdomsprogramPerioder()).thenReturn(perioder);
        return grunnlag;
    }
}

