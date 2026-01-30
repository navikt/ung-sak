package no.nav.ung.sak.behandlingslager.aktør;

import no.nav.ung.kodeverk.person.RelasjonsRolleType;
import no.nav.ung.sak.typer.PersonIdent;

public class Familierelasjon {
    private final PersonIdent personIdent;
    private final RelasjonsRolleType relasjonsrolle;
    private final RelasjonsRolleType minRelasjonsRolle;

    public Familierelasjon(PersonIdent personIdent, RelasjonsRolleType relasjonsrolle, RelasjonsRolleType minRelasjonsRolle) {
        this.personIdent = personIdent;
        this.relasjonsrolle = relasjonsrolle;
        this.minRelasjonsRolle = minRelasjonsRolle;
    }

    public PersonIdent getPersonIdent() {
        return personIdent;
    }

    public RelasjonsRolleType getRelasjonsrolle() {
        return relasjonsrolle;
    }

    public RelasjonsRolleType getMinRelasjonsRolle() {
        return minRelasjonsRolle;
    }

    @Override
    public String toString() {
        // tar ikke med personIdent i toString så det ikke lekkeri logger etc.
        return getClass().getSimpleName()
            + "<relasjon=" + relasjonsrolle //$NON-NLS-1$
            + ", minRelasjonsRolle=" + minRelasjonsRolle //$NON-NLS-1$
            + ">"; //$NON-NLS-1$
    }
}
