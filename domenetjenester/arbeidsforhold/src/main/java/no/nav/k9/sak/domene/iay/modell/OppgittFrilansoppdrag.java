package no.nav.k9.sak.domene.iay.modell;

import java.math.BigDecimal;
import java.util.Objects;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class OppgittFrilansoppdrag implements IndexKey {

    @ChangeTracked
    private String oppdragsgiver;

    @ChangeTracked
    private DatoIntervallEntitet periode;

    @ChangeTracked
    private BigDecimal inntekt;

    OppgittFrilansoppdrag() {
    }

    /** deep-copy ctor. */
    OppgittFrilansoppdrag(OppgittFrilansoppdrag kopierFra) {
        this.oppdragsgiver = kopierFra.oppdragsgiver;
        this.periode = kopierFra.periode;
        this.inntekt = kopierFra.inntekt;
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { periode, oppdragsgiver };
        return IndexKeyComposer.createKey(keyParts);
    }

    void setPeriode(DatoIntervallEntitet periode) {
        this.periode = periode;
    }

    public BigDecimal getInntekt() {
        return inntekt;
    }

    void setInntekt(BigDecimal inntekt) {
        this.inntekt = inntekt;
    }

    void setOppdragsgiver(String oppdragsgiver) {
        this.oppdragsgiver = oppdragsgiver;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OppgittFrilansoppdrag that = (OppgittFrilansoppdrag) o;
        return Objects.equals(oppdragsgiver, that.oppdragsgiver) &&
                periode.equals(that.periode) &&
                Objects.equals(inntekt, that.inntekt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oppdragsgiver, periode, inntekt);
    }

    @Override
    public String toString() {
        return "OppgittFrilansoppdrag{" +
                "oppdragsgiver='" + oppdragsgiver + '\'' +
                ", periode=" + periode +
                ", inntekt=" + inntekt +
                '}';
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public String getOppdragsgiver() {
        return oppdragsgiver;
    }

}
