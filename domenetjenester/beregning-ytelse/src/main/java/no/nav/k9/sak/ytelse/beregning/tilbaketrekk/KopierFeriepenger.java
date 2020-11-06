package no.nav.k9.sak.ytelse.beregning.tilbaketrekk;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;

public class KopierFeriepenger {
    private KopierFeriepenger() {
        // skjul public constructor
    }

    public static void kopierFraTil(Long behandlingId, BeregningsresultatEntitet fraResultat, BeregningsresultatEntitet tilResultat) {
        var bgFeriepengerPrÅrListe = fraResultat.getBeregningsresultatAndelTimeline();
        if (bgFeriepengerPrÅrListe == null || bgFeriepengerPrÅrListe.isEmpty()) {
            // ignorerer
            return;
        }

        tilResultat.setFeriepengerRegelInput(fraResultat.getFeriepengerRegelInput());
        tilResultat.setFeriepengerRegelSporing(fraResultat.getFeriepengerRegelSporing());

        bgFeriepengerPrÅrListe.forEach(prÅr -> {
            for (var bgAndel : prÅr.getValue()) {
                LocalDate fom = bgAndel.getFom();
                var beregningsresultatPeriode = tilResultat.getBeregningsresultatPerioder().stream()
                    .filter(brp -> brp.getBeregningsresultatPeriodeFom().equals(fom))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Fant ikke korresponderende utbet-periode for behandling " + behandlingId));

                var haystack = beregningsresultatPeriode.getBeregningsresultatAndelList();

                var feriepengerÅrsbeløp = bgAndel.getFeriepengerÅrsbeløp();
                @SuppressWarnings("unused")
                var utbetAndel = finnKorresponderendeAndel(haystack, bgAndel, bgAndel.erBrukerMottaker())
                    .orElseGet(() -> BeregningsresultatAndel.builder(new BeregningsresultatAndel(bgAndel))
                        .medDagsats(0)
                        .medDagsatsFraBg(0)
                        .medPeriode(beregningsresultatPeriode.getPeriode())
                        .medFeriepengerÅrsbeløp(feriepengerÅrsbeløp)
                        .buildFor(beregningsresultatPeriode));
            }
        });
    }

    private static Optional<BeregningsresultatAndel> finnKorresponderendeAndel(List<BeregningsresultatAndel> haystack, BeregningsresultatAndel needle, boolean erBrukerMottaker) {
        var forrigeAndelAktivitetsnøkkel = needle.getAktivitetsnøkkel();
        var korresponderendeAndeler = haystack.stream()
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
