package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.medisinsk.Pleiegrad;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell.LivetsSluttfaseDokumentasjon;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell.LivetsSluttfaseDokumentasjonPeriode;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell.MedisinskVilkårResultat;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell.PleiePeriode;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell.Pleielokasjon;

class PleiegradKalkulatorTest {

    LocalDate dag1 = LocalDate.now().minusDays(30);
    LocalDate dag2 = dag1.plusDays(1);
    LocalDate dag3 = dag1.plusDays(2);

    MedisinskVilkårResultat vilkårresultat = new MedisinskVilkårResultat();

    @Test
    void skal_ha_100prosent_pleiegrad_når_dokumentasjon_av_livets_sluttfase_er_OK() {
        vilkårresultat.setPleieperioder(List.of(
            new PleiePeriode(dag1, dag3, Pleielokasjon.HJEMME)));
        vilkårresultat.setDokumentasjonLivetsSluttfasePerioder(List.of(
            new LivetsSluttfaseDokumentasjonPeriode(dag1, dag2, LivetsSluttfaseDokumentasjon.DOKUMENTERT)));

        assertThat(PleiegradKalkulator.regnUtPleiegrad(vilkårresultat)).isEqualTo(new LocalDateTimeline<>(dag1, dag2, Pleiegrad.LIVETS_SLUTT_TILSYN));
        assertThat(Pleiegrad.LIVETS_SLUTT_TILSYN.getProsent()).isEqualTo(100);
    }

    @Test
    void skal_ha_ingen_pleiegrad_når_det_ikke_er_dokumentert_at_personen_er_i_livets_sluttfase() {
        vilkårresultat.setPleieperioder(List.of(
            new PleiePeriode(dag1, dag3, Pleielokasjon.HJEMME)));
        vilkårresultat.setDokumentasjonLivetsSluttfasePerioder(List.of(
            new LivetsSluttfaseDokumentasjonPeriode(dag1, dag2, LivetsSluttfaseDokumentasjon.IKKE_DOKUMENTERT)));

        assertThat(PleiegradKalkulator.regnUtPleiegrad(vilkårresultat)).isEqualTo(new LocalDateTimeline<>(dag1, dag2, Pleiegrad.INGEN));
        assertThat(Pleiegrad.INGEN.getProsent()).isZero();
    }

    @Test
    void skal_ha_ingen_pleiegrad_før_det_er_dokumentert_at_personen_er_i_livets_sluttfase() {
        vilkårresultat.setPleieperioder(List.of(
            new PleiePeriode(dag1, dag3, Pleielokasjon.HJEMME)));
        vilkårresultat.setDokumentasjonLivetsSluttfasePerioder(List.of(
            new LivetsSluttfaseDokumentasjonPeriode(dag1, dag1, LivetsSluttfaseDokumentasjon.IKKE_DOKUMENTERT),
            new LivetsSluttfaseDokumentasjonPeriode(dag2, dag2, LivetsSluttfaseDokumentasjon.DOKUMENTERT))
        );

        assertThat(PleiegradKalkulator.regnUtPleiegrad(vilkårresultat)).isEqualTo(new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(dag1, dag1, Pleiegrad.INGEN),
            new LocalDateSegment<>(dag2, dag2, Pleiegrad.LIVETS_SLUTT_TILSYN)
        )));
    }

    @Test
    void skal_ha_ingen_pleiegrad_når_personen_er_innlagt() {
        vilkårresultat.setDokumentasjonLivetsSluttfasePerioder(List.of(
            new LivetsSluttfaseDokumentasjonPeriode(dag1, dag3, LivetsSluttfaseDokumentasjon.DOKUMENTERT)));
        vilkårresultat.setPleieperioder(List.of(
            new PleiePeriode(dag1, dag1, Pleielokasjon.HJEMME),
            new PleiePeriode(dag2, dag2, Pleielokasjon.INNLAGT),
            new PleiePeriode(dag3, dag3, Pleielokasjon.HJEMME)));

        assertThat(PleiegradKalkulator.regnUtPleiegrad(vilkårresultat)).isEqualTo(new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(dag1, dag1, Pleiegrad.LIVETS_SLUTT_TILSYN),
            new LocalDateSegment<>(dag2, dag2, Pleiegrad.INGEN),
            new LocalDateSegment<>(dag3, dag3, Pleiegrad.LIVETS_SLUTT_TILSYN)
        )));
    }
}
