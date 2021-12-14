package no.nav.folketrygdloven.beregningsgrunnlag.resultat;

import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class RefusjonoverstyringPeriodeEndring {

    private Arbeidsgiver arbeidsgiver;

    private InternArbeidsforholdRef arbeidsforholdRef;

    private DatoEndring fastsattRefusjonFomEndring;

    private BeløpEndring fastsattDelvisRefusjonFørDatoEndring;

    public RefusjonoverstyringPeriodeEndring() {
    }

    public RefusjonoverstyringPeriodeEndring(Arbeidsgiver arbeidsgiver,
                                             InternArbeidsforholdRef arbeidsforholdRef,
                                             DatoEndring fastsattRefusjonFomEndring,
                                             BeløpEndring fastsattDelvisRefusjonFørDatoEndring) {
        this.arbeidsgiver = arbeidsgiver;
        this.arbeidsforholdRef = arbeidsforholdRef;
        this.fastsattRefusjonFomEndring = fastsattRefusjonFomEndring;
        this.fastsattDelvisRefusjonFørDatoEndring = fastsattDelvisRefusjonFørDatoEndring;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef;
    }

    public DatoEndring getFastsattRefusjonFomEndring() {
        return fastsattRefusjonFomEndring;
    }

    public BeløpEndring getFastsattDelvisRefusjonFørDatoEndring() {
        return fastsattDelvisRefusjonFørDatoEndring;
    }
}
