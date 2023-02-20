package no.nav.folketrygdloven.beregningsgrunnlag.resultat;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;


public class BeregningAktivitetNøkkel {

    private final OpptjeningAktivitetType opptjeningAktivitetType;
    private final LocalDate fom;
    private final Arbeidsgiver arbeidsgiver;
    private final InternArbeidsforholdRef arbeidsforholdRef;

    public BeregningAktivitetNøkkel(OpptjeningAktivitetType opptjeningAktivitetType,
                                    LocalDate fom,
                                    Arbeidsgiver arbeidsgiver,
                                    InternArbeidsforholdRef arbeidsforholdRef) {
        this.opptjeningAktivitetType = opptjeningAktivitetType;
        this.fom = fom;
        this.arbeidsgiver = arbeidsgiver;
        this.arbeidsforholdRef = arbeidsforholdRef;
    }

    public OpptjeningAktivitetType getOpptjeningAktivitetType() {
        return opptjeningAktivitetType;
    }

    public LocalDate getFom() {
        return fom;
    }


    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BeregningAktivitetNøkkel)) {
            return false;
        }
        BeregningAktivitetNøkkel that = (BeregningAktivitetNøkkel) o;
        return Objects.equals(opptjeningAktivitetType, that.opptjeningAktivitetType)
            && Objects.equals(fom, that.fom)
            && Objects.equals(arbeidsgiver, that.arbeidsgiver)
            && Objects.equals(arbeidsforholdRef, that.arbeidsforholdRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(opptjeningAktivitetType, fom, arbeidsgiver, arbeidsforholdRef);
    }

}
