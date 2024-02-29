package no.nav.k9.sak.registerendringer;

import java.util.ArrayList;
import java.util.List;

public class EndringerIAY {

    public static EndringerIAY INGEN_RELEVANTE_ENDRINGER = new EndringerIAY();

    private List<Aktivitetsendringer> ansattforholdEndringer = new ArrayList<>();
    private List<Aktivitetsendringer> inntektEndringer = new ArrayList<>();

    private EndringerIAY() {
    }

    public EndringerIAY(List<Aktivitetsendringer> ansattforholdEndringer, List<Aktivitetsendringer> inntektEndringer) {
        this.ansattforholdEndringer = ansattforholdEndringer;
        this.inntektEndringer = inntektEndringer;
    }

    public List<Aktivitetsendringer> getAnsattforholdEndringer() {
        return ansattforholdEndringer;
    }

    public List<Aktivitetsendringer> getInntektEndringer() {
        return inntektEndringer;
    }
}
