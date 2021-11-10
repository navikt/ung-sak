package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.kompletthet;

import java.util.Objects;

import no.nav.k9.kodeverk.dokument.DokumentMalType;
import no.nav.k9.sak.typer.Arbeidsgiver;

public class TidligereEtterlysning {
    private DokumentMalType dokumentType;
    private Arbeidsgiver arbeidsgiver;

    public TidligereEtterlysning(DokumentMalType dokumentType, Arbeidsgiver arbeidsgiver) {
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
        TidligereEtterlysning that = (TidligereEtterlysning) o;
        return dokumentType == that.dokumentType && Objects.equals(arbeidsgiver, that.arbeidsgiver);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dokumentType, arbeidsgiver);
    }
}
