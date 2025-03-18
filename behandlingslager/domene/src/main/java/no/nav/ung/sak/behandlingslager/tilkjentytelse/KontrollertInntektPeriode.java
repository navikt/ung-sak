package no.nav.ung.sak.behandlingslager.tilkjentytelse;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.PostgreSQLRangeType;
import no.nav.ung.sak.behandlingslager.Range;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity(name = "KontrollertInntektPeriode")
@Table(name = "KONTROLLERT_INNTEKT_PERIODE")
public class KontrollertInntektPeriode extends BaseEntitet {


    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_KONTROLLERT_INNTEKT_PERIODE")
    private Long id;

    @Type(PostgreSQLRangeType.class)
    @Column(name = "periode", columnDefinition = "daterange")
    private Range<LocalDate> periode;

    @Column(name = "arbeidsinntekt")
    private BigDecimal arbeidsinntekt;

    @Column(name = "ytelse")
    private BigDecimal ytelse;

    protected KontrollertInntektPeriode() {
    }

    KontrollertInntektPeriode(KontrollertInntektPeriode eksisterende) {
        this.periode = Range.closed(eksisterende.getPeriode().getFomDato(), eksisterende.getPeriode().getTomDato());
        this.arbeidsinntekt = eksisterende.getArbeidsinntekt();
        this.ytelse = eksisterende.getYtelse();
    }

    private KontrollertInntektPeriode(DatoIntervallEntitet periode,
                                      BigDecimal arbeidsinntekt,
                                      BigDecimal ytelse) {
        this.periode = Range.closed(periode.getFomDato(), periode.getTomDato());
        this.arbeidsinntekt = arbeidsinntekt;
        this.ytelse = ytelse;
    }

    public DatoIntervallEntitet getPeriode() {
        return DatoIntervallEntitet.fra(periode);
    }

    public BigDecimal getArbeidsinntekt() {
        return arbeidsinntekt;
    }

    public BigDecimal getYtelse() {
        return ytelse;
    }

    public static Builder ny() {
        return new Builder();
    }

    public static class Builder {

        private DatoIntervallEntitet periode;
        private BigDecimal arbeidsinntekt;
        private BigDecimal ytelse;

        private Builder() {}

        public Builder medPeriode(DatoIntervallEntitet periode) {
            if (periode == null) {
                throw new IllegalArgumentException("periode kan ikke være null");
            }
            this.periode = periode;
            return this;
        }

        public Builder medArbeidsinntekt(BigDecimal arbeidsinntekt) {
            this.arbeidsinntekt = arbeidsinntekt;
            return this;
        }

        public Builder medYtelse(BigDecimal ytelse) {
            this.ytelse = ytelse;
            return this;
        }


        public KontrollertInntektPeriode build() {
            return new KontrollertInntektPeriode(periode, arbeidsinntekt, ytelse);
        }


    }

}
