package no.nav.foreldrepenger.domene.personopplysning;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class PersonopplysningAksjonspunkt {

    private PersonstatusPeriode personstatusPeriode;
    private String kjønnKode;
    private String statsborgerskapKode;

    public PersonopplysningAksjonspunkt(PersonstatusPeriode personstatusPeriode) {
        this.personstatusPeriode = personstatusPeriode;
    }

    public PersonopplysningAksjonspunkt(PersonstatusPeriode personstatusPeriode, String kjønnKode, String statsborgerskapKode) {
        this.personstatusPeriode = personstatusPeriode;
        this.kjønnKode = kjønnKode;
        this.statsborgerskapKode = statsborgerskapKode;
    }

    public Optional<PersonstatusPeriode> getPersonstatusTypeKode() {
        return Optional.ofNullable(personstatusPeriode);
    }

    public Optional<String> getKjønnKode() {
        return Optional.ofNullable(kjønnKode);
    }

    public Optional<String> getStatsborgerskapKode() {
        return Optional.ofNullable(statsborgerskapKode);
    }

    public static final class PersonstatusPeriode {

        private PersonstatusType personstatus;
        private LocalDate gyldigFom;
        private LocalDate gyldigTom;

        public PersonstatusPeriode(PersonstatusType personstatus, LocalDate gyldigFom, LocalDate gyldigTom) {
            this.personstatus = personstatus;
            this.gyldigFom = gyldigFom;
            this.gyldigTom = gyldigTom;
        }

        public PersonstatusPeriode(PersonstatusType personstatus, DatoIntervallEntitet periode) {
            this.personstatus = personstatus;
            this.gyldigFom = periode.getFomDato();
            this.gyldigTom = periode.getTomDato();
        }

        public PersonstatusType getPersonstatus() {
            return personstatus;
        }

        public LocalDate getGyldigFom() {
            return gyldigFom;
        }

        public LocalDate getGyldigTom() {
            return gyldigTom;
        }
    }
}
