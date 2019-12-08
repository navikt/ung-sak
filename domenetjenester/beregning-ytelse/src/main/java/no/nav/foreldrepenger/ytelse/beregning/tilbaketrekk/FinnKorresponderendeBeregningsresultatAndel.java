package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAktivitetsnøkkel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;

public class FinnKorresponderendeBeregningsresultatAndel {
    private FinnKorresponderendeBeregningsresultatAndel() {
        // skjul public constructor
    }

    public static Optional<BeregningsresultatAndel> finn(List<BeregningsresultatAndel> haystack, BeregningsresultatAndel needle, boolean erBrukerMottaker) {
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
