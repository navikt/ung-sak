package no.nav.ung.sak.domene.iay.modell;

import java.util.Objects;

import no.nav.ung.kodeverk.api.IndexKey;
import no.nav.ung.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import no.nav.ung.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

public class OppgittAnnenAktivitet implements IndexKey {

    @ChangeTracked
    DatoIntervallEntitet periode;

    @ChangeTracked
    private ArbeidType arbeidType;

    public OppgittAnnenAktivitet(DatoIntervallEntitet periode, ArbeidType arbeidType) {
        this.periode = periode;
        this.arbeidType = arbeidType;
    }

    /** deep copy ctor. */
    OppgittAnnenAktivitet(OppgittAnnenAktivitet kopierFra) {
        this.periode = kopierFra.periode;
        this.arbeidType = kopierFra.arbeidType;
    }

    OppgittAnnenAktivitet() {
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { periode, arbeidType };
        return IndexKeyComposer.createKey(keyParts);
    }

    public ArbeidType getArbeidType() {
        return arbeidType;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof OppgittAnnenAktivitet that))
            return false;
        return Objects.equals(periode, that.periode) &&
            Objects.equals(arbeidType, that.arbeidType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, arbeidType);
    }

    @Override
    public String toString() {
        return "AnnenAktivitetEntitet{" +
            "periode=" + periode +
            ", arbeidType=" + arbeidType +
            '}';
    }
}
