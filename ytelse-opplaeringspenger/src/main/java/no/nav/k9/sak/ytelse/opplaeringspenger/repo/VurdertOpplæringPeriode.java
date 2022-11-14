package no.nav.k9.sak.ytelse.opplaeringspenger.repo;

import java.time.LocalDate;
import java.util.Objects;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "VurdertOpplæringPeriode")
@Table(name = "olp_vurdert_opplaering_periode")
@Immutable
public class VurdertOpplæringPeriode extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OLP_VURDERT_OPPLAERING_PERIODE")
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @Column(name = "gjennomfoert_opplaering", nullable = false)
    private Boolean gjennomførtOpplæring;

    @JoinColumn(name = "vurdert_reisetid_id", unique = true)
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private VurdertReisetid reisetid;

    @Column(name = "begrunnelse")
    private String begrunnelse;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    VurdertOpplæringPeriode() {
    }

    public VurdertOpplæringPeriode(DatoIntervallEntitet periode, Boolean gjennomførtOpplæring, VurdertReisetid reisetid, String begrunnelse) {
        this.periode = periode;
        this.gjennomførtOpplæring = gjennomførtOpplæring;
        this.reisetid = reisetid;
        this.begrunnelse = begrunnelse;
    }

    public VurdertOpplæringPeriode(LocalDate fom, LocalDate tom, Boolean gjennomførtOpplæring, VurdertReisetid reisetid, String begrunnelse) {
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        this.gjennomførtOpplæring = gjennomførtOpplæring;
        this.begrunnelse = begrunnelse;
        this.reisetid = reisetid;
    }

    public VurdertOpplæringPeriode(VurdertOpplæringPeriode that) {
        this.periode = that.periode;
        this.gjennomførtOpplæring = that.gjennomførtOpplæring;
        this.begrunnelse = that.begrunnelse;
        this.reisetid = that.reisetid != null ? new VurdertReisetid(that.reisetid) : null;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public Boolean getGjennomførtOpplæring() {
        return gjennomførtOpplæring;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public VurdertReisetid getReisetid() {
        return reisetid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VurdertOpplæringPeriode that = (VurdertOpplæringPeriode) o;
        return Objects.equals(periode, that.periode)
            && Objects.equals(gjennomførtOpplæring, that.gjennomførtOpplæring)
            && Objects.equals(reisetid, that.reisetid)
            && Objects.equals(begrunnelse, that.begrunnelse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, gjennomførtOpplæring, reisetid, begrunnelse);
    }
}
