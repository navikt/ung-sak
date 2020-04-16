package no.nav.k9.sak.domene.iay.modell;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

/**
 * Entitetsklasse for oppgitte arbeidsforhold.
 * <p>
 * Implementert iht. builder pattern (ref. "Effective Java, 2. ed." J.Bloch).
 * Non-public constructors og setters, dvs. immutable.
 * <p>
 * OBS: Legger man til nye felter så skal dette oppdateres mange steder:
 * builder, equals, hashcode etc.
 */
public class OppgittArbeidsforhold implements IndexKey {

    @ChangeTracked
    private DatoIntervallEntitet periode;

    private boolean erUtenlandskInntekt;

    @ChangeTracked
    private ArbeidType arbeidType;

    private OppgittUtenlandskVirksomhet utenlandskVirksomhet = new OppgittUtenlandskVirksomhet();

    OppgittArbeidsforhold() {
    }
    
    OppgittArbeidsforhold(OppgittArbeidsforhold kopierFra) {
        this.periode = kopierFra.periode;
        this.erUtenlandskInntekt=kopierFra.erUtenlandskInntekt;
        this.arbeidType=kopierFra.arbeidType;
        this.utenlandskVirksomhet = kopierFra.utenlandskVirksomhet;
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { periode, utenlandskVirksomhet, arbeidType };
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

    public Boolean erUtenlandskInntekt() {
        return erUtenlandskInntekt;
    }

    public ArbeidType getArbeidType() {
        return arbeidType;
    }

    public OppgittUtenlandskVirksomhet getUtenlandskVirksomhet() {
        return utenlandskVirksomhet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof OppgittArbeidsforhold)) return false;

        OppgittArbeidsforhold that = (OppgittArbeidsforhold) o;

        return
            Objects.equals(periode, that.periode) &&
            Objects.equals(arbeidType, that.arbeidType) &&
            Objects.equals(utenlandskVirksomhet, that.utenlandskVirksomhet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, arbeidType, utenlandskVirksomhet);
    }

    @Override
    public String toString() {
        return "OppgittArbeidsforholdImpl{" +
            "periode=" + periode +
            ", erUtenlandskInntekt=" + erUtenlandskInntekt +
            ", arbeidType=" + arbeidType +
            ", utenlandskVirksomhet=" + utenlandskVirksomhet +
            '}';
    }

    void setPeriode(DatoIntervallEntitet periode) {
        this.periode = periode;
    }

    void setErUtenlandskInntekt(Boolean erUtenlandskInntekt) {
        this.erUtenlandskInntekt = erUtenlandskInntekt;
    }

    void setArbeidType(ArbeidType arbeidType) {
        this.arbeidType = arbeidType;
    }

    void setUtenlandskVirksomhet(OppgittUtenlandskVirksomhet utenlandskVirksomhet) {
        this.utenlandskVirksomhet = utenlandskVirksomhet;
    }
}
