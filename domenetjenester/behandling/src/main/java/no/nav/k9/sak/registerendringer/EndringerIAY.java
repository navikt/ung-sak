package no.nav.k9.sak.registerendringer;

import java.util.ArrayList;
import java.util.List;

public class EndringerIAY {

    public static EndringerIAY INGEN_RELEVANTE_ENDRINGER = new EndringerIAY();

    private List<ArbeidsgiverEndring> ansattforholdEndringer = new ArrayList<>();
    private List<ArbeidsgiverEndring> inntektEndringer = new ArrayList<>();

    private EndringerIAY() {
    }

    public EndringerIAY(List<ArbeidsgiverEndring> ansattforholdEndringer, List<ArbeidsgiverEndring> inntektEndringer) {
        this.ansattforholdEndringer = ansattforholdEndringer;
        this.inntektEndringer = inntektEndringer;
    }

    public List<ArbeidsgiverEndring> getAnsattforholdEndringer() {
        return ansattforholdEndringer;
    }

    public List<ArbeidsgiverEndring> getInntektEndringer() {
        return inntektEndringer;
    }
}
