package no.nav.k9.sak.ytelse.opplaeringspenger.repo;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "VurdertOpplæringGrunnlag")
@Table(name = "olp_vurdert_opplaering_grunnlag")
public class VurdertOpplæringGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OLP_VURDERT_OPPLAERING_GRUNNLAG")
    private Long id;

    @Column(name = "behandling_id", updatable = false, nullable = false)
    private Long behandlingId;

    @Column(name = "aktiv", nullable = false)
    private Boolean aktiv = true;

    @Column(name = "godkjent_institusjon", nullable = false)
    private Boolean godkjentInstitusjon = false;

    @OneToMany(mappedBy = "vurdertOpplæringGrunnlag", cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private List<VurdertOpplæring> vurdertOpplæring;

    @Column(name = "begrunnelse")
    private String begrunnelse;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public VurdertOpplæringGrunnlag() {
    }

    public VurdertOpplæringGrunnlag(Long behandlingId, Boolean godkjentInstitusjon, List<VurdertOpplæring> vurdertOpplæringList, String begrunnelse) {
        this.behandlingId = behandlingId;
        this.godkjentInstitusjon = godkjentInstitusjon;
        this.vurdertOpplæring = vurdertOpplæringList
            .stream()
            .map(vurdertOpplæring -> new VurdertOpplæring(vurdertOpplæring).medGrunnlag(this))
            .toList();
        this.begrunnelse = begrunnelse;
    }

    public List<VurdertOpplæring> getVurdertOpplæring() {
        return vurdertOpplæring;
    }

    public Boolean getGodkjentInstitusjon() {
        return godkjentInstitusjon;
    }

    public void setAktiv(Boolean aktiv) {
        this.aktiv = aktiv;
    }

    public void setVurdertOpplæring(List<VurdertOpplæring> vurdertOpplæring) {
        this.vurdertOpplæring = vurdertOpplæring;
    }
}
