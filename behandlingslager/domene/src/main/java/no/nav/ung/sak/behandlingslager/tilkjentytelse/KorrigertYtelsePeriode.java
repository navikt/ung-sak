package no.nav.ung.sak.behandlingslager.tilkjentytelse;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.hjemmel.HjemmelKodeverdiConverter;
import no.nav.ung.kodeverk.ytelse.KorrigertYtelseÅrsak;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.PostgreSQLRangeType;
import no.nav.ung.sak.behandlingslager.Range;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity(name = "KorrigertYtelsePeriode")
@Table(name = "KORRIGERT_YTELSE_PERIODE")
public class KorrigertYtelsePeriode extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_KORRIGERT_YTELSE_PERIODE")
    private Long id;

    @Type(PostgreSQLRangeType.class)
    @Column(name = "periode", columnDefinition = "daterange")
    private Range<LocalDate> periode;

    @Column(name = "korrigert_dagsats", nullable = false)
    private BigDecimal dagsats;

    @Convert(converter = HjemmelKodeverdiConverter.class)
    @Column(name = "aarsak_for_korrigering", nullable = false)
    private KorrigertYtelseÅrsak årsak;

    protected KorrigertYtelsePeriode() {
    }

    private KorrigertYtelsePeriode(DatoIntervallEntitet periode,
                                   BigDecimal dagsats,
                                   KorrigertYtelseÅrsak årsak) {
        this.periode = Range.closed(periode.getFomDato(), periode.getTomDato());
        this.dagsats = dagsats;
        this.årsak = årsak;
    }

    public DatoIntervallEntitet getPeriode() {
        return DatoIntervallEntitet.fra(periode);
    }

    public BigDecimal getDagsats() {
        return dagsats;
    }

    public KorrigertYtelseÅrsak getÅrsak() {
        return årsak;
    }

    public static Builder ny() {
        return new Builder();
    }

    public static class Builder {

        private DatoIntervallEntitet periode;
        private BigDecimal dagsats;
        private KorrigertYtelseÅrsak årsak;

        private Builder() {
        }

        public Builder medPeriode(DatoIntervallEntitet periode) {
            if (periode == null) {
                throw new IllegalArgumentException("periode kan ikke være null");
            }
            this.periode = periode;
            return this;
        }


        public Builder medDagsats(BigDecimal dagsats) {
            if (dagsats == null) {
                throw new IllegalArgumentException("dagsats kan ikke være null");
            }
            this.dagsats = dagsats;
            return this;
        }

        public Builder medÅrsak(KorrigertYtelseÅrsak årsak) {
            if (årsak == null) {
                throw new IllegalArgumentException("årsak kan ikke være null");
            }
            this.årsak = årsak;
            return this;
        }

        public KorrigertYtelsePeriode build() {
            return new KorrigertYtelsePeriode(periode, dagsats, årsak);
        }


    }

}
