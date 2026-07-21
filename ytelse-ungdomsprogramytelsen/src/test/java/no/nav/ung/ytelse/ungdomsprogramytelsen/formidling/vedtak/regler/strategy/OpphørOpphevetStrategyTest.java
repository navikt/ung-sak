package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.vedtak.regler.strategy;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrevÅrsakType;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatInfo;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold.OpphørOpphevetInnholdBygger;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Skillet mellom «opphevet» (opphør faktisk vedtatt i tidligere behandling) og «avbrutt i samme behandling»
 * (opphør og opphevelse slått sammen på samme, fortsatt åpne behandling) avgjøres av
 * {@code UngDetaljertResultatTidslinjeUtleder} og reflekteres i {@link DetaljertResultatType}. Denne strategien
 * trenger derfor bare å lese resultattypen direkte, ikke gjøre egen utledning.
 */
class OpphørOpphevetStrategyTest {

    private final OpphørOpphevetInnholdBygger opphørOpphevetInnholdBygger = mock(OpphørOpphevetInnholdBygger.class);
    private final OpphørOpphevetStrategy strategy = new OpphørOpphevetStrategy(opphørOpphevetInnholdBygger);

    @Test
    void skal_sende_brev_når_resultatet_er_opphør_opphevet() {
        var resultat = strategy.evaluer(mockBehandling(), tidslinjeMed(DetaljertResultatType.OPPHØR_OPPHEVET));

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).bygger()).isNotNull();
    }

    @Test
    void skal_ikke_sende_brev_men_returnere_eksplisitt_resultat_når_opphør_ble_avbrutt_i_samme_behandling() {
        // Vi må returnere et eksplisitt "ingen brev"-resultat (ikke tom liste), ellers tolkes perioden
        // som IKKE_IMPLEMENTERT og gir feilaktig aksjonspunkt om manuell fatting av vedtak.
        var resultat = strategy.evaluer(mockBehandling(), tidslinjeMed(DetaljertResultatType.OPPHØR_MOTTATT_OG_AVBRUTT_I_SAMME_BEHANDLING));

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).bygger()).isNull();
        assertThat(resultat.get(0).ingenBrevÅrsakType()).isEqualTo(IngenBrevÅrsakType.IKKE_RELEVANT);
    }

    @Test
    void skal_ikke_sende_brev_når_resultatet_ikke_inneholder_opphør_opphevet_eller_avbrutt() {
        var resultat = strategy.evaluer(mockBehandling(), LocalDateTimeline.empty());

        assertThat(resultat).isEmpty();
    }

    private Behandling mockBehandling() {
        return mock(Behandling.class);
    }

    private LocalDateTimeline<DetaljertResultat> tidslinjeMed(DetaljertResultatType type) {
        var resultat = new DetaljertResultat(
            Set.of(DetaljertResultatInfo.of(type)),
            Set.of(),
            Set.of(),
            Set.of());
        return new LocalDateTimeline<>(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), resultat);
    }

}
