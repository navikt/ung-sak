package no.nav.foreldrepenger.behandlingslager.behandling.medisinsk;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;

@Entity(name = "OmsorgenFor")
@Table(name = "MD_OMSORGENFOR")
public class OmsorgenFor extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MD_OMSORGENFOR")
    private Long id;

    @Column(name = "har_omsorg_for", nullable = false)
    private boolean harOmsorgFor;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public OmsorgenFor() {
    }

    public OmsorgenFor(boolean harOmsorgFor) {
        this.harOmsorgFor = harOmsorgFor;
    }

    public boolean getHarOmsorgFor() {
        return harOmsorgFor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OmsorgenFor that = (OmsorgenFor) o;
        return harOmsorgFor == that.harOmsorgFor;
    }

    @Override
    public int hashCode() {
        return Objects.hash(harOmsorgFor);
    }

    @Override
    public String toString() {
        return "OmsorgenFor{" +
            "id=" + id +
            ", harOmsorgFor=" + harOmsorgFor +
            ", versjon=" + versjon +
            '}';
    }
}
