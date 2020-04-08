package no.nav.k9.sak.domene.iay.modell;

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

    OppgittFrilansoppdrag() {
    }

    public OppgittFrilansoppdrag(String oppdragsgiver, DatoIntervallEntitet periode) {
        this.oppdragsgiver = oppdragsgiver;
        this.periode = periode;
    }

    /** deep-copy ctor. */
    OppgittFrilansoppdrag(OppgittFrilansoppdrag kopierFra) {
        this.oppdragsgiver = kopierFra.oppdragsgiver;
        this.periode = kopierFra.periode;
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { periode, oppdragsgiver };
        return IndexKeyComposer.createKey(keyParts);
    }

    void setPeriode(DatoIntervallEntitet periode) {
        this.periode = periode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof OppgittFrilansoppdrag))
            return false;
        OppgittFrilansoppdrag that = (OppgittFrilansoppdrag) o;
        return Objects.equals(oppdragsgiver, that.oppdragsgiver) &&
            Objects.equals(periode, that.periode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oppdragsgiver, periode);
    }

    @Override
    public String toString() {
        return "FrilansoppdragEntitet<" + "oppdragsgiver='" + oppdragsgiver + '\'' + ", periode=" + periode + '>';
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public String getOppdragsgiver() {
        return oppdragsgiver;
    }

}
