package no.nav.k9.sak.behandlingslager.saksnummer;

import java.util.Objects;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.typer.AktørId;

@Entity(name = "ReservertSaksnummerAktør")
@Table(name = "RESERVERT_SAKSNUMMER_AKTOR")
public class ReservertSaksnummerAktørEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_RESERVERT_SAKSNUMMER_AKTOR")
    @Column(name = "id")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id", nullable = false, updatable = false)))
    private AktørId aktørId;

    ReservertSaksnummerAktørEntitet() {
        // for hibernate
    }

    ReservertSaksnummerAktørEntitet(AktørId aktørId) {
        this.aktørId = aktørId;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ReservertSaksnummerAktørEntitet entitet = (ReservertSaksnummerAktørEntitet) o;
        return Objects.equals(aktørId, entitet.aktørId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørId);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + "id=" + id + '>';
    }
}
