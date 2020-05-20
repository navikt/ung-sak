package no.nav.k9.sak.produksjonsstyring.totrinn;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;

@Entity(name = "Totrinnresultatgrunnlag")
@Table(name = "TOTRINNRESULTATGRUNNLAG")
public class Totrinnresultatgrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_TOTRINNRESULTATGRUNNLAG")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "behandling_id", nullable = false, updatable = false)
    private Behandling behandling;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    @JoinColumn(name = "bg_grunnlag_id", nullable = false, updatable = false)
    private List<BeregningsgrunnlagToTrinn> beregningsgrunnlagVersjoner;

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
                                   List<BeregningsgrunnlagToTrinn> beregningsgrunnlagGrunnlagUuid,
                                   UUID iayGrunnlagUuid) {
        this.behandling = behandling;
        this.beregningsgrunnlagVersjoner = beregningsgrunnlagGrunnlagUuid;
        this.iayGrunnlagUuid = iayGrunnlagUuid;
    }

    public Long getId() {
        return id;
    }

    public boolean isAktiv() {
        return aktiv;
    }

    public List<BeregningsgrunnlagToTrinn> getBeregningsgrunnlagList() {
        return List.copyOf(beregningsgrunnlagVersjoner);
    }

    public void setBeregningsgrunnlagList(List<BeregningsgrunnlagToTrinn> beregningsgrunnlagGrunnlagUuid) {
        this.beregningsgrunnlagVersjoner = beregningsgrunnlagGrunnlagUuid;
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
