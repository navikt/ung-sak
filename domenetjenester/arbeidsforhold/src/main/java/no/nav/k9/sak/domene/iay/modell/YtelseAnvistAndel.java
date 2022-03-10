package no.nav.k9.sak.domene.iay.modell;

import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import no.nav.abakus.iaygrunnlag.kodeverk.IndexKey;
import no.nav.abakus.iaygrunnlag.kodeverk.Inntektskategori;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Beløp;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.Stillingsprosent;


public class YtelseAnvistAndel extends BaseEntitet implements IndexKey {


    @ChangeTracked
    private Arbeidsgiver arbeidsgiver;

    @Embedded
    private InternArbeidsforholdRef arbeidsforholdRef;

    @ChangeTracked
    private Beløp dagsats;

    @Embedded
    @ChangeTracked
    private Stillingsprosent utbetalingsgradProsent;

    @Embedded
    @ChangeTracked
    private Stillingsprosent refusjonsgradProsent;

    @Column(name = "inntektskategori", nullable = false, updatable = false)
    private Inntektskategori inntektskategori = Inntektskategori.UDEFINERT;

    public YtelseAnvistAndel() {
        // hibernate
    }

    public YtelseAnvistAndel(YtelseAnvistAndel ytelseAnvistAndel) {
        ytelseAnvistAndel.getArbeidsgiver().ifPresent(this::setArbeidsgiver);
        this.dagsats = ytelseAnvistAndel.getDagsats();
        this.inntektskategori = ytelseAnvistAndel.getInntektskategori();
        this.arbeidsforholdRef = ytelseAnvistAndel.getArbeidsforholdRef();
        this.utbetalingsgradProsent = ytelseAnvistAndel.getUtbetalingsgradProsent();
        this.refusjonsgradProsent = ytelseAnvistAndel.getRefusjonsgradProsent();
    }

    public Optional<Arbeidsgiver> getArbeidsgiver() {
        return Optional.ofNullable(arbeidsgiver);
    }

    public void setArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        this.arbeidsgiver = arbeidsgiver;
    }


    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef;
    }

    public void setArbeidsforholdRef(InternArbeidsforholdRef arbeidsforholdRef) {
        this.arbeidsforholdRef = arbeidsforholdRef;
    }

    public Beløp getDagsats() {
        return dagsats;
    }

    public void setDagsats(Beløp dagsats) {
        this.dagsats = dagsats;
    }

    public Stillingsprosent getUtbetalingsgradProsent() {
        return utbetalingsgradProsent;
    }

    public void setUtbetalingsgradProsent(Stillingsprosent utbetalingsgradProsent) {
        this.utbetalingsgradProsent = utbetalingsgradProsent;
    }

    public Stillingsprosent getRefusjonsgradProsent() {
        return refusjonsgradProsent;
    }

    public void setRefusjonsgradProsent(Stillingsprosent refusjonsgradProsent) {
        this.refusjonsgradProsent = refusjonsgradProsent;
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

    public void setInntektskategori(Inntektskategori inntektskategori) {
        this.inntektskategori = inntektskategori;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        YtelseAnvistAndel that = (YtelseAnvistAndel) o;
        return Objects.equals(arbeidsgiver, that.arbeidsgiver) &&
            dagsats.equals(that.dagsats) &&
            inntektskategori == that.inntektskategori &&
            utbetalingsgradProsent.equals(that.utbetalingsgradProsent) &&
            refusjonsgradProsent.equals(that.refusjonsgradProsent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsgiver, dagsats, inntektskategori, utbetalingsgradProsent, refusjonsgradProsent);
    }

    @Override
    public String toString() {
        return "YtelseAnvistAndel{" +
            "arbeidsgiver=" + arbeidsgiver +
            ", arbeidsforholdRef=" + arbeidsforholdRef +
            ", dagsats=" + dagsats +
            ", utbetalingsgradProsent=" + utbetalingsgradProsent +
            ", refusjonsgradProsent=" + refusjonsgradProsent +
            ", inntektskategori=" + inntektskategori +
            '}';
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { arbeidsgiver, dagsats, inntektskategori, utbetalingsgradProsent, refusjonsgradProsent };
        return IndexKeyComposer.createKey(keyParts);
    }
}
