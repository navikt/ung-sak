package no.nav.folketrygdloven.beregningsgrunnlag.output;


import java.util.Optional;

import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class BeregningsgrunnlagPrStatusOgAndelEndring {

    private InntektEndring inntektEndring;
    private InntektskategoriEndring inntektskategoriEndring;
    private AktivitetStatus aktivitetStatus;
    private OpptjeningAktivitetType arbeidsforholdType;
    private Arbeidsgiver arbeidsgiver;
    private InternArbeidsforholdRef arbeidsforholdRef;

    public BeregningsgrunnlagPrStatusOgAndelEndring(InntektEndring inntektEndring,
                                                    InntektskategoriEndring inntektskategoriEndring,
                                                    AktivitetStatus aktivitetStatus,
                                                    OpptjeningAktivitetType arbeidsforholdType,
                                                    Arbeidsgiver arbeidsgiver,
                                                    InternArbeidsforholdRef arbeidsforholdRef) {
        this.inntektEndring = inntektEndring;
        this.inntektskategoriEndring = inntektskategoriEndring;
        this.aktivitetStatus = aktivitetStatus;
        this.arbeidsforholdType = arbeidsforholdType;
        this.arbeidsgiver = arbeidsgiver;
        this.arbeidsforholdRef = arbeidsforholdRef;
    }

    public Optional<InntektEndring> getInntektEndring() {
        return Optional.ofNullable(inntektEndring);
    }

    public Optional<InntektskategoriEndring> getInntektskategoriEndring() {
        return Optional.ofNullable(inntektskategoriEndring);
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public OpptjeningAktivitetType getArbeidsforholdType() {
        return arbeidsforholdType;
    }

    public Optional<Arbeidsgiver> getArbeidsgiver() {
        return Optional.ofNullable(arbeidsgiver);
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef;
    }
}
