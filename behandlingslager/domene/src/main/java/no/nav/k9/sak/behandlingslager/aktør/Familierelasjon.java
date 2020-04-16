package no.nav.k9.sak.behandlingslager.aktør;

import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.sak.typer.PersonIdent;

import java.time.LocalDate;

public class Familierelasjon {
    private PersonIdent personIdent;
    private RelasjonsRolleType relasjonsrolle;
    private LocalDate fødselsdato;
    private String adresse;
    private Boolean harSammeBosted;

    /**
     * @deprecated bruk ctor med PersonIdent
     */
    @Deprecated
    public Familierelasjon(String fnr, RelasjonsRolleType relasjonsrolle, LocalDate fødselsdato,
            String adresse, Boolean harSammeBosted) {

        this(PersonIdent.fra(fnr), relasjonsrolle, fødselsdato, adresse, harSammeBosted);
    }

    public Familierelasjon(PersonIdent personIdent,  RelasjonsRolleType relasjonsrolle, LocalDate fødselsdato,
            String adresse, Boolean harSammeBosted) {
        this.personIdent = personIdent;
        this.relasjonsrolle = relasjonsrolle;
        this.fødselsdato = fødselsdato;
        this.adresse = adresse;
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

    public String getAdresse() {
        return adresse;
    }

    public Boolean getHarSammeBosted() {
        return harSammeBosted;
    }

    public LocalDate getFødselsdato(){return fødselsdato;}

    @Override
    public String toString() {
        // tar ikke med personIdent i toString så det ikke lekkeri logger etc.
        return getClass().getSimpleName()
                + "<relasjon=" + relasjonsrolle  //$NON-NLS-1$
                + ", fødselsdato=" + fødselsdato //$NON-NLS-1$
                + ">"; //$NON-NLS-1$
    }
}
