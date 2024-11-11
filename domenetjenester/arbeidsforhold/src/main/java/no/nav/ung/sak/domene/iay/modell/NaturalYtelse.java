package no.nav.ung.sak.domene.iay.modell;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.kodeverk.arbeidsforhold.NaturalYtelseType;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import no.nav.ung.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.Beløp;

public class NaturalYtelse implements IndexKey {

    @ChangeTracked
    private DatoIntervallEntitet periode;

    @ChangeTracked
    private Beløp beloepPerMnd;

    private NaturalYtelseType type = NaturalYtelseType.UDEFINERT;

    NaturalYtelse() {
    }

    public NaturalYtelse(LocalDate fom, LocalDate tom, BigDecimal beloepPerMnd, NaturalYtelseType type) {
        this.beloepPerMnd = beloepPerMnd == null ? Beløp.ZERO : new Beløp(beloepPerMnd);
        this.type = type;
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }

    public NaturalYtelse(DatoIntervallEntitet datoIntervall, BigDecimal beloepPerMnd, NaturalYtelseType type) {
        this.beloepPerMnd = beloepPerMnd == null ? Beløp.ZERO : new Beløp(beloepPerMnd);
        this.type = type;
        this.periode = datoIntervall;
    }

    NaturalYtelse(NaturalYtelse naturalYtelse) {
        this.periode = naturalYtelse.getPeriode();
        this.beloepPerMnd = naturalYtelse.getBeloepPerMnd();
        this.type = naturalYtelse.getType();
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { type, periode };
        return IndexKeyComposer.createKey(keyParts);
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public Beløp getBeloepPerMnd() {
        return beloepPerMnd;
    }

    public NaturalYtelseType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof NaturalYtelse))
            return false;
        NaturalYtelse that = (NaturalYtelse) o;
        return Objects.equals(periode, that.periode) &&
            Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, type);
    }

    @Override
    public String toString() {
        return "NaturalYtelseEntitet{" +
            "periode=" + periode +
            ", beloepPerMnd=" + beloepPerMnd +
            ", type=" + type +
            '}';
    }
}
