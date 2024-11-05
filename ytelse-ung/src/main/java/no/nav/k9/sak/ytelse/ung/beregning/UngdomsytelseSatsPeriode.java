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
import no.nav.k9.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
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

    /**
     * Dagsats basert på satstype og grunnbeløp, ekskludert barnetillegg
     */
    @Column(name = "dagsats", nullable = false)
    private BigDecimal dagsats;

    @Type(PostgreSQLRangeType.class)
    @Column(name = "periode", columnDefinition = "daterange")
    private Range<LocalDate> periode;

    @Column(name = "grunnbeløp", nullable = false)
    private BigDecimal grunnbeløp;

    @Column(name = "grunnbeløp_faktor", nullable = false)
    private BigDecimal grunnbeløpFaktor;

    @Column(name = "sats_type", nullable = false)
    private UngdomsytelseSatsType satsType;

    @Column(name = "antall_barn", nullable = false)
    private int antallBarn;

    @Column(name = "dagsats_barnetillegg", nullable = false)
    private BigDecimal dagsatsBarnetillegg;

    public UngdomsytelseSatsPeriode() {
    }

    public UngdomsytelseSatsPeriode(UngdomsytelseSatsPeriode ungdomsytelseSatsPeriode) {
        this.dagsats = ungdomsytelseSatsPeriode.getDagsats();
        this.periode = ungdomsytelseSatsPeriode.getPeriode().toRange();
        this.grunnbeløp = ungdomsytelseSatsPeriode.getGrunnbeløp();
        this.grunnbeløpFaktor = ungdomsytelseSatsPeriode.getGrunnbeløpFaktor();
        this.satsType = ungdomsytelseSatsPeriode.getSatsType();
        this.antallBarn = ungdomsytelseSatsPeriode.getAntallBarn();
        this.dagsatsBarnetillegg = ungdomsytelseSatsPeriode.getDagsatsBarnetillegg();
    }

    public UngdomsytelseSatsPeriode(LocalDateInterval periode, UngdomsytelseSatser satser) {
        this.periode = DatoIntervallEntitet.fra(periode).toRange();
        this.dagsats = satser.dagsats();
        this.grunnbeløp = satser.grunnbeløp();
        this.grunnbeløpFaktor = satser.grunnbeløpFaktor();
        this.satsType = satser.satsType();
        this.antallBarn = satser.antallBarn();
        this.dagsatsBarnetillegg = satser.dagsatsBarnetillegg();
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

    public UngdomsytelseSatsType getSatsType() {
        return satsType;
    }

    public int getAntallBarn() {
        return antallBarn;
    }

    public BigDecimal getDagsatsBarnetillegg() {
        return dagsatsBarnetillegg;
    }

    public UngdomsytelseSatser satser(){
        return new UngdomsytelseSatser(dagsats, grunnbeløp, grunnbeløpFaktor, satsType, antallBarn, dagsatsBarnetillegg);
    }
}
