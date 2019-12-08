package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAktivitetsnøkkel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

public class VurderBehovForÅHindreTilbaketrekk {

    private VurderBehovForÅHindreTilbaketrekk() {
        // Skjuler default
    }


    /**
     * @deprecated Erstattes av VurderBehovForÅHindreTilbaketrekkV2 #skalVurdereTilbaketrekk.
     */
    @Deprecated
    public static boolean skalVurdereTilbaketrekk(LocalDateTimeline<BRAndelSammenligning> brAndelTidslinje) {

        // hvis endring i totalDagsats
        for (LocalDateSegment<BRAndelSammenligning> segment : brAndelTidslinje.toSegments()) {
            BRAndelSammenligning sammenligning = segment.getValue();
            List<BeregningsresultatAndel> forrigeAndeler = sammenligning.getForrigeAndeler();
            List<BeregningsresultatAndel> bgAndeler = sammenligning.getBgAndeler();

            List<BeregningsresultatAndel> forrigeBrukerAndeler = forrigeAndeler.stream()
                .filter(BeregningsresultatAndel::erBrukerMottaker)
                .collect(Collectors.toList());

            for (BeregningsresultatAndel forrigeBrukerAndel : forrigeBrukerAndeler) {
                Optional<BeregningsresultatAndel> bgBrukerAndel = finnKorresponderendeAndeler(bgAndeler, forrigeBrukerAndel, true);
                Optional<BeregningsresultatAndel> bgArbeidsgiverAndel = finnKorresponderendeAndeler(bgAndeler, forrigeBrukerAndel, false);
                int bgDagsatsBruker = dagsats(bgBrukerAndel);
                int bgDagsatsArbeidsgiver = dagsats(bgArbeidsgiverAndel);
                int endringIDagsatsBruker = bgDagsatsBruker - forrigeBrukerAndel.getDagsats();

                boolean skalStoppes = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                    endringIDagsatsBruker,
                    bgDagsatsArbeidsgiver
                );
                if (skalStoppes) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int dagsats(Optional<BeregningsresultatAndel> andel) {
        return andel.map(BeregningsresultatAndel::getDagsats).orElse(0);
    }

    private static Optional<BeregningsresultatAndel> finnKorresponderendeAndeler(List<BeregningsresultatAndel> haystack, BeregningsresultatAndel needle, boolean erBrukerMottaker) {
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


