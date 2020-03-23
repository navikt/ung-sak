package no.nav.folketrygdloven.beregningsgrunnlag.output;

import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;


public class ErMottattYtelseEndring {

    private AktivitetStatus aktivitetStatus;
    private Arbeidsgiver arbeidsgiver;
    private InternArbeidsforholdRef arbeidsforholdRef;
    private ToggleEndring erMottattYtelseEndring;

    public ErMottattYtelseEndring(AktivitetStatus aktivitetStatus,
                                  Arbeidsgiver arbeidsgiver,
                                  InternArbeidsforholdRef arbeidsforholdRef,
                                  ToggleEndring erMottattYtelseEndring) {
        this.aktivitetStatus = aktivitetStatus;
        this.arbeidsgiver = arbeidsgiver;
        this.arbeidsforholdRef = arbeidsforholdRef;
        this.erMottattYtelseEndring = erMottattYtelseEndring;
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef;
    }

    public ToggleEndring getErMottattYtelseEndring() {
        return erMottattYtelseEndring;
    }
}
