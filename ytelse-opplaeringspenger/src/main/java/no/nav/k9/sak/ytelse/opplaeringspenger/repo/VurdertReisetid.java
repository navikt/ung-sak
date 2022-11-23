package no.nav.k9.sak.ytelse.opplaeringspenger.repo;

import java.util.Objects;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
        @AttributeOverride(name = "fomDato", column = @Column(name = "reise_til_fom")),
        @AttributeOverride(name = "tomDato", column = @Column(name = "reise_til_tom"))
    })
    private DatoIntervallEntitet reiseperiodeTil;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "reise_hjem_fom")),
        @AttributeOverride(name = "tomDato", column = @Column(name = "reise_hjem_tom"))
    })
    private DatoIntervallEntitet reiseperiodeHjem;

    @Column(name = "begrunnelse", nullable = false)
    private String begrunnelse;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    VurdertReisetid() {
    }

    public VurdertReisetid(DatoIntervallEntitet reiseperiodeTil, DatoIntervallEntitet reiseperiodeHjem, String begrunnelse) {
        this.reiseperiodeTil = reiseperiodeTil;
        this.reiseperiodeHjem = reiseperiodeHjem;
        this.begrunnelse = begrunnelse;
    }

    public VurdertReisetid(VurdertReisetid that) {
        this.reiseperiodeTil = that.reiseperiodeTil;
        this.reiseperiodeHjem = that.reiseperiodeHjem;
        this.begrunnelse = that.begrunnelse;
    }

    public DatoIntervallEntitet getReiseperiodeTil() {
        return reiseperiodeTil;
    }

    public DatoIntervallEntitet getReiseperiodeHjem() {
        return reiseperiodeHjem;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VurdertReisetid that = (VurdertReisetid) o;
        return Objects.equals(reiseperiodeTil, that.reiseperiodeTil)
            && Objects.equals(reiseperiodeHjem, that.reiseperiodeHjem)
            && Objects.equals(begrunnelse, that.begrunnelse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reiseperiodeTil, reiseperiodeHjem, begrunnelse);
    }
}
