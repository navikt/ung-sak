package no.nav.ung.sak.behandlingslager.behandling.personopplysning;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import no.nav.ung.kodeverk.person.RelasjonsRolleType;
import no.nav.ung.sak.typer.AktørId;

public class PersonInformasjonBuilder {

    private final PersonInformasjonEntitet kladd;
    private final PersonopplysningVersjonType type;

    public PersonInformasjonBuilder(PersonopplysningVersjonType type) {
        this(new PersonInformasjonEntitet(), type);
    }

    /** for testing og inkrementell bygging (eks. overstyringer). */
    public PersonInformasjonBuilder(PersonInformasjonEntitet kladd, PersonopplysningVersjonType type) {
        this.kladd = new PersonInformasjonEntitet(Objects.requireNonNull(kladd)); // tar kopi av input
        this.type = Objects.requireNonNull(type);
    }

    public boolean harAktørId(AktørId aktørId) {
        return kladd.harAktørId(aktørId);
    }

    public PersonInformasjonBuilder leggTil(PersonopplysningBuilder builder) {
        if (!builder.getErOppdatering()) {
            kladd.leggTilPersonopplysning(builder.build());
        }
        return this;
    }

    public PersonInformasjonBuilder leggTil(RelasjonBuilder builder) {
        if (!builder.getErOppdatering()) {
            kladd.leggTilPersonrelasjon(builder.build());
        }
        return this;
    }

    public PersonInformasjonEntitet build() {
        return new PersonInformasjonEntitet(kladd);
    }

    public PersonopplysningVersjonType getType() {
        return type;
    }

    public PersonopplysningBuilder getPersonopplysningBuilder(AktørId aktørId) {
        Objects.requireNonNull(aktørId, "aktørId");
        return kladd.getPersonBuilderForAktørId(aktørId);
    }

    public RelasjonBuilder getRelasjonBuilder(AktørId fraAktør, AktørId tilAktør, RelasjonsRolleType rolle) {
        Objects.requireNonNull(fraAktør, "fraAktør");
        Objects.requireNonNull(tilAktør, "tilAktør");
        Objects.requireNonNull(rolle, "rolle");
        return kladd.getRelasjonBuilderForAktørId(fraAktør, tilAktør, rolle);
    }

    public static final class PersonopplysningBuilder {
        private final PersonopplysningEntitet kladd;
        private final boolean oppdatering;

        private PersonopplysningBuilder(PersonopplysningEntitet kladd, boolean oppdatering) {
            this.kladd = kladd;
            this.oppdatering = oppdatering;
        }

        public AktørId getAktørId() {
            return kladd.getAktørId();
        }

        private static PersonopplysningBuilder oppdatere(PersonopplysningEntitet kladd) {
            return new PersonopplysningBuilder(kladd, true);
        }

        private static PersonopplysningBuilder ny() {
            return new PersonopplysningBuilder(new PersonopplysningEntitet(), false);
        }

        static PersonopplysningBuilder oppdater(Optional<PersonopplysningEntitet> aggregat) {
            return aggregat.map(PersonopplysningBuilder::oppdatere).orElseGet(PersonopplysningBuilder::ny);
        }

        public PersonopplysningBuilder medAktørId(AktørId aktørId) {
            kladd.setAktørId(aktørId);
            return this;
        }

        public PersonopplysningBuilder medNavn(String navn) {
            kladd.setNavn(navn);
            return this;
        }

        public PersonopplysningBuilder medFødselsdato(LocalDate fødselsdato) {
            kladd.setFødselsdato(fødselsdato);
            return this;
        }

        public PersonopplysningBuilder medDødsdato(LocalDate dødsdato) {
            kladd.setDødsdato(dødsdato);
            return this;
        }

        public PersonopplysningEntitet build() {
            return kladd;
        }

        boolean getErOppdatering() {
            return oppdatering;
        }
    }

    public static final class RelasjonBuilder {

        private final PersonRelasjonEntitet kladd;
        private final boolean oppdatering;

        private RelasjonBuilder(PersonRelasjonEntitet kladd, boolean oppdatering) {
            this.kladd = kladd;
            this.oppdatering = oppdatering;
        }

        private static RelasjonBuilder ny() {
            return new RelasjonBuilder(new PersonRelasjonEntitet(), false);
        }

        private static RelasjonBuilder oppdatere(PersonRelasjonEntitet entitet) {
            return new RelasjonBuilder(entitet, true);
        }

        static RelasjonBuilder oppdater(Optional<PersonRelasjonEntitet> aggregat) {
            return aggregat.map(RelasjonBuilder::oppdatere).orElseGet(RelasjonBuilder::ny);
        }

        public RelasjonBuilder fraAktør(AktørId fraAktørId) {
            kladd.setFraAktørId(fraAktørId);
            return this;
        }

        public RelasjonBuilder tilAktør(AktørId tilAktørId) {
            kladd.setTilAktørId(tilAktørId);
            return this;
        }

        public RelasjonBuilder medRolle(RelasjonsRolleType relasjonsrolle) {
            kladd.setRelasjonsrolle(relasjonsrolle);
            return this;
        }

        public PersonRelasjonEntitet build() {
            return kladd;
        }

        boolean getErOppdatering() {
            return oppdatering;
        }
    }

}
