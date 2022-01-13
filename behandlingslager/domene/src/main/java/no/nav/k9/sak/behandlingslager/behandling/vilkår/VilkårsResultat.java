package no.nav.k9.sak.behandlingslager.behandling.vilkår;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "ResultatVilkårResultat")
@Table(name = "RS_VILKARS_RESULTAT")
public class VilkårsResultat extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_RS_VILKARS_RESULTAT")
    private Long id;

    @Column(name = "behandling_id", updatable = false, nullable = false)
    private Long behandlingId;

    @ManyToOne
    @JoinColumn(name = "vilkarene_id", updatable = false)
    private Vilkårene vilkårene;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;
    
    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public VilkårsResultat() {
    }

    public VilkårsResultat(Long behandlingId, Vilkårene vilkårene) {
        this.behandlingId = behandlingId;
        this.vilkårene = vilkårene;
    }

    public Vilkårene getVilkårene() {
        return vilkårene;
    }

    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    @Override
    public String toString() {
        return "VilkårsResultat{" +
            "id=" + id +
            ", behandlingId=" + behandlingId +
            ", vilkårene=" + vilkårene +
            ", aktiv=" + aktiv +
            '}';
    }
}
