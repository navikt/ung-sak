package no.nav.k9.sak.ytelse.opplaeringspenger.repo;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Immutable;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "VurdertReisetid")
@Table(name = "olp_vurdert_reisetid")
@Immutable
public class VurdertReisetid extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OLP_VURDERT_REISETID")
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "opplaering_fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "opplaering_tom", nullable = false))
    })
    private DatoIntervallEntitet opplæringperiode;

    @BatchSize(size = 20)
    @JoinColumn(name = "vurdert_reisetid_id", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private Set<VurdertReisetidPeriode> reiseperioder;

    @Column(name = "begrunnelse", nullable = false)
    private String begrunnelse;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    VurdertReisetid() {
    }

    public VurdertReisetid(DatoIntervallEntitet opplæringperiode, List<VurdertReisetidPeriode> reiseperioder, String begrunnelse) {
        Objects.requireNonNull(opplæringperiode);
        Objects.requireNonNull(reiseperioder);

        this.opplæringperiode = opplæringperiode;
        this.reiseperioder = reiseperioder.stream()
            .map(VurdertReisetidPeriode::new)
            .collect(Collectors.toSet());
        this.begrunnelse = begrunnelse;
    }

    public VurdertReisetid(VurdertReisetid that) {
        this.opplæringperiode = that.opplæringperiode;
        this.reiseperioder = that.reiseperioder.stream()
            .map(VurdertReisetidPeriode::new)
            .collect(Collectors.toSet());
        this.begrunnelse = that.begrunnelse;
    }

    public DatoIntervallEntitet getOpplæringperiode() {
        return opplæringperiode;
    }

    public Set<VurdertReisetidPeriode> getReiseperioder() {
        return reiseperioder;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VurdertReisetid that = (VurdertReisetid) o;
        return Objects.equals(opplæringperiode, that.opplæringperiode)
            && Objects.equals(reiseperioder, that.reiseperioder)
            && Objects.equals(begrunnelse, that.begrunnelse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(opplæringperiode, reiseperioder, begrunnelse);
    }
}
