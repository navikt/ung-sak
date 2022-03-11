package no.nav.k9.sak.behandlingslager.aktør;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.k9.kodeverk.geografisk.AdresseType;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.PersonIdent;

public class Familierelasjon {
    private final PersonIdent personIdent;
    private final RelasjonsRolleType relasjonsrolle;

    public Familierelasjon(PersonIdent personIdent, RelasjonsRolleType relasjonsrolle) {
        this.personIdent = personIdent;
        this.relasjonsrolle = relasjonsrolle;
    }

    public PersonIdent getPersonIdent() {
        return personIdent;
    }

    public RelasjonsRolleType getRelasjonsrolle() {
        return relasjonsrolle;
    }

    public Boolean getHarSammeBosted(Personinfo fra, Personinfo til) {
        return utledSammeBosted(fra, til);
    }

    public List<Periode> getPerioderMedDeltBosted(Personinfo fra, Personinfo til) {
        return utledDeltBostedPerioder(fra, til);
    }

    private List<Periode> utledDeltBostedPerioder(Personinfo fra, Personinfo til) {
        var fraAdresser = fra.getAdresseInfoList().stream()
            .filter(ad -> AdresseType.BOSTEDSADRESSE.equals(ad.getGjeldendePostadresseType()))
            .collect(Collectors.toList());

        return til.getDeltBostedList().stream()
            .filter(adr1 -> fraAdresser.stream().anyMatch(adr2 -> Adresseinfo.likeAdresser(adr1.getAdresseinfo(), adr2)))
            .map(DeltBosted::getPeriode)
            .collect(Collectors.toList());
    }

    private boolean utledSammeBosted(Personinfo fra, Personinfo til) {
        var tilAdresser = til.getAdresseInfoList().stream()
            .filter(ad -> AdresseType.BOSTEDSADRESSE.equals(ad.getGjeldendePostadresseType()))
            .collect(Collectors.toList());
        return fra.getAdresseInfoList().stream()
            .filter(a -> AdresseType.BOSTEDSADRESSE.equals(a.getGjeldendePostadresseType()))
            .anyMatch(adr1 -> tilAdresser.stream().anyMatch(adr2 -> Adresseinfo.likeAdresser(adr1, adr2)));
    }

    @Override
    public String toString() {
        // tar ikke med personIdent i toString så det ikke lekkeri logger etc.
        return getClass().getSimpleName()
            + "<relasjon=" + relasjonsrolle //$NON-NLS-1$
            + ">"; //$NON-NLS-1$
    }
}
