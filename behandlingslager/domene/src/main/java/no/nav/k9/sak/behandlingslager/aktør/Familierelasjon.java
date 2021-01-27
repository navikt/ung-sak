package no.nav.k9.sak.behandlingslager.aktør;

import java.time.LocalDate;

import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.sak.typer.PersonIdent;

public class Familierelasjon {
    private PersonIdent personIdent;
    private RelasjonsRolleType relasjonsrolle;
    private Boolean harSammeBosted;

    /**
     * @deprecated bruk ctor med PersonIdent
     */
    @Deprecated
    public Familierelasjon(String fnr, RelasjonsRolleType relasjonsrolle, LocalDate fødselsdato,
            String adresse, Boolean harSammeBosted) {

        this(PersonIdent.fra(fnr), relasjonsrolle, harSammeBosted);
    }

    public Familierelasjon(PersonIdent personIdent,  RelasjonsRolleType relasjonsrolle, Boolean harSammeBosted) {
        this.personIdent = personIdent;
        this.relasjonsrolle = relasjonsrolle;
        this.harSammeBosted = harSammeBosted;
    }

    /**
     * @deprecated bruk {@link #getPersonIdent()}
     */
    @Deprecated
    public String getFnr() {
        return personIdent.getIdent();
    }

    public PersonIdent getPersonIdent() {
        return personIdent;
    }

    public RelasjonsRolleType getRelasjonsrolle() {
        return relasjonsrolle;
    }

    public Boolean getHarSammeBosted() {
        return harSammeBosted;
    }

    @Override
    public String toString() {
        // tar ikke med personIdent i toString så det ikke lekkeri logger etc.
        return getClass().getSimpleName()
                + "<relasjon=" + relasjonsrolle  //$NON-NLS-1$
                + ">"; //$NON-NLS-1$
    }
}
