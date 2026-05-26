package no.nav.ung.ytelse.ungdomsprogramytelsen.registerinnhenting;

import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPerioder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
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
    private static final LocalDate MAKS_DATO = LocalDate.of(2024, 12, 31);

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
        var eldste = grunnlagMedMaksDato(MAKS_DATO);
        var nyeste = grunnlagMedMaksDato(MAKS_DATO);
        when(repository.hentGrunnlagBasertPåId(ELDSTE_ID)).thenReturn(Optional.of(eldste));
        when(repository.hentGrunnlagBasertPåId(NYESTE_ID)).thenReturn(Optional.of(nyeste));

        var resultat = utleder.utledStartpunkt(ref, NYESTE_ID, ELDSTE_ID);

        assertThat(resultat).isEqualTo(StartpunktType.UDEFINERT);
    }

    @Test
    void økt_maksdato_gir_init_perioder() {
        var eldste = grunnlagMedMaksDato(MAKS_DATO);
        var nyeste = grunnlagMedMaksDato(MAKS_DATO.plusDays(28));
        when(repository.hentGrunnlagBasertPåId(ELDSTE_ID)).thenReturn(Optional.of(eldste));
        when(repository.hentGrunnlagBasertPåId(NYESTE_ID)).thenReturn(Optional.of(nyeste));

        var resultat = utleder.utledStartpunkt(ref, NYESTE_ID, ELDSTE_ID);

        assertThat(resultat).isEqualTo(StartpunktType.INIT_PERIODER);
    }

    @Test
    void maksdato_satt_for_første_gang_gir_init_perioder() {
        var eldste = grunnlagMedMaksDato(null);
        var nyeste = grunnlagMedMaksDato(MAKS_DATO);
        when(repository.hentGrunnlagBasertPåId(ELDSTE_ID)).thenReturn(Optional.of(eldste));
        when(repository.hentGrunnlagBasertPåId(NYESTE_ID)).thenReturn(Optional.of(nyeste));

        var resultat = utleder.utledStartpunkt(ref, NYESTE_ID, ELDSTE_ID);

        assertThat(resultat).isEqualTo(StartpunktType.INIT_PERIODER);
    }

    @Test
    void redusert_maksdato_gir_udefinert() {
        // Forretningsregel: maksdato kan ikke reduseres. Hvis det likevel skjer, ignorer.
        var eldste = grunnlagMedMaksDato(MAKS_DATO);
        var nyeste = grunnlagMedMaksDato(MAKS_DATO.minusDays(10));
        when(repository.hentGrunnlagBasertPåId(ELDSTE_ID)).thenReturn(Optional.of(eldste));
        when(repository.hentGrunnlagBasertPåId(NYESTE_ID)).thenReturn(Optional.of(nyeste));

        var resultat = utleder.utledStartpunkt(ref, NYESTE_ID, ELDSTE_ID);

        assertThat(resultat).isEqualTo(StartpunktType.UDEFINERT);
    }

    @Test
    void endrede_perioder_uten_endret_maksdato_gir_init_perioder() {
        var eldste = grunnlagMedMaksDatoOgPeriode(MAKS_DATO, FOM, TOM);
        var nyeste = grunnlagMedMaksDatoOgPeriode(MAKS_DATO, FOM, TOM.plusDays(30));
        when(repository.hentGrunnlagBasertPåId(ELDSTE_ID)).thenReturn(Optional.of(eldste));
        when(repository.hentGrunnlagBasertPåId(NYESTE_ID)).thenReturn(Optional.of(nyeste));

        var resultat = utleder.utledStartpunkt(ref, NYESTE_ID, ELDSTE_ID);

        assertThat(resultat).isEqualTo(StartpunktType.INIT_PERIODER);
    }

    private UngdomsprogramPeriodeGrunnlag grunnlagMedMaksDato(LocalDate maksDato) {
        return grunnlagMedMaksDatoOgPeriode(maksDato, FOM, TOM);
    }

    private UngdomsprogramPeriodeGrunnlag grunnlagMedMaksDatoOgPeriode(LocalDate maksDato, LocalDate fom, LocalDate tom) {
        var grunnlag = mock(UngdomsprogramPeriodeGrunnlag.class);
        when(grunnlag.getPeriodeMaksDato()).thenReturn(Optional.ofNullable(maksDato));
        var periode = mock(UngdomsprogramPeriode.class);
        when(periode.getPeriode()).thenReturn(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        var perioder = mock(UngdomsprogramPerioder.class);
        when(perioder.getPerioder()).thenReturn(Set.of(periode));
        when(grunnlag.getUngdomsprogramPerioder()).thenReturn(perioder);
        return grunnlag;
    }
}
