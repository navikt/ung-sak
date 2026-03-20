package no.nav.ung.ytelse.aktivitetspenger.beregning.minstesats;

import jakarta.persistence.*;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.ung.kodeverk.hjemmel.Hjemmel;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.kodeverk.HjemmelKodeverdiConverter;
import no.nav.ung.sak.behandlingslager.kodeverk.UngdomsytelseSatsTypeKodeverdiConverter;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.PostgreSQLRangeType;
import no.nav.ung.sak.domene.typer.tid.Range;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity(name = "AktivitetspengerGrunnsatsPeriode")
@Table(name = "AVP_GRUNNSATS_PERIODE")
public class AktivitetspengerGrunnsatsPeriode extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_AVP_GRUNNSATS_PERIODE")
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

    @Convert(converter = UngdomsytelseSatsTypeKodeverdiConverter.class)
    @Column(name = "sats_type", nullable = false)
    private UngdomsytelseSatsType satsType;

    @Convert(converter = HjemmelKodeverdiConverter.class)
    @Column(name = "hjemmel", nullable = false)
    private Hjemmel hjemmel = Hjemmel.UNG_FORSKRIFT_PARAGRAF_9;

    @Column(name = "antall_barn", nullable = false)
    private int antallBarn;

    @Column(name = "dagsats_barnetillegg", nullable = false)
    private int dagsatsBarnetillegg;

    public AktivitetspengerGrunnsatsPeriode() {
    }

    public AktivitetspengerGrunnsatsPeriode(AktivitetspengerGrunnsatsPeriode other) {
        this.dagsats = other.getDagsats();
        this.periode = other.getPeriode().toRange();
        this.grunnbeløp = other.getGrunnbeløp();
        this.grunnbeløpFaktor = other.getGrunnbeløpFaktor();
        this.satsType = other.getSatsType();
        this.antallBarn = other.getAntallBarn();
        this.dagsatsBarnetillegg = other.getDagsatsBarnetillegg();
    }

    public AktivitetspengerGrunnsatsPeriode(LocalDateInterval periode, AktivitetspengerSatser satser) {
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

    public int getDagsatsBarnetillegg() {
        return dagsatsBarnetillegg;
    }

    public UngdomsytelseSatser satser() {
        return new UngdomsytelseSatser(dagsats, grunnbeløp, grunnbeløpFaktor, satsType, antallBarn, dagsatsBarnetillegg);
    }
}

