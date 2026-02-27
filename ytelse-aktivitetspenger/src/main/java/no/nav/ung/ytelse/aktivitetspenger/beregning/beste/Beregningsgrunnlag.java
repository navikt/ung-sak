package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import jakarta.persistence.*;
import no.nav.ung.sak.diff.DiffIgnore;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;

@Entity(name = "Beregningsgrunnlag")
@Table(name = "GR_BEREGNINGSGRUNNLAG")
public class Beregningsgrunnlag {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_BEREGNINGSGRUNNLAG")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @Column(name = "virkningsdato", nullable = false, updatable = false)
    private LocalDate virkningsdato;

    @Column(name = "siste_lignede_aar", nullable = false, updatable = false)
    private Year sisteLignedeÅr;

    @Column(name = "aarsinntekt_siste_aar", nullable = false, updatable = false)
    private BigDecimal årsinntektAvkortetOppjustertSisteÅr;

    @Column(name = "aarsinntekt_siste_tre_aar", nullable = false, updatable = false)
    private BigDecimal årsinntektAvkortetOppjustertSisteTreÅr;

    @Column(name = "aarsinntekt_beste_beregning", nullable = false, updatable = false)
    private BigDecimal årsinntektAvkortetOppjustertBesteBeregning;

    @Embedded
    private BeregningsgrunnlagInput beregningInput;

    @Lob
    @DiffIgnore
    @Column(name = "regel_sporing", nullable = false, updatable = false, length = 100000)
    private String regelSporing;

    @Column(name = "aktiv", nullable = false, updatable = true)
    private boolean aktiv = true;

    protected Beregningsgrunnlag() {
    }

    Beregningsgrunnlag(BeregningInput beregningInputGrunnlag, BigDecimal årsinntektSisteÅr, BigDecimal årsinntektSisteTreÅr, BigDecimal årsinntektBesteBeregning, String regelSporing) {
        this.virkningsdato = beregningInputGrunnlag.virkningsdato();
        this.sisteLignedeÅr = beregningInputGrunnlag.sisteLignedeÅr();
        this.årsinntektAvkortetOppjustertSisteÅr = årsinntektSisteÅr;
        this.årsinntektAvkortetOppjustertSisteTreÅr = årsinntektSisteTreÅr;
        this.årsinntektAvkortetOppjustertBesteBeregning = årsinntektBesteBeregning;
        this.regelSporing = regelSporing;
        this.beregningInput = new BeregningsgrunnlagInput(beregningInputGrunnlag);
    }

    public LocalDate getVirkningsdato() {
        return virkningsdato;
    }

    public BigDecimal getÅrsinntektAvkortetOppjustertSisteÅr() {
        return årsinntektAvkortetOppjustertSisteÅr;
    }

    public BigDecimal getÅrsinntektAvkortetOppjustertSisteTreÅr() {
        return årsinntektAvkortetOppjustertSisteTreÅr;
    }

    public BigDecimal getÅrsinntektAvkortetOppjustertBesteBeregning() {
        return årsinntektAvkortetOppjustertBesteBeregning;
    }

    public BeregningsgrunnlagInput getBeregningInput() {
        return beregningInput;
    }

    public String getRegelSporing() {
        return regelSporing;
    }

    void setIkkeAktivt() {
        this.aktiv = false;
    }

    void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }
}
