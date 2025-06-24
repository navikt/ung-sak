package no.nav.ung.sak.behandlingslager.tilkjentytelse;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.PostgreSQLRangeType;
import no.nav.ung.sak.behandlingslager.Range;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
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

    @Column(name = "utbetalingsgrad", nullable = false)
    private int utbetalingsgrad;

    @Column(name = "avvik_avrunding", nullable = false)
    private double avvikGrunnetAvrunding;

    protected TilkjentYtelsePeriode() {
    }

    private TilkjentYtelsePeriode(DatoIntervallEntitet periode,
                                  BigDecimal uredusertBeløp,
                                  BigDecimal reduksjon,
                                  BigDecimal redusertBeløp,
                                  BigDecimal dagsats,
                                  int utbetalingsgrad,
                                  double avvikGrunnetAvrunding) {
        this.periode = Range.closed(periode.getFomDato(), periode.getTomDato());
        this.uredusertBeløp = uredusertBeløp;
        this.reduksjon = reduksjon;
        this.redusertBeløp = redusertBeløp;
        this.dagsats = dagsats;
        this.utbetalingsgrad = utbetalingsgrad;
        this.avvikGrunnetAvrunding = avvikGrunnetAvrunding;
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

    public int getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    public double getAvvikGrunnetAvrunding() {
        return avvikGrunnetAvrunding;
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
        private int utbetalingsgrad;
        private double avvikGrunnetAvrunding;

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

        public Builder medUtbetalingsgrad(int utbetalingsgrad) {
            this.utbetalingsgrad = utbetalingsgrad;
            return this;
        }

        public Builder medAvvikGrunnetAvrunding(double avvikGrunnetAvrunding) {
            this.avvikGrunnetAvrunding = avvikGrunnetAvrunding;
            return this;
        }


        public TilkjentYtelsePeriode build() {
            return new TilkjentYtelsePeriode(periode, uredusertBeløp, reduksjon, redusertBeløp, dagsats, utbetalingsgrad, avvikGrunnetAvrunding);
        }


    }

}
