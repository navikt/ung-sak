package no.nav.k9.sak.ytelse.opplaeringspenger.repo;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "VurdertOpplæringGrunnlag")
@Table(name = "GR_OPPLAERING")
public class VurdertOpplæringGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_OPPLAERING")
    private Long id;

    @Column(name = "behandling_id", updatable = false, nullable = false)
    private Long behandlingId;

    @Column(name = "aktiv", nullable = false)
    private Boolean aktiv = true;

    @Column(name = "godkjent_institusjon", nullable = false)
    private Boolean godkjentInstitusjon = false;

    @JoinColumn(name = "holder_id", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
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
            .map(vurdertOpplæring -> new VurdertOpplæring(vurdertOpplæring))
            .toList();
        this.begrunnelse = begrunnelse;
    }

    public List<VurdertOpplæring> getVurdertOpplæring() {
        return vurdertOpplæring;
    }

    public Boolean getGodkjentInstitusjon() {
        return godkjentInstitusjon;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public void setAktiv(Boolean aktiv) {
        this.aktiv = aktiv;
    }

    public void setVurdertOpplæring(List<VurdertOpplæring> vurdertOpplæring) {
        this.vurdertOpplæring = vurdertOpplæring;
    }
}
