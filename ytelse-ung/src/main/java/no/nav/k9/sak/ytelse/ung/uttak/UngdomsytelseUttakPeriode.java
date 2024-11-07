package no.nav.k9.sak.ytelse.ung.uttak;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hibernate.annotations.Type;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.PostgreSQLRangeType;
import no.nav.k9.sak.behandlingslager.Range;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.kodeverk.ungdomsytelse.uttak.UngdomsytelseUttakAvslagsårsak;
import no.nav.k9.sak.ytelse.ung.kodeverk.UngdomsytelseUttakAvslagsårsakKodeverdiConverter;

@Entity(name = "UngdomsytelseUttakPeriode")
@Table(name = "UNG_UTTAK_PERIODE")
public class UngdomsytelseUttakPeriode extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UNG_UTTAK_PERIODE")
    private Long id;

    @Type(PostgreSQLRangeType.class)
    @Column(name = "periode", columnDefinition = "daterange")
    private Range<LocalDate> periode;

    @Column(name = "utbetalingsgrad", nullable = false)
    private BigDecimal utbetalingsgrad;

    @Convert(converter = UngdomsytelseUttakAvslagsårsakKodeverdiConverter.class)
    @Column(name = "avslag_aarsak")
    private UngdomsytelseUttakAvslagsårsak avslagsårsak;

    public UngdomsytelseUttakPeriode() {
    }

    public UngdomsytelseUttakPeriode(UngdomsytelseUttakPeriode ungdomsytelseUttakPeriode) {
        this.periode = ungdomsytelseUttakPeriode.getPeriode().toRange();
        this.utbetalingsgrad = ungdomsytelseUttakPeriode.getUtbetalingsgrad();
    }

    public UngdomsytelseUttakPeriode(BigDecimal utbetalingsgrad,
                                     DatoIntervallEntitet periode) {
        this.utbetalingsgrad = utbetalingsgrad;
        this.periode = periode.toRange();
    }

    public UngdomsytelseUttakPeriode(UngdomsytelseUttakAvslagsårsak avslagsårsak,
                                     DatoIntervallEntitet periode) {
        this.utbetalingsgrad = BigDecimal.ZERO;
        this.periode = periode.toRange();
        this.avslagsårsak = avslagsårsak;
    }


    public DatoIntervallEntitet getPeriode() {
        return DatoIntervallEntitet.fra(periode);
    }

    public BigDecimal getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    public UngdomsytelseUttakAvslagsårsak getAvslagsårsak() {
        return avslagsårsak;
    }
}
