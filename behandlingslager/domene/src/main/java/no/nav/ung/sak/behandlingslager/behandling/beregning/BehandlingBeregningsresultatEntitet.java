package no.nav.ung.sak.behandlingslager.behandling.beregning;

import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.ung.sak.behandlingslager.BaseEntitet;


@Entity(name = "BeregningsresultatAggregatEntitet")
@Table(name = "BR_RESULTAT_BEHANDLING")
public class BehandlingBeregningsresultatEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BR_RESULTAT_BEHANDLING")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;


    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @OneToOne(optional = false)
    @JoinColumn(name = "bg_beregningsresultat_fp_id", nullable = false, updatable = false, unique = true)
    private BeregningsresultatEntitet bgBeregningsresultat;

    @OneToOne
    @JoinColumn(name = "utbet_beregningsresultat_fp_id", updatable = false, unique = true)
    private BeregningsresultatEntitet utbetBeregningsresultat;

    @Column(name = "hindre_tilbaketrekk")
    private Boolean skalHindreTilbaketrekk = Boolean.FALSE;

    BehandlingBeregningsresultatEntitet() {
        // NOSONAR
    }

    public BehandlingBeregningsresultatEntitet(BehandlingBeregningsresultatEntitet kladd) {
        this.behandlingId = kladd.behandlingId;
        this.bgBeregningsresultat = kladd.bgBeregningsresultat;
        this.skalHindreTilbaketrekk = kladd.skalHindreTilbaketrekk;
    }

    public Long getId() {
        return id;
    }

    public BeregningsresultatEntitet getBgBeregningsresultat() {
        return bgBeregningsresultat;
    }


    public BeregningsresultatEntitet getUtbetBeregningsresultat() {
        return utbetBeregningsresultat;
    }

    public Optional<Boolean> skalHindreTilbaketrekk() {
        return Optional.ofNullable(skalHindreTilbaketrekk);
    }

    public boolean erAktivt() {
        return aktiv;
    }

    public void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    void setBehandling(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    void setBgBeregningsresultatFP(BeregningsresultatEntitet bgBeregningsresultatFP) {
        this.bgBeregningsresultat = bgBeregningsresultatFP;
    }

    void setUtbetBeregningsresultatFP(BeregningsresultatEntitet utbetBeregningsresultatFP) {
        this.utbetBeregningsresultat = utbetBeregningsresultatFP;
    }

    void setSkalHindreTilbaketrekk(Boolean skalHindreTilbaketrekk) {
        this.skalHindreTilbaketrekk = skalHindreTilbaketrekk;
    }

    public void deaktiver() {
        this.aktiv = false;
    }
}
