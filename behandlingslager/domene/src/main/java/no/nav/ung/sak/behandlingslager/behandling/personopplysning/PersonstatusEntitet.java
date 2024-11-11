package no.nav.ung.sak.behandlingslager.behandling.personopplysning;

import java.util.Objects;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import no.nav.ung.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.ung.sak.behandlingslager.kodeverk.PersonstatusKodeverdiConverter;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;

@Entity(name = "PersonopplysningPersonstatus")
@Table(name = "PO_PERSONSTATUS")
public class PersonstatusEntitet extends BaseEntitet implements HarAktørId, IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PO_PERSONSTATUS")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id", updatable = false, nullable = false)))
    private AktørId aktørId;

    @Embedded
    private DatoIntervallEntitet periode;

    @ChangeTracked
    @Convert(converter = PersonstatusKodeverdiConverter.class)
    @Column(name = "personstatus", nullable = false)
    private PersonstatusType personstatus = PersonstatusType.UDEFINERT;

    @ManyToOne(optional = false)
    @JoinColumn(name = "po_informasjon_id", nullable = false, updatable = false)
    private PersonInformasjonEntitet personopplysningInformasjon;

    PersonstatusEntitet() {
    }

    PersonstatusEntitet(PersonstatusEntitet personstatus) {
        this.aktørId = personstatus.getAktørId();
        this.periode = personstatus.getPeriode();
        this.personstatus = personstatus.getPersonstatus();
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { aktørId, personstatus, periode };
        return IndexKeyComposer.createKey(keyParts);
    }

    void setPersonInformasjon(PersonInformasjonEntitet personInformasjon) {
        this.personopplysningInformasjon = personInformasjon;
    }

    void setId(Long id) {
        this.id = id;
    }

    void setPersonstatus(PersonstatusType personstatus) {
        this.personstatus = personstatus;
    }

    @Override
    public AktørId getAktørId() {
        return aktørId;
    }

    void setAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    void setPeriode(DatoIntervallEntitet gyldighetsperiode) {
        this.periode = gyldighetsperiode;
    }

    public PersonstatusType getPersonstatus() {
        return personstatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PersonstatusEntitet entitet = (PersonstatusEntitet) o;
        return Objects.equals(aktørId, entitet.aktørId) &&
            Objects.equals(periode, entitet.periode) &&
            Objects.equals(personstatus, entitet.personstatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørId, periode, personstatus);
    }

    @Override
    public String toString() {
        var sb = new StringBuilder(getClass().getSimpleName()+"<");
        sb.append(", periode=").append(periode);
        sb.append(", personstatus=").append(personstatus);
        sb.append('>');
        return sb.toString();
    }

}
