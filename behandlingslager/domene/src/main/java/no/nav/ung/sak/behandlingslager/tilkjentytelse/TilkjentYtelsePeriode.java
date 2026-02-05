package no.nav.ung.sak.behandlingslager.tilkjentytelse;

import jakarta.persistence.*;
import no.nav.ung.sak.BaseEntitet;
import no.nav.ung.sak.tid.PostgreSQLRangeType;
import no.nav.ung.sak.tid.Range;
import no.nav.ung.sak.tid.DatoIntervallEntitet;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity(name = "TilkjentYtelsePeriode")
@Table(name = "TILKJENT_YTELSE_PERIODE")
public class TilkjentYtelsePeriode extends BaseEntitet {


    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_TILKJENT_YTELSE_PERIODE")
    private Long id;

    @Type(PostgreSQLRangeType.class)
    @Column(name = "periode", columnDefinition = "daterange")
    private Range<LocalDate> periode;

    @Column(name = "uredusert_belop", nullable = false)
    private BigDecimal uredusertBeløp;

    @Column(name = "reduksjon", nullable = false)
    private BigDecimal reduksjon;

    @Column(name = "redusert_belop", nullable = false)
    private BigDecimal redusertBeløp;

    @Column(name = "dagsats", nullable = false)
    private BigDecimal dagsats;

    @Column(name = "utbetalingsgrad", nullable = false, precision = 13, scale = 10)
    private BigDecimal utbetalingsgrad;

    protected TilkjentYtelsePeriode() {
    }

    private TilkjentYtelsePeriode(DatoIntervallEntitet periode,
                                  BigDecimal uredusertBeløp,
                                  BigDecimal reduksjon,
                                  BigDecimal redusertBeløp,
                                  BigDecimal dagsats, BigDecimal utbetalingsgrad) {
        this.periode = Range.closed(periode.getFomDato(), periode.getTomDato());
        this.uredusertBeløp = uredusertBeløp;
        this.reduksjon = reduksjon;
        this.redusertBeløp = redusertBeløp;
        this.dagsats = dagsats;
        this.utbetalingsgrad = utbetalingsgrad;
    }

    public DatoIntervallEntitet getPeriode() {
        return DatoIntervallEntitet.fra(periode);
    }

    public BigDecimal getUredusertBeløp() {
        return uredusertBeløp;
    }

    public BigDecimal getReduksjon() {
        return reduksjon;
    }

    public BigDecimal getRedusertBeløp() {
        return redusertBeløp;
    }

    public BigDecimal getDagsats() {
        return dagsats;
    }

    public BigDecimal getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    public static Builder ny() {
        return new Builder();
    }

    public static class Builder {

        private DatoIntervallEntitet periode;
        private BigDecimal uredusertBeløp;
        private BigDecimal reduksjon;
        private BigDecimal redusertBeløp;
        private BigDecimal dagsats;
        private BigDecimal utbetalingsgrad;

        private Builder() {}

        public Builder medPeriode(DatoIntervallEntitet periode) {
            if (periode == null) {
                throw new IllegalArgumentException("periode kan ikke være null");
            }
            this.periode = periode;
            return this;
        }

        public Builder medUredusertBeløp(BigDecimal uredusertBeløp) {
            if (uredusertBeløp == null) {
                throw new IllegalArgumentException("uredusertBeløp kan ikke være null");
            }
            this.uredusertBeløp = uredusertBeløp;
            return this;
        }

        public Builder medReduksjon(BigDecimal reduksjon) {
            if (reduksjon == null) {
                throw new IllegalArgumentException("reduksjon kan ikke være null");
            }
            this.reduksjon = reduksjon;
            return this;
        }

        public Builder medRedusertBeløp(BigDecimal redusertBeløp) {
            if (redusertBeløp == null) {
                throw new IllegalArgumentException("redusertBeløp kan ikke være null");
            }
            this.redusertBeløp = redusertBeløp;
            return this;
        }

        public Builder medDagsats(BigDecimal dagsats) {
            if (dagsats == null) {
                throw new IllegalArgumentException("dagsats kan ikke være null");
            }
            this.dagsats = dagsats;
            return this;
        }

        public Builder medUtbetalingsgrad(BigDecimal utbetalingsgrad) {
            if (utbetalingsgrad == null) {
                throw new IllegalArgumentException("utbetalingsgrad kan ikke være null");
            }
            this.utbetalingsgrad = utbetalingsgrad;
            return this;
        }

        public TilkjentYtelsePeriode build() {
            return new TilkjentYtelsePeriode(periode, uredusertBeløp, reduksjon, redusertBeløp, dagsats, utbetalingsgrad);
        }


    }

}
