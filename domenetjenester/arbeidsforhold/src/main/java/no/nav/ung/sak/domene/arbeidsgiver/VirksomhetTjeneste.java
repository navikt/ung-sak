package no.nav.ung.sak.domene.arbeidsgiver;

import static no.nav.ung.StringTrimmer.trim;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.organisasjon.OrganisasjonRestKlient;
import no.nav.k9.felles.integrasjon.organisasjon.OrganisasjonstypeEReg;
import no.nav.k9.felles.util.LRUCache;
import no.nav.ung.kodeverk.organisasjon.Organisasjonstype;
import no.nav.ung.sak.behandlingslager.virksomhet.Virksomhet;
import no.nav.ung.sak.typer.OrgNummer;
import no.nav.ung.sak.typer.OrganisasjonsNummerValidator;

@ApplicationScoped
public class VirksomhetTjeneste {

    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(24, TimeUnit.HOURS);

    private static final Virksomhet KUNSTIG_VIRKSOMHET = new Virksomhet.Builder()
        .medNavn("Kunstig virksomhet")
        .medOrganisasjonstype(Organisasjonstype.KUNSTIG)
        .medOrgnr(OrgNummer.KUNSTIG_ORG)
        .medRegistrert(LocalDate.of(1978, 01, 01))
        .medOppstart(LocalDate.of(1978, 01, 01))
        .build();

    private LRUCache<String, Virksomhet> cache = new LRUCache<>(2000, CACHE_ELEMENT_LIVE_TIME_MS);

    private OrganisasjonRestKlient eregRestKlient;

    public VirksomhetTjeneste() {
        // CDI
    }

    @Inject
    public VirksomhetTjeneste(OrganisasjonRestKlient eregRestKlient) {
        this.eregRestKlient = eregRestKlient;
    }

    /**
     * Henter informasjon fra Enhetsregisteret hvis applikasjonen ikke kjenner til orgnr eller har data som er eldre enn 24 timer.
     *
     * @param orgNummer orgnummeret
     * @return relevant informasjon om virksomheten.
     * @throws IllegalArgumentException ved forespørsel om orgnr som ikke finnes i enhetsreg
     */
    public Virksomhet hentOrganisasjon(String orgNummer) {
        return hent(orgNummer);
    }

    public Optional<Virksomhet> finnOrganisasjon(String orgNummer) {
        if (orgNummer == null)
            return Optional.empty();
        if (OrgNummer.erKunstig(orgNummer)) {
            return Optional.of(hent(orgNummer));
        }
        return OrganisasjonsNummerValidator.erGyldig(orgNummer) ? Optional.of(hent(orgNummer)) : Optional.empty();
    }

    private Virksomhet hent(String orgnr) {
        if (Organisasjonstype.erKunstig(orgnr)) {
            return KUNSTIG_VIRKSOMHET;
        }
        var virksomhet = Optional.ofNullable(cache.get(orgnr)).orElseGet(() -> hentOrganisasjonRest(orgnr));
        cache.put(orgnr, virksomhet);
        return virksomhet;
    }

    private Virksomhet hentOrganisasjonRest(String orgNummer) {
        Objects.requireNonNull(orgNummer, "orgNummer"); // NOSONAR
        var org = eregRestKlient.hentOrganisasjon(orgNummer);
        var builder = Virksomhet.getBuilder()
            .medNavn(trim(org.getNavn()))
            .medRegistrert(org.getRegistreringsdato())
            .medOrgnr(org.getOrganisasjonsnummer());
        if (OrganisasjonstypeEReg.VIRKSOMHET.equals(org.getType())) {
            builder.medOrganisasjonstype(Organisasjonstype.VIRKSOMHET)
                .medOppstart(org.getOppstartsdato())
                .medAvsluttet(org.getNedleggelsesdato() != null ? org.getNedleggelsesdato() : org.getOpphørsdato());
        } else if (OrganisasjonstypeEReg.JURIDISK_ENHET.equals(org.getType())) {
            builder.medOrganisasjonstype(Organisasjonstype.JURIDISK_ENHET);
        } else if (OrganisasjonstypeEReg.ORGLEDD.equals(org.getType())) {
            builder.medOrganisasjonstype(Organisasjonstype.ORGLEDD);
        }
        return builder.build();
    }

}
