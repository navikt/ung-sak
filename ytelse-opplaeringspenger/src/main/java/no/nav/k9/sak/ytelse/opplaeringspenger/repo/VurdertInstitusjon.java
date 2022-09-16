package no.nav.k9.sak.ytelse.opplaeringspenger.repo;

import java.util.Objects;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "VurdertInstitusjon")
@Table(name = "olp_vurdert_institusjon")
@Immutable
public class VurdertInstitusjon extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OLP_VURDERT_INSTITUSJON")
    private Long id;

    @Column(name = "institusjon", nullable = false)
    private String institusjon;

    @Column(name = "godkjent", nullable = false)
    private Boolean godkjent = false;

    @Column(name = "begrunnelse")
    private String begrunnelse;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public VurdertInstitusjon() {
    }

    public VurdertInstitusjon(String institusjon, Boolean godkjent, String begrunnelse) {
        this.institusjon = institusjon;
        this.godkjent = godkjent;
        this.begrunnelse = begrunnelse;
    }

    public VurdertInstitusjon(VurdertInstitusjon vurdertInstitusjon) {
        this.institusjon = vurdertInstitusjon.institusjon;
        this.godkjent = vurdertInstitusjon.godkjent;
        this.begrunnelse = vurdertInstitusjon.begrunnelse;
    }

    public String getInstitusjon() {
        return institusjon;
    }

    public Boolean getGodkjent() {
        return godkjent;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VurdertInstitusjon that = (VurdertInstitusjon) o;
        return Objects.equals(institusjon, that.institusjon)
            && Objects.equals(godkjent, that.godkjent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(institusjon, godkjent, begrunnelse);
    }

    @Override
    public String toString() {
        return "VurdertInstitusjon{" +
            "institusjon=" + institusjon +
            ", godkjent=" + godkjent +
            ", begrunnelse=" + begrunnelse +
            '}';
    }
}
