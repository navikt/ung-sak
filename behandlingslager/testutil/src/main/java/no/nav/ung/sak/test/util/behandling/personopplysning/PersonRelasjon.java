package no.nav.ung.sak.test.util.behandling.personopplysning;

import no.nav.ung.kodeverk.person.RelasjonsRolleType;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.HarAktørId;
import no.nav.ung.sak.typer.AktørId;

public final class PersonRelasjon implements HarAktørId {

    private AktørId fraAktørId;
    private AktørId tilAktørId;
    private RelasjonsRolleType relasjonsrolle;

    @Override
    public AktørId getAktørId() {
        return fraAktørId;
    }

    public AktørId getTilAktørId() {
        return tilAktørId;
    }

    public RelasjonsRolleType getRelasjonsrolle() {
        return relasjonsrolle;
    }

    private PersonRelasjon(Builder builder) {
        this.fraAktørId = builder.fraAktørId;
        this.tilAktørId = builder.tilAktørId;
        this.relasjonsrolle = builder.relasjonsrolle;
    }

    public static Builder builder() {
        return new Builder();
    }


    public static final class Builder {
        private AktørId fraAktørId;
        private AktørId tilAktørId;
        private RelasjonsRolleType relasjonsrolle;
        private Boolean harSammeBosted;

        private Builder() {
        }

        public PersonRelasjon build() {
            return new PersonRelasjon(this);
        }

        public Builder fraAktørId(AktørId fraAktørId) {
            this.fraAktørId = fraAktørId;
            return this;
        }

        public Builder tilAktørId(AktørId tilAktørId) {
            this.tilAktørId = tilAktørId;
            return this;
        }

        public Builder relasjonsrolle(RelasjonsRolleType relasjonsrolle) {
            this.relasjonsrolle = relasjonsrolle;
            return this;
        }

        public Builder harSammeBosted(Boolean harSammeBosted) {
            this.harSammeBosted = harSammeBosted;
            return this;
        }
    }
}
