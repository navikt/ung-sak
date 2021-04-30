package no.nav.k9.sak.behandlingslager.behandling.personopplysning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.k9.kodeverk.geografisk.AdresseType;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;

@Entity(name = "PersonInformasjon")
@Table(name = "PO_INFORMASJON")
@DynamicInsert
@DynamicUpdate
public class PersonInformasjonEntitet extends BaseEntitet {

    private static final String REF_NAME = "personopplysningInformasjon";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PO_INFORMASJON")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @ChangeTracked
    @OneToMany(mappedBy = REF_NAME)
    private List<PersonstatusEntitet> personstatuser = new ArrayList<>();

    @ChangeTracked
    @OneToMany(mappedBy = REF_NAME)
    private List<StatsborgerskapEntitet> statsborgerskap = new ArrayList<>();

    @ChangeTracked
    @OneToMany(mappedBy = REF_NAME)
    private List<PersonAdresseEntitet> adresser = new ArrayList<>();

    @ChangeTracked
    @OneToMany(mappedBy = REF_NAME)
    private List<PersonopplysningEntitet> personopplysninger = new ArrayList<>();

    @ChangeTracked
    @OneToMany(mappedBy = REF_NAME)
    private List<PersonRelasjonEntitet> relasjoner = new ArrayList<>();

    PersonInformasjonEntitet() {
    }

    PersonInformasjonEntitet(PersonInformasjonEntitet aggregat) {
        if (Optional.ofNullable(aggregat.getAdresser()).isPresent()) {
            aggregat.getAdresser()
                .forEach(e -> {
                    PersonAdresseEntitet entitet = new PersonAdresseEntitet(e);
                    adresser.add(entitet);
                    entitet.setPersonopplysningInformasjon(this);
                });
        }
        if (Optional.ofNullable(aggregat.getPersonstatus()).isPresent()) {
            aggregat.getPersonstatus()
                .forEach(e -> {
                    PersonstatusEntitet entitet = new PersonstatusEntitet(e);
                    personstatuser.add(entitet);
                    entitet.setPersonInformasjon(this);
                });
        }
        if (Optional.ofNullable(aggregat.getStatsborgerskap()).isPresent()) {
            aggregat.getStatsborgerskap()
                .forEach(e -> {
                    StatsborgerskapEntitet entitet = new StatsborgerskapEntitet(e);
                    statsborgerskap.add(entitet);
                    entitet.setPersonopplysningInformasjon(this);
                });
        }
        if (Optional.ofNullable(aggregat.getRelasjoner()).isPresent()) {
            aggregat.getRelasjoner()
                .forEach(e -> {
                    PersonRelasjonEntitet entitet = new PersonRelasjonEntitet(e);
                    relasjoner.add(entitet);
                    entitet.setPersonopplysningInformasjon(this);
                });
        }
        if (Optional.ofNullable(aggregat.getPersonopplysninger()).isPresent()) {
            aggregat.getPersonopplysninger()
                .forEach(e -> {
                    PersonopplysningEntitet entitet = new PersonopplysningEntitet(e);
                    personopplysninger.add(entitet);
                    entitet.setPersonopplysningInformasjon(this);
                });
        }
    }

    void leggTilAdresse(PersonAdresseEntitet adresse) {
        final PersonAdresseEntitet adresse1 = adresse;
        adresse1.setPersonopplysningInformasjon(this);
        adresser.add(adresse1);
    }

    void leggTilStatsborgerskap(StatsborgerskapEntitet statsborgerskap) {
        final StatsborgerskapEntitet statsborgerskap1 = statsborgerskap;
        statsborgerskap1.setPersonopplysningInformasjon(this);
        this.statsborgerskap.add(statsborgerskap1);
    }

    void leggTilPersonstatus(PersonstatusEntitet personstatus) {
        final PersonstatusEntitet personstatus1 = personstatus;
        personstatus1.setPersonInformasjon(this);
        this.personstatuser.add(personstatus1);
    }

    void leggTilPersonrelasjon(PersonRelasjonEntitet relasjon) {
        final PersonRelasjonEntitet relasjon1 = relasjon;
        relasjon1.setPersonopplysningInformasjon(this);
        this.relasjoner.add(relasjon1);
    }

    void leggTilPersonopplysning(PersonopplysningEntitet personopplysning) {
        var aktørId = personopplysning.getAktørId();
        if (harAktørId(aktørId)) {
            throw new IllegalStateException("Kan ikke overskrive aktørId:" + aktørId);
        }

        personopplysning.setPersonopplysningInformasjon(this);
        this.personopplysninger.add(personopplysning);
    }

    void fjernPersonopplysning(AktørId aktørId) {
        this.personopplysninger.removeIf(e -> e.getAktørId().equals(aktørId));
    }

    /**
     * Rydder bort alt unntatt personopplysninger
     *
     * @deprecated bør håndteres i ctor
     */
    @Deprecated(forRemoval = true)
    void tilbakestill() {
        this.adresser.clear();
        this.personstatuser.clear();
        this.relasjoner.clear();
        this.statsborgerskap.clear();
    }

    PersonInformasjonBuilder.PersonopplysningBuilder getPersonBuilderForAktørId(AktørId aktørId) {
        Objects.requireNonNull(aktørId, "aktørId");
        final Optional<PersonopplysningEntitet> eksisterendeAktør = personopplysninger.stream().filter(it -> it.getAktørId().equals(aktørId)).findFirst();
        return PersonInformasjonBuilder.PersonopplysningBuilder.oppdater(eksisterendeAktør).medAktørId(aktørId);
    }

    boolean harAktørId(AktørId aktørId) {
        Objects.requireNonNull(aktørId, "aktørId");
        return personopplysninger.stream().anyMatch(it -> it.getAktørId().equals(aktørId));
    }

    /**
     * Relasjoner mellom to aktører
     *
     * @return entitet
     */
    public List<PersonRelasjonEntitet> getRelasjoner() {
        return Collections.unmodifiableList(relasjoner);
    }

    /**
     * Alle relevante aktørers personopplysninger
     *
     * @return entitet
     */
    public List<PersonopplysningEntitet> getPersonopplysninger() {
        return Collections.unmodifiableList(personopplysninger);
    }

    public PersonopplysningEntitet getPersonopplysning(AktørId aktørId) {
        return getPersonopplysninger().stream().filter(p -> p.getAktørId().equals(aktørId)).findFirst().orElseThrow(() -> new IllegalStateException("Mangler personopplysninger for forespurt aktør"));
    }

    /**
     * Alle relevante aktørers personstatuser med gyldighetstidspunkt (fom, tom)
     * <p>
     * Det er kun hentet inn historikk for søker, de andre aktørene ligger inne med perioden fødselsdato -> dødsdato/tidenes ende
     *
     * @return entitet
     */
    public List<PersonstatusEntitet> getPersonstatus() {
        return Collections.unmodifiableList(personstatuser);
    }

    /**
     * Alle relevante aktørers statsborgerskap med gyldighetstidspunkt (fom, tom)
     * <p>
     * Det er kun hentet inn historikk for søker, de andre aktørene ligger inne med perioden fødselsdato -> dødsdato/tidenes ende
     *
     * @return entitet
     */
    public List<StatsborgerskapEntitet> getStatsborgerskap() {
        return Collections.unmodifiableList(statsborgerskap);
    }

    /**
     * Alle relevante aktørers adresser med gyldighetstidspunkt (fom, tom)
     * <p>
     * Det er kun hentet inn historikk for søker, de andre aktørene ligger inne med perioden fødselsdato -> dødsdato/tidenes ende
     *
     * @return entitet
     */
    public List<PersonAdresseEntitet> getAdresser() {
        return Collections.unmodifiableList(adresser);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PersonInformasjonEntitet that = (PersonInformasjonEntitet) o;
        return Objects.equals(personstatuser, that.personstatuser) &&
            Objects.equals(statsborgerskap, that.statsborgerskap) &&
            Objects.equals(adresser, that.adresser) &&
            Objects.equals(personopplysninger, that.personopplysninger) &&
            Objects.equals(relasjoner, that.relasjoner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(personstatuser, statsborgerskap, adresser, personopplysninger, relasjoner);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PersonInformasjonEntitet{");
        sb.append("id=").append(id);
        sb.append(", personstatuser=").append(personstatuser);
        sb.append(", statsborgerskap=").append(statsborgerskap);
        sb.append(", adresser=").append(adresser);
        sb.append(", personopplysninger=").append(personopplysninger);
        sb.append(", relasjoner=").append(relasjoner);
        sb.append('}');
        return sb.toString();
    }

    PersonInformasjonBuilder.RelasjonBuilder getRelasjonBuilderForAktørId(AktørId fraAktør, AktørId tilAktør, RelasjonsRolleType rolle) {
        Objects.requireNonNull(fraAktør);
        Objects.requireNonNull(tilAktør);
        final Optional<PersonRelasjonEntitet> eksisterende = relasjoner.stream()
            .filter(it -> it.getAktørId().equals(fraAktør) && it.getTilAktørId().equals(tilAktør) && it.getRelasjonsrolle().equals(rolle))
            .findAny();
        return PersonInformasjonBuilder.RelasjonBuilder.oppdater(eksisterende).fraAktør(fraAktør).tilAktør(tilAktør).medRolle(rolle);
    }

    PersonInformasjonBuilder.AdresseBuilder getAdresseBuilderForAktørId(AktørId aktørId, AdresseType type, DatoIntervallEntitet periode) {
        Objects.requireNonNull(aktørId);
        Objects.requireNonNull(type);
        final Optional<PersonAdresseEntitet> eksisterende = adresser.stream()
            .filter(it -> it.getAktørId().equals(aktørId) && it.getAdresseType().equals(type) && erSannsynligvisSammePeriode(it.getPeriode(), periode))
            .findAny();
        return PersonInformasjonBuilder.AdresseBuilder.oppdater(eksisterende).medAktørId(aktørId).medAdresseType(type).medPeriode(periode);
    }

    private boolean erSannsynligvisSammePeriode(DatoIntervallEntitet eksiterendePeriode, DatoIntervallEntitet nyPeriode) {
        return eksiterendePeriode.equals(nyPeriode) || eksiterendePeriode.getFomDato().equals(nyPeriode.getFomDato())
            && eksiterendePeriode.getTomDato().equals(Tid.TIDENES_ENDE) && !nyPeriode.getTomDato().equals(Tid.TIDENES_ENDE);
    }

    PersonInformasjonBuilder.StatsborgerskapBuilder getStatsborgerskapBuilderForAktørId(AktørId aktørId, Landkoder landkode, DatoIntervallEntitet periode, Region region) {
        Objects.requireNonNull(aktørId);
        final Optional<StatsborgerskapEntitet> eksisterende = statsborgerskap.stream()
            .filter(it -> it.getAktørId().equals(aktørId) && it.getStatsborgerskap().equals(landkode) && erSannsynligvisSammePeriode(it.getPeriode(), periode))
            .findAny();
        return PersonInformasjonBuilder.StatsborgerskapBuilder.oppdater(eksisterende).medAktørId(aktørId).medStatsborgerskap(landkode).medPeriode(periode).medRegion(region);
    }

    PersonInformasjonBuilder.PersonstatusBuilder getPersonstatusBuilderForAktørId(AktørId aktørId, DatoIntervallEntitet periode) {
        Objects.requireNonNull(aktørId);
        final Optional<PersonstatusEntitet> eksisterende = personstatuser.stream()
            .filter(it -> it.getAktørId().equals(aktørId) && erSannsynligvisSammePeriode(it.getPeriode(), periode))
            .findAny();
        return PersonInformasjonBuilder.PersonstatusBuilder.oppdater(eksisterende).medAktørId(aktørId).medPeriode(periode);
    }
}
