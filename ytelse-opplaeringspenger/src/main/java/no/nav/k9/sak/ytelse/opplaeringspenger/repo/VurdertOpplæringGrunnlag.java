package no.nav.k9.sak.ytelse.opplaeringspenger.repo;

import java.util.List;

import org.hibernate.annotations.Immutable;

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

    @ManyToOne
    @Immutable
    @JoinColumn(name = "vurdert_institusjon_holder_id", nullable = false, updatable = false, unique = true)
    private VurdertInstitusjonHolder vurdertInstitusjonHolder;

    @ManyToOne
    @Immutable
    @JoinColumn(name = "vurdert_opplaering_holder_id", nullable = false, updatable = false, unique = true)
    private VurdertOpplæringHolder vurdertOpplæringHolder;

    @Column(name = "begrunnelse")
    private String begrunnelse;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public VurdertOpplæringGrunnlag() {
    }

    public VurdertOpplæringGrunnlag(Long behandlingId, String begrunnelse) {
        this.behandlingId = behandlingId;
        this.begrunnelse = begrunnelse;
    }

    public VurdertInstitusjonHolder getVurdertInstitusjonHolder() {
        return vurdertInstitusjonHolder;
    }

    public VurdertOpplæringHolder getVurdertOpplæringHolder() {
        return vurdertOpplæringHolder;
    }

    public void setVurdertInstitusjonHolder(VurdertInstitusjonHolder vurdertInstitusjonHolder) {
        this.vurdertInstitusjonHolder = vurdertInstitusjonHolder;
    }

    public void setVurdertOpplæringHolder(VurdertOpplæringHolder vurdertOpplæringHolder) {
        this.vurdertOpplæringHolder = vurdertOpplæringHolder;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public void setAktiv(Boolean aktiv) {
        this.aktiv = aktiv;
    }

    public VurdertOpplæringGrunnlag medVurdertOpplæring(List<VurdertOpplæring> vurdertOpplæring) {
        this.vurdertOpplæringHolder = new VurdertOpplæringHolder(vurdertOpplæring);
        return this;
    }

    public VurdertOpplæringGrunnlag medVurdertInstitusjon(List<VurdertInstitusjon> vurdertInstitusjon) {
        this.vurdertInstitusjonHolder = new VurdertInstitusjonHolder(vurdertInstitusjon);
        return this;
    }
}
