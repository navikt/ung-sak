package no.nav.k9.sak.ytelse.opplaeringspenger.repo.vurdering;

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

    @ManyToOne
    @Immutable
    @JoinColumn(name = "vurdert_institusjon_holder_id", updatable = false, unique = true)
    private VurdertInstitusjonHolder vurdertInstitusjonHolder;

    @ManyToOne
    @Immutable
    @JoinColumn(name = "vurdert_opplaering_holder_id", updatable = false, unique = true)
    private VurdertOpplæringHolder vurdertOpplæringHolder;

    @ManyToOne
    @Immutable
    @JoinColumn(name = "vurderte_perioder_id", updatable = false, unique = true)
    private VurdertOpplæringPerioderHolder vurdertePerioder;

    @ManyToOne
    @Immutable
    @JoinColumn(name = "vurdert_reisetid_holder_id", updatable = false, unique = true)
    private VurdertReisetidHolder vurdertReisetid;

    @Column(name = "aktiv", nullable = false)
    private Boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    VurdertOpplæringGrunnlag() {
    }

    public VurdertOpplæringGrunnlag(Long behandlingId, VurdertInstitusjonHolder vurdertInstitusjonHolder, VurdertOpplæringHolder vurdertOpplæringHolder, VurdertOpplæringPerioderHolder vurdertePerioder, VurdertReisetidHolder vurdertReisetid) {
        this.behandlingId = behandlingId;
        this.vurdertInstitusjonHolder = vurdertInstitusjonHolder;
        this.vurdertOpplæringHolder = vurdertOpplæringHolder;
        this.vurdertePerioder = vurdertePerioder;
        this.vurdertReisetid = vurdertReisetid;
    }

    public VurdertOpplæringGrunnlag(VurdertOpplæringGrunnlag grunnlag, VurdertInstitusjonHolder vurdertInstitusjonHolder, VurdertOpplæringHolder vurdertOpplæringHolder, VurdertOpplæringPerioderHolder vurdertePerioder, VurdertReisetidHolder vurdertReisetid) {
        this.behandlingId = grunnlag.behandlingId;
        this.vurdertInstitusjonHolder = vurdertInstitusjonHolder;
        this.vurdertOpplæringHolder = vurdertOpplæringHolder;
        this.vurdertePerioder = vurdertePerioder;
        this.vurdertReisetid = vurdertReisetid;
    }

    public VurdertOpplæringGrunnlag(Long behandlingId, VurdertOpplæringGrunnlag grunnlag) {
        this.behandlingId = behandlingId;
        this.vurdertInstitusjonHolder = grunnlag.vurdertInstitusjonHolder;
        this.vurdertOpplæringHolder = grunnlag.vurdertOpplæringHolder;
        this.vurdertePerioder = grunnlag.vurdertePerioder;
        this.vurdertReisetid = grunnlag.vurdertReisetid;
    }

    public VurdertInstitusjonHolder getVurdertInstitusjonHolder() {
        return vurdertInstitusjonHolder;
    }

    public VurdertOpplæringHolder getVurdertOpplæringHolder() {
        return vurdertOpplæringHolder;
    }

    public VurdertOpplæringPerioderHolder getVurdertePerioder() {
        return vurdertePerioder;
    }

    public VurdertReisetidHolder getVurdertReisetid() {
        return vurdertReisetid;
    }

    void setAktiv(Boolean aktiv) {
        this.aktiv = aktiv;
    }
}
