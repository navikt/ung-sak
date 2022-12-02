package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningAktiviteter;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.typer.Periode;

class SkalVentePåRegelendring_8_41Test {
    private static final LocalDate REGELENDRING = LocalDate.of(2023,1,1);

    @Test
    public void skalIkkeSettePåVentMedTidligSTP() {
        var arbeid = OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID,
            new Periode(førEndring(300), førEndring(10)), "999999999");
        var næring = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.NÆRING,
            new Periode(førEndring(300), etterEndring(10)));
        var opptjening = Arrays.asList(arbeid, næring);
        var skalVente = SkalVentePåRegelendring_8_41.kanPåvirkesAvRegelendring(førEndring(50), opptjening);
        assertThat(skalVente).isFalse();
    }

    @Test
    public void skalIkkeVenteNårIngenRelevantStatus() {
        var dagpenger = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.DAGPENGER,
            new Periode(førEndring(300), etterEndring(100)));
        var opptjening = Arrays.asList(dagpenger);
        var skalVente = SkalVentePåRegelendring_8_41.kanPåvirkesAvRegelendring(etterEndring(10), opptjening);
        assertThat(skalVente).isFalse();
    }

    @Test
    public void skalIkkeVenteNårIngenAktivitetFremTilSTP() {
        var arbeid = OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID,
            new Periode(førEndring(300), førEndring(10)), "999999999");
        var næring = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.NÆRING,
            new Periode(førEndring(300), førEndring(10)));
        var opptjening = Arrays.asList(arbeid, næring);
        var skalVente = SkalVentePåRegelendring_8_41.kanPåvirkesAvRegelendring(etterEndring(10), opptjening);
        assertThat(skalVente).isFalse();
    }

    @Test
    public void skalVenteNårNæringOgArbeid() {
        var arbeid = OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID,
            new Periode(førEndring(300), etterEndring(10)), "999999999");
        var næring = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.NÆRING,
            new Periode(førEndring(300), etterEndring(10)));
        var opptjening = Arrays.asList(arbeid, næring);
        var skalVente = SkalVentePåRegelendring_8_41.kanPåvirkesAvRegelendring(etterEndring(10), opptjening);
        assertThat(skalVente).isTrue();
    }

    @Test
    public void skalVenteNårNæringOgFrilans() {
        var frilans = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.FRILANS,
            new Periode(førEndring(300), etterEndring(10)));
        var næring = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.NÆRING,
            new Periode(førEndring(300), etterEndring(10)));
        var opptjening = Arrays.asList(frilans, næring);
        var skalVente = SkalVentePåRegelendring_8_41.kanPåvirkesAvRegelendring(etterEndring(10), opptjening);
        assertThat(skalVente).isTrue();
    }

    @Test
    public void skalIkkeVenteNårKunNæring() {
        var næring = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.NÆRING,
            new Periode(førEndring(300), etterEndring(10)));
        var opptjening = Arrays.asList(næring);
        var skalVente = SkalVentePåRegelendring_8_41.kanPåvirkesAvRegelendring(etterEndring(10), opptjening);
        assertThat(skalVente).isFalse();
    }

    private LocalDate førEndring(int dager) {
        return REGELENDRING.minusDays(dager);
    }

    private LocalDate etterEndring(int dager) {
        return REGELENDRING.plusMonths(dager);
    }
}
