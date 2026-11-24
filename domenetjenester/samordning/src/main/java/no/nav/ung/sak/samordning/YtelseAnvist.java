package no.nav.ung.sak.samordning;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.PostgreSQLRangeType;
import no.nav.ung.sak.domene.typer.tid.Range;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Innvilget delperiode i et {@link VedtakYtelse}.
 * <p>
 * Dagsats og beløp er tomme når perioden ikke er beregnet ennå (f.eks. framtidige måneder),
 * og 0 når perioden er beregnet uten utbetaling. Perioden er innvilget i begge tilfeller.
 */
@Entity(name = "YtelseAnvist")
@Table(name = "ve_ytelse_anvist")
public class YtelseAnvist extends BaseEntitet {

    @Id
    @SequenceGenerator(name = "seq_ve_ytelse_anvist", sequenceName = "seq_ve_ytelse_anvist")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_ve_ytelse_anvist")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vedtak_ytelse_id", nullable = false, updatable = false)
    private VedtakYtelse vedtakYtelse;

    @Type(PostgreSQLRangeType.class)
    @Column(name = "periode", columnDefinition = "daterange", nullable = false, updatable = false)
    private Range<LocalDate> periode;

    @Column(name = "dagsats", updatable = false)
    private BigDecimal dagsats;

    @Column(name = "beloep", updatable = false)
    private BigDecimal beløp;

    @Column(name = "antall_barn", updatable = false)
    private Integer antallBarn;

    @Column(name = "dagsats_barnetillegg", updatable = false)
    private Integer dagsatsBarnetillegg;

    YtelseAnvist() {
        // for hibernate
    }

    public static Builder builder(DatoIntervallEntitet periode) {
        return new Builder(periode);
    }

    void setVedtakYtelse(VedtakYtelse vedtakYtelse) {
        this.vedtakYtelse = vedtakYtelse;
    }

    public DatoIntervallEntitet getPeriode() {
        return DatoIntervallEntitet.fra(periode);
    }

    public Optional<BigDecimal> getDagsats() {
        return Optional.ofNullable(dagsats);
    }

    public Optional<BigDecimal> getBeløp() {
        return Optional.ofNullable(beløp);
    }

    public Optional<Integer> getAntallBarn() {
        return Optional.ofNullable(antallBarn);
    }

    public Optional<Integer> getDagsatsBarnetillegg() {
        return Optional.ofNullable(dagsatsBarnetillegg);
    }

    public boolean erBeregnet() {
        return dagsats != null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<periode=" + getPeriode()
            + ", dagsats=" + dagsats
            + ", antallBarn=" + antallBarn
            + ">";
    }

    public static class Builder {
        private final YtelseAnvist kladd = new YtelseAnvist();

        private Builder(DatoIntervallEntitet periode) {
            kladd.periode = Objects.requireNonNull(periode, "periode").toRange();
        }

        public Builder medDagsats(BigDecimal dagsats) {
            kladd.dagsats = dagsats;
            return this;
        }

        public Builder medBeløp(BigDecimal beløp) {
            kladd.beløp = beløp;
            return this;
        }

        public Builder medAntallBarn(Integer antallBarn) {
            kladd.antallBarn = antallBarn;
            return this;
        }

        public Builder medDagsatsBarnetillegg(Integer dagsatsBarnetillegg) {
            kladd.dagsatsBarnetillegg = dagsatsBarnetillegg;
            return this;
        }

        public YtelseAnvist build() {
            return kladd;
        }
    }
}
