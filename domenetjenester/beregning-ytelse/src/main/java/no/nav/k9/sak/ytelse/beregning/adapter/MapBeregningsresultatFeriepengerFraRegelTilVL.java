package no.nav.k9.sak.ytelse.beregning.adapter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Beløp;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;

public class MapBeregningsresultatFeriepengerFraRegelTilVL {

    private MapBeregningsresultatFeriepengerFraRegelTilVL() {
    }

    public static void mapTilResultatFraRegelModell(BeregningsresultatEntitet resultat, BeregningsresultatFeriepengerRegelModell regelModell) {
        regelModell.getBeregningsresultatPerioder().forEach(regelBeregningsresultatPeriode -> mapPeriode(resultat, regelBeregningsresultatPeriode));
    }

    private static void mapPeriode(BeregningsresultatEntitet resultat, no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatPeriode regelBeregningsresultatPeriode) {
        BeregningsresultatPeriode vlBeregningsresultatPeriode = resultat.getBeregningsresultatPerioder().stream()
            .filter(periode -> periode.getBeregningsresultatPeriodeFom().equals(regelBeregningsresultatPeriode.getFom()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Fant ikke BeregningsresultatPeriode"));
        regelBeregningsresultatPeriode.getBeregningsresultatAndelList().forEach(regelAndel -> mapAndel(vlBeregningsresultatPeriode, regelAndel));
    }

    private static void mapAndel(BeregningsresultatPeriode vlBeregningsresultatPeriode, no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatAndel regelAndel) {
        if (regelAndel.getBeregningsresultatFeriepengerPrÅrListe().isEmpty()) {
            return;
        }
        AktivitetStatus regelAndelAktivitetStatus = AktivitetStatusMapper.fraRegelTilVl(regelAndel);
        String regelArbeidsgiverId = regelAndel.getArbeidsforhold() == null ? null : regelAndel.getArbeidsgiverId();
        String regelArbeidsforholdId = regelAndel.getArbeidsforhold() != null ? regelAndel.getArbeidsforhold().getArbeidsforholdId() : null;
        BeregningsresultatAndel andel = vlBeregningsresultatPeriode.getBeregningsresultatAndelList().stream()
            .filter(vlAndel -> {
                String vlArbeidsforholdRef = vlAndel.getArbeidsforholdRef() == null ? null : vlAndel.getArbeidsforholdRef().getReferanse();
                return Objects.equals(vlAndel.getAktivitetStatus(), regelAndelAktivitetStatus)
                    && Objects.equals(vlAndel.getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElse(null), regelArbeidsgiverId)
                    && Objects.equals(vlArbeidsforholdRef, regelArbeidsforholdId)
                    && Objects.equals(vlAndel.erBrukerMottaker(), regelAndel.erBrukerMottaker());
            })
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Fant ikke " + regelAndel));
        regelAndel.getBeregningsresultatFeriepengerPrÅrListe()
            .stream()
            .filter(MapBeregningsresultatFeriepengerFraRegelTilVL::erAvrundetÅrsbeløpUlik0)
            .forEach(prÅr -> {
                BigDecimal feriepengerÅrsbeløp = prÅr.getÅrsbeløp().setScale(0, RoundingMode.HALF_UP);
                andel.setFeriepengerBeløp(new Beløp(feriepengerÅrsbeløp));
            });
    }

    private static boolean erAvrundetÅrsbeløpUlik0(no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerPrÅr prÅr) {
        long årsbeløp = prÅr.getÅrsbeløp().setScale(0, RoundingMode.HALF_UP).longValue();
        return årsbeløp != 0L;
    }
}
