package no.nav.folketrygdloven.beregningsgrunnlag.output;

import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;


public class ErTidsbegrensetArbeidsforholdEndring {

    private Arbeidsgiver arbeidsgiver;
    private InternArbeidsforholdRef arbeidsforholdRef;
    private ToggleEndring erTidsbegrensetArbeidsforholdEndring;

    public ErTidsbegrensetArbeidsforholdEndring(Arbeidsgiver arbeidsgiver,
                                                InternArbeidsforholdRef arbeidsforholdRef,
                                                ToggleEndring erTidsbegrensetArbeidsforholdEndring) {
        this.arbeidsgiver = arbeidsgiver;
        this.arbeidsforholdRef = arbeidsforholdRef;
        this.erTidsbegrensetArbeidsforholdEndring = erTidsbegrensetArbeidsforholdEndring;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef;
    }

    public ToggleEndring getErTidsbegrensetArbeidsforholdEndring() {
        return erTidsbegrensetArbeidsforholdEndring;
    }
}
