package no.nav.k9.sak.ytelse.beregning.tilbaketrekk;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAktivitetsnøkkel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.typer.Beløp;

public class KopierFeriepenger {
    private KopierFeriepenger() {
        // skjul public constructor
    }

    public static void kopierFraTil(Long behandlingId, BeregningsresultatEntitet fraResultat, BeregningsresultatEntitet tilResultat) {
        var bgFeriepengerPrÅrListe = fraResultat.getBeregningsresultatFeriepengerPrÅrListe();
        if (bgFeriepengerPrÅrListe == null || bgFeriepengerPrÅrListe.isEmpty()) {
            // ignorerer
            return;
        }

        tilResultat.setFeriepengerRegelInput(fraResultat.getFeriepengerRegelInput());
        tilResultat.setFeriepengerRegelSporing(fraResultat.getFeriepengerRegelSporing());

        bgFeriepengerPrÅrListe.forEach(prÅr -> {
            BeregningsresultatAndel bgAndel = prÅr.getBeregningsresultatAndel();
            LocalDate fom = bgAndel.getBeregningsresultatPeriode().getBeregningsresultatPeriodeFom();
            BeregningsresultatPeriode beregningsresultatPeriode = tilResultat.getBeregningsresultatPerioder().stream()
                .filter(brp -> brp.getBeregningsresultatPeriodeFom().equals(fom))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Fant ikke korresponderende utbet-periode for behandling " + behandlingId));

            List<BeregningsresultatAndel> haystack = beregningsresultatPeriode.getBeregningsresultatAndelList();

            BigDecimal feriepengerÅrsbeløp = prÅr.getÅrsbeløp().getVerdi();
            BeregningsresultatAndel utbetAndel = finnKorresponderendeAndel(haystack, bgAndel, bgAndel.erBrukerMottaker())
                .orElseGet(() -> BeregningsresultatAndel.builder(Kopimaskin.deepCopy(bgAndel) /* FIXME: bytt ut med copy ctor istdf. reflection her. */)
                    .medDagsats(0)
                    .medDagsatsFraBg(0)
                    .medPeriode(beregningsresultatPeriode.getPeriode())
                    .medFeriepengerÅrsbeløp(new Beløp(feriepengerÅrsbeløp))
                    .buildFor(beregningsresultatPeriode));

            BeregningsresultatFeriepengerPrÅr.builder()
                .medOpptjeningsår(prÅr.getOpptjeningsår())
                .medÅrsbeløp(feriepengerÅrsbeløp.longValue())
                .buildFor(utbetAndel);
        });
    }

    private static Optional<BeregningsresultatAndel> finnKorresponderendeAndel(List<BeregningsresultatAndel> haystack, BeregningsresultatAndel needle, boolean erBrukerMottaker) {
        BeregningsresultatAktivitetsnøkkel forrigeAndelAktivitetsnøkkel = needle.getAktivitetsnøkkel();
        List<BeregningsresultatAndel> korresponderendeAndeler = haystack.stream()
            .filter(andel -> andel.erBrukerMottaker() == erBrukerMottaker)
            .filter(andel -> Objects.equals(andel.getAktivitetsnøkkel(), forrigeAndelAktivitetsnøkkel))
            .collect(Collectors.toList());
        if (korresponderendeAndeler.size() > 1) {
            throw new IllegalArgumentException("Forventet å finne maks en korresponderende BeregningsresultatAndel " + forrigeAndelAktivitetsnøkkel
                + ". Antall matchende aktiviteter var " + korresponderendeAndeler.size());
        }
        return korresponderendeAndeler.stream().findFirst();
    }


}
