package no.nav.foreldrepenger.behandlingslager.behandling.medisinsk;

import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.k9.sak.typer.AktørId;

@Entity(name = "Pleietrengende")
@Table(name = "MD_PLEIETRENGENDE")
public class Pleietrengende extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MD_PLEIETRENGENDE")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id")))
    private AktørId aktørId;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    Pleietrengende() {
        // Hibernate
    }

    public Pleietrengende(AktørId aktørId) {
        Objects.requireNonNull(aktørId);
        this.aktørId = aktørId;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pleietrengende that = (Pleietrengende) o;
        return Objects.equals(aktørId, that.aktørId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørId);
    }
}
