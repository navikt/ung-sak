package no.nav.ung.sak.behandlingslager.tilkjentytelse;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;
import no.nav.ung.kodeverk.kontroll.KontrollertInntektKildeKodeverdiConverter;
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

    @Convert(converter = KontrollertInntektKildeKodeverdiConverter.class)
    @Column(name = "kilde", nullable = false)
    private KontrollertInntektKilde kilde;

    @Column(name = "er_manuelt_vurdert", nullable = false)
    private boolean erManueltVurdert;


    private KontrollertInntektPeriode() {
        // Hibernate
    }

    KontrollertInntektPeriode(KontrollertInntektPeriode eksisterende) {
        this.periode = Range.closed(eksisterende.getPeriode().getFomDato(), eksisterende.getPeriode().getTomDato());
        this.arbeidsinntekt = eksisterende.getArbeidsinntekt();
        this.ytelse = eksisterende.getYtelse();
    }

    private KontrollertInntektPeriode(DatoIntervallEntitet periode,
                                      BigDecimal arbeidsinntekt,
                                      BigDecimal ytelse,
                                      KontrollertInntektKilde kilde,
                                      boolean erManueltVurdert) {
        this.periode = Range.closed(periode.getFomDato(), periode.getTomDato());
        this.arbeidsinntekt = arbeidsinntekt;
        this.ytelse = ytelse;
        this.kilde = kilde;
        this.erManueltVurdert = erManueltVurdert;
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

    public KontrollertInntektKilde getKilde() {
        return kilde;
    }

    public boolean getErManueltVurdert() {
        return erManueltVurdert;
    }

    @Override
    public String toString() {
        return "KontrollertInntektPeriode{" +
            "periode=" + periode +
            ", arbeidsinntekt=" + arbeidsinntekt +
            ", ytelse=" + ytelse +
            ", kilde=" + kilde +
            ", erManueltVurdert=" + erManueltVurdert +
            '}';
    }

    public static Builder ny() {
        return new Builder();
    }

    public static class Builder {

        private DatoIntervallEntitet periode;
        private BigDecimal arbeidsinntekt;
        private BigDecimal ytelse;
        private KontrollertInntektKilde kilde;
        private boolean erManueltVurdert;

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

        public Builder medKilde(KontrollertInntektKilde kilde) {
            this.kilde = kilde;
            return this;
        }

        public Builder medErManueltVurdert(boolean erManueltVurdert) {
            this.erManueltVurdert = erManueltVurdert;
            return this;
        }


        public KontrollertInntektPeriode build() {
            return new KontrollertInntektPeriode(periode, arbeidsinntekt, ytelse, kilde, erManueltVurdert);
        }


    }

}
