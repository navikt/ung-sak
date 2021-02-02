package no.nav.k9.sak.behandlingslager.aktør;

import java.time.LocalDate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.geografisk.AdresseType;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.sak.typer.PersonIdent;

public class Familierelasjon {
    private PersonIdent personIdent;
    private RelasjonsRolleType relasjonsrolle;
    private Boolean harSammeBostedTps;

    private static final Logger LOG = LoggerFactory.getLogger(Familierelasjon.class);

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
        this.harSammeBostedTps = harSammeBosted;
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

    public Boolean getHarSammeBosted(Personinfo fra, Personinfo til) {
        var harSammeBostedPdl = utledSammeBosted(fra, til);
        if (harSammeBostedPdl == harSammeBostedTps) {
            LOG.info("K9SAK PDL sammeBosted match tps {} pdl {}", harSammeBostedTps, harSammeBostedPdl);
        } else {
            LOG.info("K9SAK PDL sammeBosted mismatch tps {} pdl {}", harSammeBostedTps, harSammeBostedPdl);
        }

        return harSammeBostedTps;
    }

    private boolean utledSammeBosted(Personinfo fra, Personinfo til) {
        // FIXME: Erstatt getAdresseInfoListPdl() ned getAdresseInfoList()  når TPS utfases
        var tilAdresser = til.getAdresseInfoListPdl().stream()
            .filter(ad -> AdresseType.BOSTEDSADRESSE.equals(ad.getGjeldendePostadresseType()))
            .collect(Collectors.toList());
        return fra.getAdresseInfoListPdl().stream()
            .filter(a -> AdresseType.BOSTEDSADRESSE.equals(a.getGjeldendePostadresseType()))
            .anyMatch(adr1 -> tilAdresser.stream().anyMatch(adr2 -> Adresseinfo.likeAdresser(adr1, adr2)));
    }

    @Override
    public String toString() {
        // tar ikke med personIdent i toString så det ikke lekkeri logger etc.
        return getClass().getSimpleName()
                + "<relasjon=" + relasjonsrolle  //$NON-NLS-1$
                + ">"; //$NON-NLS-1$
    }
}
