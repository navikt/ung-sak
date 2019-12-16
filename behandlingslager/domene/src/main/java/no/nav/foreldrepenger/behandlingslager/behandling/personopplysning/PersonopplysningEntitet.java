package no.nav.foreldrepenger.behandlingslager.behandling.personopplysning;

import java.time.LocalDate;
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

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.HarAktørId;

@Entity(name = "Personopplysning")
@Table(name = "PO_PERSONOPPLYSNING")
public class PersonopplysningEntitet extends BaseEntitet implements HarAktørId, IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PO_PERSONOPPLYSNING")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id", updatable = false)))
    private AktørId aktørId;

    @ChangeTracked
    @Convert(converter = NavBrukerKjønn.KodeverdiConverter.class)
    @Column(name = "bruker_kjoenn")
    private NavBrukerKjønn brukerKjønn = NavBrukerKjønn.UDEFINERT;

    @ChangeTracked
    @Convert(converter = SivilstandType.KodeverdiConverter.class)
    @Column(name = "sivilstand_type", nullable = false)
    private SivilstandType sivilstand = SivilstandType.UOPPGITT;

    @ChangeTracked()
    @Column(name = "navn")
    private String navn;

    @ChangeTracked
    @Column(name = "doedsdato")
    private LocalDate dødsdato;

    @ChangeTracked
    @Column(name = "foedselsdato", nullable = false)
    private LocalDate fødselsdato;

    @ChangeTracked
    @Convert(converter = Region.KodeverdiConverter.class)
    @Column(name="region", nullable = false)
    private Region region = Region.UDEFINERT;

    @ManyToOne(optional = false)
    @JoinColumn(name = "po_informasjon_id", nullable = false, updatable = false)
    private PersonInformasjonEntitet personopplysningInformasjon;

    PersonopplysningEntitet() {
    }

    PersonopplysningEntitet(PersonopplysningEntitet personopplysning) {
        this.aktørId = personopplysning.getAktørId();
        this.navn = personopplysning.getNavn();
        this.brukerKjønn = personopplysning.getKjønn();
        this.fødselsdato = personopplysning.getFødselsdato();
        this.dødsdato = personopplysning.getDødsdato();
        this.region = personopplysning.getRegion();
        this.sivilstand = personopplysning.getSivilstand();
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
        this.personopplysningInformasjon = personopplysningInformasjon;
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(getAktørId());
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

    public SivilstandType getSivilstand() {
        return sivilstand;
    }

    void setSivilstand(SivilstandType sivilstand) {
        this.sivilstand = sivilstand;
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

    public Region getRegion() {
        return region;
    }

    void setRegion(Region region) {
        this.region = region;
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
            Objects.equals(sivilstand, entitet.sivilstand) &&
            Objects.equals(aktørId, entitet.aktørId) &&
            Objects.equals(navn, entitet.navn) &&
            Objects.equals(fødselsdato, entitet.fødselsdato) &&
            Objects.equals(dødsdato, entitet.dødsdato) &&
            Objects.equals(region, entitet.region);
    }

    @Override
    public int hashCode() {
        return Objects.hash(brukerKjønn, sivilstand, aktørId, navn, fødselsdato, dødsdato, region);
    }

    @Override
    public String toString() {
        return "PersonopplysningEntitet{" + "id=" + id +
            ", aktørId=" + aktørId +
            ", brukerKjønn=" + brukerKjønn +
            ", sivilstand=" + sivilstand +
            ", navn='" + navn + '\'' +
            ", fødselsdato=" + fødselsdato +
            ", dødsdato=" + dødsdato +
            ", region=" + region +
            '}';
    }

    public int compareTo(PersonopplysningEntitet other) {
        return other.getAktørId().compareTo(this.getAktørId());
    }
}
