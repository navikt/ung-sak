package no.nav.ung.sak.domene.iay.modell;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import no.nav.ung.kodeverk.api.IndexKey;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import no.nav.ung.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.ung.sak.typer.Beløp;

public class Refusjon implements IndexKey {

    @ChangeTracked
    private Beløp refusjonsbeløpMnd;

    @ChangeTracked
    private LocalDate fom;

    Refusjon() {
    }

    public Refusjon(BigDecimal refusjonsbeløpMnd, LocalDate fom) {
        this.refusjonsbeløpMnd = refusjonsbeløpMnd == null ? null : new Beløp(refusjonsbeløpMnd);
        this.fom = fom;
    }

    Refusjon(Refusjon refusjon) {
        this.refusjonsbeløpMnd = refusjon.getRefusjonsbeløp();
        this.fom = refusjon.getFom();
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { fom, refusjonsbeløpMnd };
        return IndexKeyComposer.createKey(keyParts);
    }

    public Beløp getRefusjonsbeløp() {
        return refusjonsbeløpMnd;
    }

    public LocalDate getFom() {
        return fom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof Refusjon that)) return false;
        return Objects.equals(refusjonsbeløpMnd, that.refusjonsbeløpMnd) &&
            Objects.equals(fom, that.fom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(refusjonsbeløpMnd, fom);
    }
}
