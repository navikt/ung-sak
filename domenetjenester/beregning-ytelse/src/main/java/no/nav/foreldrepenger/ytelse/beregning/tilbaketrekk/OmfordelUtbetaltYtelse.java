package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import no.nav.folketrygdloven.beregningsgrunnlag.Kopimaskin;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;

class OmfordelUtbetaltYtelse {
    private OmfordelUtbetaltYtelse() {
        // skjul public constructor
    }

    static List<BeregningsresultatAndel.Builder> omfordel(List<BeregningsresultatAndel> forrigeAndeler, List<BeregningsresultatAndel> bgAndeler) {

        List<BeregningsresultatAndel.Builder> list = new ArrayList<>();

        for (BeregningsresultatAndel bgAndel : bgAndeler) {// finn korresponderende andel
            int dagsats = beregnDagsats(forrigeAndeler, bgAndeler, bgAndel);
            if (dagsats == 0 && !bgAndel.erBrukerMottaker()) {
                continue;
            }
            BeregningsresultatAndel.Builder builder = BeregningsresultatAndel.builder(Kopimaskin.deepCopy(bgAndel))
                .medDagsats(dagsats)
                .medDagsatsFraBg(bgAndel.getDagsatsFraBg());
            list.add(builder);
        }
        return list;
    }

    private static int beregnDagsats(List<BeregningsresultatAndel> forrigeAndeler, List<BeregningsresultatAndel> bgAndeler, BeregningsresultatAndel bgAndel) {
        if (bgAndel.erBrukerMottaker()) {
            return beregnDagsatsBruker(forrigeAndeler, bgAndeler, bgAndel);
        }
        return beregnDagsatsArbeidsgiver(forrigeAndeler, bgAndeler, bgAndel);
    }

    private static int beregnDagsatsBruker(List<BeregningsresultatAndel> forrigeAndeler, List<BeregningsresultatAndel> bgAndeler, BeregningsresultatAndel bgAndel) {
        Optional<BeregningsresultatAndel> forrigeAndel = FinnKorresponderendeBeregningsresultatAndel.finn(forrigeAndeler, bgAndel, bgAndel.erBrukerMottaker());
        int forrigeDagsats = forrigeAndel.map(BeregningsresultatAndel::getDagsats).orElse(0);
        int bgDagsats = bgAndel.getDagsats();
        Optional<BeregningsresultatAndel> bgAndelBruker = FinnKorresponderendeBeregningsresultatAndel.finn(bgAndeler, bgAndel, true);
        int bgAndelBrukerDagsats = bgAndelBruker.map(BeregningsresultatAndel::getDagsats).orElse(0);

        int endringDagsatsBruker = bgAndelBrukerDagsats - forrigeDagsats;
        Optional<BeregningsresultatAndel> bgAndelArbeidsgiver = FinnKorresponderendeBeregningsresultatAndel.finn(bgAndeler, bgAndel, false);
        int bgDagsatsArbeidsgiver = bgAndelArbeidsgiver.map(BeregningsresultatAndel::getDagsats).orElse(0);

        if (KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(endringDagsatsBruker, bgDagsatsArbeidsgiver)) {
            if (bgDagsats <= forrigeDagsats) {
                return forrigeDagsats;
            } else {
                return bgDagsats;
            }
        } else {
            return bgDagsats;
        }
    }

    private static int beregnDagsatsArbeidsgiver(List<BeregningsresultatAndel> forrigeAndeler, List<BeregningsresultatAndel> bgAndeler, BeregningsresultatAndel bgAndel) {
        Optional<BeregningsresultatAndel> forrigeAndelBruker = FinnKorresponderendeBeregningsresultatAndel.finn(forrigeAndeler, bgAndel, true);
        int forrigeAndelBrukerDagsats = forrigeAndelBruker.map(BeregningsresultatAndel::getDagsats).orElse(0);
        Optional<BeregningsresultatAndel> bgAndelBruker = FinnKorresponderendeBeregningsresultatAndel.finn(bgAndeler, bgAndel, true);
        int bgAndelBrukerDagsats = bgAndelBruker.map(BeregningsresultatAndel::getDagsats).orElse(0);

        int endringDagsatsBruker = bgAndelBrukerDagsats - forrigeAndelBrukerDagsats;
        if (KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(endringDagsatsBruker, bgAndel.getDagsats())) {
            int bgAndelDagsats = bgAndelBrukerDagsats + bgAndel.getDagsats();
            return bgAndelDagsats - forrigeAndelBrukerDagsats;
        } else {
            return bgAndel.getDagsats();
        }
    }

}
