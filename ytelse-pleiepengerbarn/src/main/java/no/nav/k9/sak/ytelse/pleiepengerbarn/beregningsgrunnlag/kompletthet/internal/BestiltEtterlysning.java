package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.kompletthet.internal;

import java.util.Objects;

import no.nav.k9.formidling.kontrakt.kodeverk.DokumentMalType;
import no.nav.k9.sak.typer.Arbeidsgiver;

public class BestiltEtterlysning {
    private DokumentMalType dokumentType;
    private Arbeidsgiver arbeidsgiver;

    public BestiltEtterlysning(DokumentMalType dokumentType, Arbeidsgiver arbeidsgiver) {
        this.dokumentType = dokumentType;
        this.arbeidsgiver = arbeidsgiver;
    }

    public DokumentMalType getDokumentType() {
        return dokumentType;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BestiltEtterlysning that = (BestiltEtterlysning) o;
        return dokumentType == that.dokumentType && Objects.equals(arbeidsgiver, that.arbeidsgiver);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dokumentType, arbeidsgiver);
    }
}
