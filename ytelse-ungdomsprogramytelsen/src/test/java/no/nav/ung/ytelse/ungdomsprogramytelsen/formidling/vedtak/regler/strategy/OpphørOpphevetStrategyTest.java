package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.vedtak.regler.strategy;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPerioder;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatInfo;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold.OpphørOpphevetInnholdBygger;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpphørOpphevetStrategyTest {

    private static final Long BEHANDLING_ID = 1000L;
    private static final Long ORIGINAL_BEHANDLING_ID = 999L;

    private final OpphørOpphevetInnholdBygger opphørOpphevetInnholdBygger = mock(OpphørOpphevetInnholdBygger.class);
    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository = mock(UngdomsprogramPeriodeRepository.class);
    private final OpphørOpphevetStrategy strategy = new OpphørOpphevetStrategy(opphørOpphevetInnholdBygger, ungdomsprogramPeriodeRepository);

    @Test
    void skal_sende_brev_når_originalbehandling_hadde_lukket_sluttdato() {
        var behandling = mockBehandlingMedOriginal(ORIGINAL_BEHANDLING_ID);
        mockOriginalGrunnlag(new UngdomsprogramPeriode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 15)));

        var resultat = strategy.evaluer(behandling, opphørOpphevetTidslinje());

        assertThat(resultat).hasSize(1);
    }

    @Test
    void skal_ikke_sende_brev_når_originalbehandling_fortsatt_hadde_åpen_sluttdato() {
        // Opphør og opphevelse er slått sammen på samme, fortsatt åpne behandling.
        // Opphøret ble dermed aldri faktisk vedtatt/iverksatt, og det finnes ikke noe
        // opphørsbrev for brukeren å oppheve.
        var behandling = mockBehandlingMedOriginal(ORIGINAL_BEHANDLING_ID);
        mockOriginalGrunnlag(new UngdomsprogramPeriode(LocalDate.of(2026, 1, 1), Tid.TIDENES_ENDE));

        var resultat = strategy.evaluer(behandling, opphørOpphevetTidslinje());

        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_ikke_sende_brev_når_original_behandling_mangler() {
        var behandling = mock(Behandling.class);
        when(behandling.getId()).thenReturn(BEHANDLING_ID);
        when(behandling.getOriginalBehandlingId()).thenReturn(Optional.empty());

        var resultat = strategy.evaluer(behandling, opphørOpphevetTidslinje());

        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_ikke_sende_brev_når_resultatet_ikke_inneholder_opphør_opphevet() {
        var behandling = mockBehandlingMedOriginal(ORIGINAL_BEHANDLING_ID);

        var resultat = strategy.evaluer(behandling, LocalDateTimeline.empty());

        assertThat(resultat).isEmpty();
    }

    private Behandling mockBehandlingMedOriginal(Long originalBehandlingId) {
        var behandling = mock(Behandling.class);
        when(behandling.getId()).thenReturn(BEHANDLING_ID);
        when(behandling.getOriginalBehandlingId()).thenReturn(Optional.of(originalBehandlingId));
        return behandling;
    }

    private void mockOriginalGrunnlag(UngdomsprogramPeriode periode) {
        var grunnlag = mock(UngdomsprogramPeriodeGrunnlag.class);
        var perioder = new UngdomsprogramPerioder(Set.of(periode));
        when(grunnlag.getUngdomsprogramPerioder()).thenReturn(perioder);
        when(ungdomsprogramPeriodeRepository.hentGrunnlag(ORIGINAL_BEHANDLING_ID)).thenReturn(Optional.of(grunnlag));
    }

    private LocalDateTimeline<DetaljertResultat> opphørOpphevetTidslinje() {
        var resultat = new DetaljertResultat(
            Set.of(DetaljertResultatInfo.of(DetaljertResultatType.OPPHØR_OPPHEVET)),
            Set.of(),
            Set.of(),
            Set.of());
        return new LocalDateTimeline<>(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), resultat);
    }

}
