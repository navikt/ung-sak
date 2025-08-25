package no.nav.ung.sak.behandlingslager.behandling.personopplysning;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.api.IndexKey;
import no.nav.ung.kodeverk.person.NavBrukerKjønn;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.aktør.AktørIdConverter;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import no.nav.ung.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.ung.sak.behandlingslager.kodeverk.KjønnKodeverdiConverter;
import no.nav.ung.sak.typer.AktørId;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDate;
import java.util.Objects;

@Entity(name = "Personopplysning")
@Table(name = "PO_PERSONOPPLYSNING")
@DynamicInsert
@DynamicUpdate
public class PersonopplysningEntitet extends BaseEntitet implements HarAktørId, IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PO_PERSONOPPLYSNING")
    private Long id;

    @Convert(converter = AktørIdConverter.class, attributeName = "aktørId")
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id", updatable = false)))
    private AktørId aktørId;

    @ChangeTracked
    @Convert(converter = KjønnKodeverdiConverter.class)
    @Column(name = "bruker_kjoenn")
    private NavBrukerKjønn brukerKjønn = NavBrukerKjønn.UDEFINERT;

    @ChangeTracked()
    @Column(name = "navn")
    private String navn;

    @ChangeTracked
    @Column(name = "doedsdato")
    private LocalDate dødsdato;

    @ChangeTracked
    @Column(name = "foedselsdato", nullable = false)
    private LocalDate fødselsdato;

    @ManyToOne(optional = false)
    @JoinColumn(name = "po_informasjon_id", nullable = false, updatable = false)
    private PersonInformasjonEntitet personopplysningInformasjon;

    protected PersonopplysningEntitet() {
    }

    protected PersonopplysningEntitet(PersonopplysningEntitet personopplysning) {
        this.aktørId = personopplysning.getAktørId();
        this.navn = personopplysning.getNavn();
        this.brukerKjønn = personopplysning.getKjønn();
        this.fødselsdato = personopplysning.getFødselsdato();
        this.dødsdato = personopplysning.getDødsdato();
    }

    private boolean harAltValgtKjønn() {
        return !NavBrukerKjønn.UDEFINERT.equals(brukerKjønn);
    }

    void setBrukerKjønn(NavBrukerKjønn brukerKjønn) {
        if (!harAltValgtKjønn()) {
            this.brukerKjønn = brukerKjønn;
        }
    }

    void setPersonopplysningInformasjon(PersonInformasjonEntitet personopplysningInformasjon) {
        if (this.personopplysningInformasjon != null && !Objects.equals(this.personopplysningInformasjon, personopplysningInformasjon)) {
            throw new IllegalStateException("Kan ikke endre personopplysningInformasjon for aktørId=" + this.getAktørId());
        }
        this.personopplysningInformasjon = personopplysningInformasjon;
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = {getAktørId()};
        return IndexKeyComposer.createKey(keyParts);
    }

    @Override
    public AktørId getAktørId() {
        return aktørId;
    }

    void setAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
    }

    public String getNavn() {
        return navn;
    }

    void setNavn(String navn) {
        this.navn = navn;
    }

    public NavBrukerKjønn getKjønn() {
        return brukerKjønn;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    void setFødselsdato(LocalDate fødselsdato) {
        this.fødselsdato = fødselsdato;
    }

    public LocalDate getDødsdato() {
        return dødsdato;
    }

    void setDødsdato(LocalDate dødsdato) {
        this.dødsdato = dødsdato;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PersonopplysningEntitet entitet = (PersonopplysningEntitet) o;
        return Objects.equals(brukerKjønn, entitet.brukerKjønn) &&
            Objects.equals(aktørId, entitet.aktørId) &&
            Objects.equals(navn, entitet.navn) &&
            Objects.equals(fødselsdato, entitet.fødselsdato) &&
            Objects.equals(dødsdato, entitet.dødsdato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(brukerKjønn, aktørId, navn, fødselsdato, dødsdato);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + "id=" + id +
            ", brukerKjønn=" + brukerKjønn +
            ", navn='" + navn + '\'' +
            ", fødselsdato=" + fødselsdato +
            ", dødsdato=" + dødsdato +
            '>';
    }

    public int compareTo(PersonopplysningEntitet other) {
        return other.getAktørId().compareTo(this.getAktørId());
    }
}
