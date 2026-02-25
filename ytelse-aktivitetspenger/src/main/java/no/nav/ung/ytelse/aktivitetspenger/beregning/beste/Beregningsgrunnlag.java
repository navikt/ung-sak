package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import jakarta.persistence.*;
import no.nav.ung.sak.diff.DiffIgnore;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity(name = "Beregningsgrunnlag")
@Table(name = "GR_BEREGNINSGRUNNLAG")
public class Beregningsgrunnlag {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_BEREGNINSGRUNNLAG")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @Column(name = "virkningsdato", nullable = false, updatable = false)
    private LocalDate virkningsdato;

    @Column(name = "aarsinntekt_siste_aar", nullable = false, updatable = false)
    private BigDecimal årsinntektSisteÅr;

    @Column(name = "aarsinntekt_siste_tre_aar", nullable = false, updatable = false)
    private BigDecimal årsinntektSisteTreÅr;

    @Column(name = "aarsinntekt_beste_beregning", nullable = false, updatable = false)
    private BigDecimal årsinntektBesteBeregning;

    @Lob
    @DiffIgnore
    @Column(name = "regel_input", nullable = false, updatable = false, length = 10000)
    private String regelInput;

    @Lob
    @DiffIgnore
    @Column(name = "regel_sporing", nullable = false, updatable = false, length = 100000)
    private String regelSporing;

    @Column(name = "aktiv", nullable = false, updatable = true)
    private boolean aktiv = true;

    Beregningsgrunnlag() {
    }

    Beregningsgrunnlag(LocalDate virkningsdato, BigDecimal årsinntektSisteÅr, BigDecimal årsinntektSisteTreÅr, BigDecimal årsinntektBesteBeregning, String regelInput, String regelSporing) {
        this.virkningsdato = virkningsdato;
        this.årsinntektSisteÅr = årsinntektSisteÅr;
        this.årsinntektSisteTreÅr = årsinntektSisteTreÅr;
        this.årsinntektBesteBeregning = årsinntektBesteBeregning;
        this.regelInput = regelInput;
        this.regelSporing = regelSporing;
    }

    public LocalDate getVirkningsdato() {
        return virkningsdato;
    }

    public BigDecimal getÅrsinntektSisteÅr() {
        return årsinntektSisteÅr;
    }

    public BigDecimal getÅrsinntektSisteTreÅr() {
        return årsinntektSisteTreÅr;
    }

    public BigDecimal getÅrsinntektBesteBeregning() {
        return årsinntektBesteBeregning;
    }

    public String getRegelInput() {
        return regelInput;
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
