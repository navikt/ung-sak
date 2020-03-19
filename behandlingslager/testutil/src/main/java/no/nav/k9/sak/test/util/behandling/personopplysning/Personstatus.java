package no.nav.k9.sak.test.util.behandling.personopplysning;

import java.time.LocalDate;

import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.sak.typer.AktørId;

public final class Personstatus {

    private AktørId aktørId;
    private DatoIntervallEntitet periode;
    private PersonstatusType personstatus = PersonstatusType.UDEFINERT;

    public AktørId getAktørId() {
        return aktørId;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public PersonstatusType getPersonstatus() {
        return personstatus;
    }

    private Personstatus(Builder builder) {
        this.aktørId = builder.aktørId;
        this.periode = builder.periode;
        this.personstatus = builder.personstatus;
    }

    public static Builder builder() {
        return new Builder();
    }


    public static final class Builder {
        private AktørId aktørId;
        private DatoIntervallEntitet periode;
        private PersonstatusType personstatus;

        private Builder() {
        }

        public Personstatus build() {
            return new Personstatus(this);
        }

        public Builder aktørId(AktørId aktørId) {
            this.aktørId = aktørId;
            return this;
        }

        public Builder periode(LocalDate fom, LocalDate tom) {
            this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
            return this;
        }

        public Builder personstatus(PersonstatusType personstatus) {
            this.personstatus = personstatus;
            return this;
        }
    }
}
