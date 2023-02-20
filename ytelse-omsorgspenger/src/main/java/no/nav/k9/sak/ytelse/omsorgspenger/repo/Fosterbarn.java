package no.nav.k9.sak.ytelse.omsorgspenger.repo;

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
import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.HarAktørId;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.typer.AktørId;

@Entity(name = "Fosterbarn")
@Table(name = "OMP_FOSTERBARN")
@Immutable
public class Fosterbarn extends BaseEntitet implements HarAktørId, IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OMP_FOSTERBARN")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id", updatable = false)))
    private AktørId aktørId;

    Fosterbarn() {
    }

    public Fosterbarn(AktørId aktørId) {
        this.aktørId = aktørId;
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = {getAktørId()};
        return IndexKeyComposer.createKey(keyParts);
    }

    Long getId() {
        return id;
    }

    @Override
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
        Fosterbarn entitet = (Fosterbarn) o;
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
