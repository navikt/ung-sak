package no.nav.ung.sak.behandlingslager.tilkjentytelse;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.hjemmel.Hjemmel;
import no.nav.ung.sak.behandlingslager.kodeverk.HjemmelKodeverdiConverter;
import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;
import no.nav.ung.sak.behandlingslager.kodeverk.KontrollertInntektKildeKodeverdiConverter;
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

    @Column(name = "inntekt")
    private BigDecimal inntekt;

    @Convert(converter = KontrollertInntektKildeKodeverdiConverter.class)
    @Column(name = "kilde", nullable = false)
    private KontrollertInntektKilde kilde;

    @Convert(converter = HjemmelKodeverdiConverter.class)
    @Column(name = "hjemmel", nullable = false)
    private Hjemmel hjemmel = Hjemmel.UNG_FORSKRIFT_PARAGRAF_11;

    @Column(name = "er_manuelt_vurdert", nullable = false)
    private boolean erManueltVurdert;

    /**
     * Begrunnelse for valg og fastsatt inntekt ved manuell vurdering.
     */
    @Column(name = "manuelt_vurdert_begrunnelse", nullable = true)
    private String begrunnelse;


    private KontrollertInntektPeriode() {
        // Hibernate
    }

    public KontrollertInntektPeriode(KontrollertInntektPeriode eksisterende) {
        this.periode = Range.closed(eksisterende.getPeriode().getFomDato(), eksisterende.getPeriode().getTomDato());
        this.inntekt = eksisterende.getInntekt();
        this.kilde = eksisterende.getKilde();
        this.erManueltVurdert = eksisterende.getErManueltVurdert();
    }

    private KontrollertInntektPeriode(DatoIntervallEntitet periode,
                                      BigDecimal inntekt,
                                      KontrollertInntektKilde kilde,
                                      boolean erManueltVurdert,
                                      String begrunnelse) {
        this.periode = Range.closed(periode.getFomDato(), periode.getTomDato());
        this.inntekt = inntekt;
        this.kilde = kilde;
        this.erManueltVurdert = erManueltVurdert;
        this.begrunnelse = begrunnelse;
    }

    public DatoIntervallEntitet getPeriode() {
        return DatoIntervallEntitet.fra(periode);
    }

    public BigDecimal getInntekt() {
        return inntekt;
    }

    public KontrollertInntektKilde getKilde() {
        return kilde;
    }

    public boolean getErManueltVurdert() {
        return erManueltVurdert;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    @Override
    public String toString() {
        return "KontrollertInntektPeriode{" +
            "periode=" + periode +
            ", inntekt=" + inntekt +
            ", kilde=" + kilde +
            ", erManueltVurdert=" + erManueltVurdert +
            '}';
    }

    public static Builder ny() {
        return new Builder();
    }

    public static class Builder {

        private DatoIntervallEntitet periode;
        private BigDecimal inntekt;
        private KontrollertInntektKilde kilde;
        private boolean erManueltVurdert;
        private String begrunnelse;

        private Builder() {
        }

        public Builder medPeriode(DatoIntervallEntitet periode) {
            if (periode == null) {
                throw new IllegalArgumentException("periode kan ikke være null");
            }
            this.periode = periode;
            return this;
        }

        public Builder medInntekt(BigDecimal arbeidsinntekt) {
            this.inntekt = arbeidsinntekt;
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

        public Builder medBegrunnelse(String begrunnelse) {
            this.begrunnelse = begrunnelse;
            return this;
        }


        public KontrollertInntektPeriode build() {
            return new KontrollertInntektPeriode(periode, inntekt, kilde, erManueltVurdert, begrunnelse);
        }


    }

}
