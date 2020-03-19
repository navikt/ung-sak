package no.nav.k9.sak.behandlingslager.behandling.personopplysning;

import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.behandlingslager.kodeverk.PersonstatusKodeverdiConverter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;

@Entity(name = "PersonopplysningPersonstatus")
@Table(name = "PO_PERSONSTATUS")
public class PersonstatusEntitet extends BaseEntitet implements HarAktørId, IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PO_PERSONSTATUS")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id", updatable = false, nullable=false)))
    private AktørId aktørId;

    @Embedded
    private DatoIntervallEntitet periode;

    @ChangeTracked
    @Convert(converter = PersonstatusKodeverdiConverter.class)
    @Column(name="personstatus", nullable = false)
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
        final StringBuilder sb = new StringBuilder("PersonstatusEntitet{");
        sb.append("aktørId=").append(aktørId);
        sb.append(", periode=").append(periode);
        sb.append(", personstatus=").append(personstatus);
        sb.append('}');
        return sb.toString();
    }

}
