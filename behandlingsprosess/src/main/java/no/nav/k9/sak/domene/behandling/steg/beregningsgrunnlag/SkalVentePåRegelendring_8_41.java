package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;


// Klasse som utleder om en sak skal sette på vent fordi den kan bli påvirket
// av regelendring på 8-41 og behandles før regelendring er fattet

import java.time.LocalDate;
import java.util.List;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningAktiviteter;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.typer.Periode;

public class SkalVentePåRegelendring_8_41 {
    private static final LocalDate DATO_FOR_REGELENDRING = LocalDate.of(2023,1,1);

    private SkalVentePåRegelendring_8_41() {
        // Skjuler default konstruktør
    }

    public static boolean kanPåvirkesAvRegelendring(LocalDate skjæringstidspunkt,
                                                    List<OpptjeningAktiviteter.OpptjeningPeriode> opptjeningsperioder) {
        if (opptjeningsperioder.isEmpty() || skjæringstidspunkt == null) {
            return false;
        }
        if (skjæringstidspunkt.isBefore(DATO_FOR_REGELENDRING)) {
            return false;
        }
        var beregningstidspunkt = skjæringstidspunkt.minusDays(1);
        var erSN = harAktivitetPåDato(beregningstidspunkt, opptjeningsperioder, OpptjeningAktivitetType.NÆRING);
        var erAT = harAktivitetPåDato(beregningstidspunkt, opptjeningsperioder, OpptjeningAktivitetType.ARBEID);
        var erFL = harAktivitetPåDato(beregningstidspunkt, opptjeningsperioder, OpptjeningAktivitetType.FRILANS);
        return erSN && (erAT || erFL);
    }

    private static boolean harAktivitetPåDato(LocalDate beregningstidspunkt,
                                              List<OpptjeningAktiviteter.OpptjeningPeriode> opptjeningPerioder,
                                              OpptjeningAktivitetType aktivitetType) {
            return opptjeningPerioder.stream()
                .anyMatch(opp -> opp.getPeriode().overlaps(new Periode(beregningstidspunkt, beregningstidspunkt))
                    && opp.getOpptjeningAktivitetType().equals(aktivitetType));
    }
}
