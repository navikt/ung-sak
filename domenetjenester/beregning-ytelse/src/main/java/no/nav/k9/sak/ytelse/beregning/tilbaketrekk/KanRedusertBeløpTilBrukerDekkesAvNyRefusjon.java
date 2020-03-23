package no.nav.k9.sak.ytelse.beregning.tilbaketrekk;

class KanRedusertBeløpTilBrukerDekkesAvNyRefusjon {

    private KanRedusertBeløpTilBrukerDekkesAvNyRefusjon() {
        // skjul public constructor
    }

    static boolean vurder(int endringIDagsatsBruker, int revurderingRefusjon) {
        boolean erEndringIDagsatsbruker = endringIDagsatsBruker < 0;
        boolean erEndringForBrukerMindreEnnNyRefusjon = Math.abs(endringIDagsatsBruker) <= revurderingRefusjon;
        boolean finnesNyRefusjon = revurderingRefusjon > 0;

        return erEndringIDagsatsbruker && erEndringForBrukerMindreEnnNyRefusjon && finnesNyRefusjon;
    }
}
