package no.nav.k9.sak.domene.iay.modell;

import java.util.Objects;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;


public class OppgittAnnenAktivitet implements IndexKey {

    @ChangeTracked
    DatoIntervallEntitet periode;

    @ChangeTracked
    private ArbeidType arbeidType;

    public OppgittAnnenAktivitet(DatoIntervallEntitet periode, ArbeidType arbeidType) {
        this.periode = periode;
        this.arbeidType = arbeidType;
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
        if (this == o) return true;
        if (o == null || !(o instanceof OppgittAnnenAktivitet)) return false;
        OppgittAnnenAktivitet that = (OppgittAnnenAktivitet) o;
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
