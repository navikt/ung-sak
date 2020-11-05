package no.nav.k9.sak.ytelse.unntaksbehandling.repo;

import static java.util.Objects.requireNonNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "ManuellVilkårsvurderingGrunnlag")
@Table(name = "GR_MAN_VILKARSVURDERING")
public class ManuellVilkårsvurderingGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_MAN_VILKARSVURDERING")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @JoinColumn(name = "fritekst_id", nullable = false)
    @OneToOne
    private VilkårsvurderingFritekst fritekstEntitet;

    ManuellVilkårsvurderingGrunnlag() {
    }

    public ManuellVilkårsvurderingGrunnlag(Long behandlingId, VilkårsvurderingFritekst fritekstEntitet) {
        requireNonNull(behandlingId, "behandlingsId kan ikke være null");
        requireNonNull(fritekstEntitet, "fritekstEntitet kan ikke være null");

        this.behandlingId = behandlingId;
        this.fritekstEntitet = fritekstEntitet;
    }

    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    @Override
    public String toString() {
        return "VilkårsvurderingGrunnlag{" +
            "id=" + id +
            ", behandlingId=" + behandlingId +
            ", aktiv=" + aktiv +
            ", versjon=" + versjon +
            '}';
    }

    public VilkårsvurderingFritekst getFritekstEntitet() {
        return fritekstEntitet;
    }
}
