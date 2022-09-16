package no.nav.k9.sak.ytelse.opplaeringspenger.repo;

import java.time.LocalDate;
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

@Entity(name = "VurdertOpplæring")
@Table(name = "olp_vurdert_opplaering")
@Immutable
public class VurdertOpplæring extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OLP_VURDERT_OPPLAERING")
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @Column(name = "noedvendig_opplaering", nullable = false)
    private Boolean nødvendigOpplæring = false;

    @Column(name = "begrunnelse")
    private String begrunnelse;

    @Column(name = "institusjon", nullable = false)
    private String institusjon;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public VurdertOpplæring() {
    }

    public VurdertOpplæring(LocalDate fom, LocalDate tom, Boolean nødvendigOpplæring, String begrunnelse, String institusjon) {
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        this.nødvendigOpplæring = nødvendigOpplæring;
        this.begrunnelse = begrunnelse;
        this.institusjon = institusjon;
    }

    public VurdertOpplæring(VurdertOpplæring that) {
        this.nødvendigOpplæring = that.nødvendigOpplæring;
        this.periode = that.periode;
        this.begrunnelse = that.begrunnelse;
        this.institusjon = that.institusjon;
    }

    public VurdertOpplæring medPeriode(LocalDate fom, LocalDate tom) {
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        return this;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public Boolean getNødvendigOpplæring() {
        return nødvendigOpplæring;
    }

    public String getInstitusjon() {
        return institusjon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VurdertOpplæring that = (VurdertOpplæring) o;
        return Objects.equals(periode, that.periode)
            && Objects.equals(nødvendigOpplæring, that.nødvendigOpplæring)
            && Objects.equals(institusjon, that.institusjon);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, nødvendigOpplæring, begrunnelse, institusjon);
    }

    @Override
    public String toString() {
        return "VurdertOpplæring{" +
            "periode=" + periode +
            ", nødvendigOpplæring=" + nødvendigOpplæring +
            ", begrunnelse=" + begrunnelse +
            ", institusjon=" + institusjon +
            '}';
    }
}
