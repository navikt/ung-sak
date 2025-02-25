package no.nav.ung.sak.domene.iay.modell;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import no.nav.ung.kodeverk.api.IndexKey;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import no.nav.ung.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.Beløp;
import no.nav.ung.sak.typer.Stillingsprosent;

public class YtelseAnvist implements IndexKey {

    @ChangeTracked
    private DatoIntervallEntitet anvistPeriode;

    @ChangeTracked
    private Beløp beløp;

    @ChangeTracked
    private Beløp dagsats;

    @ChangeTracked
    private Stillingsprosent utbetalingsgradProsent;

    @ChangeTracked
    private Set<YtelseAnvistAndel> ytelseAnvistAndeler = new LinkedHashSet<>();

    public YtelseAnvist() {
        // hibernate
    }

    public YtelseAnvist(YtelseAnvist ytelseAnvist) {
        this.anvistPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(ytelseAnvist.getAnvistFOM(), ytelseAnvist.getAnvistTOM());
        this.beløp = ytelseAnvist.getBeløp().orElse(null);
        this.dagsats = ytelseAnvist.getDagsats().orElse(null);
        this.utbetalingsgradProsent = ytelseAnvist.getUtbetalingsgradProsent().orElse(null);
        this.ytelseAnvistAndeler = ytelseAnvist.getYtelseAnvistAndeler();
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { this.anvistPeriode };
        return IndexKeyComposer.createKey(keyParts);
    }

    public LocalDate getAnvistFOM() {
        return anvistPeriode.getFomDato();
    }

    public LocalDate getAnvistTOM() {
        return anvistPeriode.getTomDato();
    }

    public Optional<Stillingsprosent> getUtbetalingsgradProsent() {
        return Optional.ofNullable(utbetalingsgradProsent);
    }

    public Optional<Beløp> getBeløp() {
        return Optional.ofNullable(beløp);
    }

    public Optional<Beløp> getDagsats() {
        return Optional.ofNullable(dagsats);
    }

    public Set<YtelseAnvistAndel> getYtelseAnvistAndeler() {
        return ytelseAnvistAndeler;
    }

    void setBeløp(Beløp beløp) {
        this.beløp = beløp;
    }

    void setDagsats(Beløp dagsats) {
        this.dagsats = dagsats;
    }

    void setAnvistPeriode(DatoIntervallEntitet periode) {
        this.anvistPeriode = periode;
    }

    void setUtbetalingsgradProsent(Stillingsprosent utbetalingsgradProsent) {
        this.utbetalingsgradProsent = utbetalingsgradProsent;
    }

    void leggTilYtelseAnvistAndel(YtelseAnvistAndel ytelseAnvistAndele) {
        this.ytelseAnvistAndeler.add(ytelseAnvistAndele);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof YtelseAnvist)) return false;
        YtelseAnvist that = (YtelseAnvist) o;
        return Objects.equals(anvistPeriode, that.anvistPeriode) &&
            Objects.equals(beløp, that.beløp) &&
            Objects.equals(dagsats, that.dagsats) &&
            Objects.equals(utbetalingsgradProsent, that.utbetalingsgradProsent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(anvistPeriode, beløp, dagsats, utbetalingsgradProsent);
    }

    @Override
    public String toString() {
        return "YtelseAnvistEntitet{" +
            "periode=" + anvistPeriode +
            ", beløp=" + beløp +
            ", dagsats=" + dagsats +
            ", utbetalingsgradProsent=" + utbetalingsgradProsent +
            '}';
    }
}
