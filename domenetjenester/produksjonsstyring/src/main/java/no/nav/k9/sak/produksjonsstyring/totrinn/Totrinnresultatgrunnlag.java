package no.nav.k9.sak.produksjonsstyring.totrinn;

import java.util.Optional;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;

@Entity(name = "Totrinnresultatgrunnlag")
@Table(name = "TOTRINNRESULTATGRUNNLAG")
public class Totrinnresultatgrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_TOTRINNRESULTATGRUNNLAG")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "behandling_id", nullable = false, updatable = false)
    private Behandling behandling;

    @Column(name = "beregningsgrunnlag_grunnlag_uuid", updatable = false)
    private UUID beregningsgrunnlagGrunnlagUuid;

    @Column(name = "iay_grunnlag_uuid", insertable = true, updatable = false)
    private UUID iayGrunnlagUuid;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;


    Totrinnresultatgrunnlag() {
        // for hibernate
    }


    public Totrinnresultatgrunnlag(Behandling behandling,
                                   UUID beregningsgrunnlagGrunnlagUuid,
                                   UUID iayGrunnlagUuid) {
        this.behandling = behandling;
        this.beregningsgrunnlagGrunnlagUuid = beregningsgrunnlagGrunnlagUuid;
        this.iayGrunnlagUuid = iayGrunnlagUuid;
    }

    public Long getId() {
        return id;
    }

    public boolean isAktiv() {
        return aktiv;
    }

    public Optional<UUID> getBeregningsgrunnlagUuid() {
        return Optional.ofNullable(beregningsgrunnlagGrunnlagUuid);
    }

    public void setBeregningsgrunnlag(UUID beregningsgrunnlagGrunnlagUuid) {
        this.beregningsgrunnlagGrunnlagUuid = beregningsgrunnlagGrunnlagUuid;
    }

    public Optional<UUID> getGrunnlagUuid() {
        return Optional.ofNullable(this.iayGrunnlagUuid);
    }

    public void setGrunnlagUuid(UUID iayGrunnlagUuid) {
        this.iayGrunnlagUuid = iayGrunnlagUuid;
    }

    public void deaktiver() {
        this.aktiv = false;
    }
}
