package no.nav.folketrygdloven.beregningsgrunnlag.tilkommetAktivitet;

import java.util.Objects;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.typer.Arbeidsgiver;

public class AktivitetstatusOgArbeidsgiver {

    private UttakArbeidType aktivitetType;
    private Arbeidsgiver arbeidsgiver;
    
    
    public AktivitetstatusOgArbeidsgiver(UttakArbeidType aktivitetType, Arbeidsgiver arbeidsgiver) {
        this.aktivitetType = aktivitetType;
        this.arbeidsgiver = arbeidsgiver;
    }
    
    
    public UttakArbeidType getAktivitetType() {
        return aktivitetType;
    }
    
    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktivitetType, arbeidsgiver);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AktivitetstatusOgArbeidsgiver other = (AktivitetstatusOgArbeidsgiver) obj;
        return aktivitetType == other.aktivitetType && Objects.equals(arbeidsgiver, other.arbeidsgiver);
    }
}
