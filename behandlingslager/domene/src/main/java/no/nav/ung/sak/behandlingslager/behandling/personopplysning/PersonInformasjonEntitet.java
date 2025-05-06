package no.nav.ung.sak.behandlingslager.behandling.personopplysning;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.person.RelasjonsRolleType;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import no.nav.ung.sak.typer.AktørId;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.*;

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
    private List<PersonopplysningEntitet> personopplysninger = new ArrayList<>();

    @ChangeTracked
    @OneToMany(mappedBy = REF_NAME)
    private List<PersonRelasjonEntitet> relasjoner = new ArrayList<>();

    PersonInformasjonEntitet() {
    }

    PersonInformasjonEntitet(PersonInformasjonEntitet aggregat) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PersonInformasjonEntitet that = (PersonInformasjonEntitet) o;
        return Objects.equals(personopplysninger, that.personopplysninger) &&
            Objects.equals(relasjoner, that.relasjoner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(personopplysninger, relasjoner);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PersonInformasjonEntitet{");
        sb.append("id=").append(id);
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
}
