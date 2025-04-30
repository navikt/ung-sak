package no.nav.ung.sak.domene.iay.modell;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import no.nav.ung.kodeverk.api.IndexKey;
import no.nav.ung.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import no.nav.ung.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

/**
 * Entitetsklasse for oppgitte arbeidsforhold.
 * <p>
 * Implementert iht. builder pattern (ref. "Effective Java, 2. ed." J.Bloch).
 * Non-public constructors og setters, dvs. immutable.
 * <p>
 * OBS: Legger man til nye felter så skal dette oppdateres mange steder:
 * builder, equals, hashcode etc.
 */
public class  OppgittArbeidsforhold implements IndexKey {

    @ChangeTracked
    private DatoIntervallEntitet periode;

    @ChangeTracked
    private BigDecimal inntekt;

    @ChangeTracked
    private ArbeidType arbeidType;

    OppgittArbeidsforhold() {
    }

    OppgittArbeidsforhold(OppgittArbeidsforhold kopierFra) {
        this.periode = kopierFra.periode;
        this.arbeidType=kopierFra.arbeidType;
        this.inntekt = kopierFra.inntekt;
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { periode, arbeidType };
        return IndexKeyComposer.createKey(keyParts);
    }

    public LocalDate getFraOgMed() {
        return periode.getFomDato();
    }

    public LocalDate getTilOgMed() {
        return periode.getTomDato();
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public ArbeidType getArbeidType() {
        return arbeidType;
    }

    void setPeriode(DatoIntervallEntitet periode) {
        this.periode = periode;
    }

    void setArbeidType(ArbeidType arbeidType) {
        this.arbeidType = arbeidType;
    }

    public BigDecimal getInntekt() {
        return inntekt;
    }

    void setInntekt(BigDecimal inntekt) {
        this.inntekt = inntekt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof OppgittArbeidsforhold)) return false;

        OppgittArbeidsforhold that = (OppgittArbeidsforhold) o;

        return
                Objects.equals(periode, that.periode) &&
                        Objects.equals(arbeidType, that.arbeidType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, arbeidType);
    }

    @Override
    public String toString() {
        return "OppgittArbeidsforholdImpl{" +
                "periode=" + periode +
                ", arbeidType=" + arbeidType +
                '}';
    }
}
