package no.nav.k9.sak.ytelse.ung.beregning;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hibernate.annotations.Type;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.PostgreSQLRangeType;
import no.nav.k9.sak.behandlingslager.Range;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "UngdomsytelseSatsPeriode")
@Table(name = "UNG_SATS_PERIODE")
public class UngdomsytelseSatsPeriode extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UNG_SATS_PERIODE")
    private Long id;

    @Column(name = "dagsats", nullable = false)
    private BigDecimal dagsats;

    @Type(PostgreSQLRangeType.class)
    @Column(name = "periode", columnDefinition = "daterange")
    private Range<LocalDate> periode;

    @Column(name = "grunnbeløp", nullable = false)
    private BigDecimal grunnbeløp;

    @Column(name = "grunnbeløp_faktor", nullable = false)
    private BigDecimal grunnbeløpFaktor;

    public UngdomsytelseSatsPeriode(UngdomsytelseSatsPeriode ungdomsytelseSatsPeriode) {
        this.dagsats = ungdomsytelseSatsPeriode.getDagsats();
        this.periode = ungdomsytelseSatsPeriode.getPeriode().toRange();
        this.grunnbeløp = ungdomsytelseSatsPeriode.getGrunnbeløp();
        this.grunnbeløpFaktor = ungdomsytelseSatsPeriode.getGrunnbeløpFaktor();
    }

    public UngdomsytelseSatsPeriode(BigDecimal dagsats,
                                    DatoIntervallEntitet periode,
                                    BigDecimal grunnbeløp,
                                    BigDecimal grunnbeløpFaktor) {
        this.dagsats = dagsats;
        this.periode = periode.toRange();
        this.grunnbeløp = grunnbeløp;
        this.grunnbeløpFaktor = grunnbeløpFaktor;
    }

    public UngdomsytelseSatsPeriode(LocalDateInterval periode, UngdomsytelseSatser satser) {
        this.periode = DatoIntervallEntitet.fra(periode).toRange();
        this.dagsats = satser.dagsats();
        this.grunnbeløp = satser.grunnbeløp();
        this.grunnbeløpFaktor = satser.grunnbeløpFaktor();
    }

    public BigDecimal getDagsats() {
        return dagsats;
    }

    public DatoIntervallEntitet getPeriode() {
        return DatoIntervallEntitet.fra(periode);
    }

    public BigDecimal getGrunnbeløp() {
        return grunnbeløp;
    }

    public BigDecimal getGrunnbeløpFaktor() {
        return grunnbeløpFaktor;
    }

    public UngdomsytelseSatser satser(){
        return new UngdomsytelseSatser(dagsats, grunnbeløp, grunnbeløpFaktor);
    }
}
