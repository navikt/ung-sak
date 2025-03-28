package no.nav.ung.sak.domene.arbeidsgiver;

import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.felles.exception.VLException;
import no.nav.k9.felles.util.LRUCache;
import no.nav.ung.kodeverk.organisasjon.Organisasjonstype;
import no.nav.ung.sak.behandlingslager.aktør.PersoninfoArbeidsgiver;
import no.nav.ung.sak.behandlingslager.virksomhet.Virksomhet;
import no.nav.ung.sak.domene.arbeidsforhold.person.PersonIdentTjeneste;
import no.nav.ung.sak.typer.Arbeidsgiver;
import no.nav.ung.sak.typer.OrgNummer;

@ApplicationScoped
public class ArbeidsgiverTjeneste {

    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(12, TimeUnit.HOURS);
    private static final long SHORT_CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);
    private PersonIdentTjeneste tpsTjeneste;
    private LRUCache<String, ArbeidsgiverOpplysninger> cache = new LRUCache<>(1000, CACHE_ELEMENT_LIVE_TIME_MS);
    private LRUCache<String, ArbeidsgiverOpplysninger> failBackoffCache = new LRUCache<>(100, SHORT_CACHE_ELEMENT_LIVE_TIME_MS);
    private VirksomhetTjeneste virksomhetTjeneste;

    ArbeidsgiverTjeneste() {
        // CDI
    }

    @Inject
    public ArbeidsgiverTjeneste(PersonIdentTjeneste tpsTjeneste, VirksomhetTjeneste virksomhetTjeneste) {
        this.tpsTjeneste = tpsTjeneste;
        this.virksomhetTjeneste = virksomhetTjeneste;
    }

    public ArbeidsgiverOpplysninger hent(Arbeidsgiver arbeidsgiver) {
        if (arbeidsgiver == null) {
            return null;
        }
        ArbeidsgiverOpplysninger arbeidsgiverOpplysninger = cache.get(arbeidsgiver.getIdentifikator());
        if (arbeidsgiverOpplysninger != null) {
            return arbeidsgiverOpplysninger;
        }
        arbeidsgiverOpplysninger = failBackoffCache.get(arbeidsgiver.getIdentifikator());
        if (arbeidsgiverOpplysninger != null) {
            return arbeidsgiverOpplysninger;
        }
        if (arbeidsgiver.getErVirksomhet() && !Organisasjonstype.erKunstig(arbeidsgiver.getOrgnr())) {
            String orgnr = arbeidsgiver.getOrgnr();
            var virksomhet = virksomhetTjeneste.hentOrganisasjon(orgnr);
            ArbeidsgiverOpplysninger nyOpplysninger = new ArbeidsgiverOpplysninger(orgnr, virksomhet.getNavn());
            cache.put(arbeidsgiver.getIdentifikator(), nyOpplysninger);
            return nyOpplysninger;
        } else if (arbeidsgiver.getErVirksomhet() && Organisasjonstype.erKunstig(arbeidsgiver.getOrgnr())) {
            return new ArbeidsgiverOpplysninger(OrgNummer.KUNSTIG_ORG, "Kunstig(Lagt til av saksbehandling)");
        } else if (arbeidsgiver.erAktørId()) {
            Optional<PersoninfoArbeidsgiver> personinfo = hentInformasjonFraTps(arbeidsgiver);
            if (personinfo.isPresent()) {
                PersoninfoArbeidsgiver personinfoArbeidsgiver = personinfo.get();
                String fødselsdato = personinfoArbeidsgiver.getFødselsdato().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                ArbeidsgiverOpplysninger nyOpplysninger = new ArbeidsgiverOpplysninger(fødselsdato, personinfoArbeidsgiver.getPersonIdent().getIdent(), personinfoArbeidsgiver.getNavn(), personinfoArbeidsgiver.getFødselsdato());
                cache.put(arbeidsgiver.getIdentifikator(), nyOpplysninger);
                return nyOpplysninger;
            } else {
                // Putter bevist ikke denne i cache da denne aktøren ikke er kjent, men legger denne i en backoff cache som benyttes for at vi ikke skal hamre på tps ved sikkerhetsbegrensning
                ArbeidsgiverOpplysninger opplysninger = new ArbeidsgiverOpplysninger(arbeidsgiver.getIdentifikator(), "N/A");
                failBackoffCache.put(arbeidsgiver.getIdentifikator(), opplysninger);
                return opplysninger;
            }
        }
        return null;
    }

    public Virksomhet hentVirksomhet(String orgNummer) {
        return virksomhetTjeneste.finnOrganisasjon(orgNummer).orElseThrow(() -> new IllegalArgumentException("Kunne ikke hente virksomhet for orgNummer: " + orgNummer));
    }

    public static Arbeidsgiver fra(Virksomhet virksomhet) {
        return Arbeidsgiver.virksomhet(virksomhet.getOrgnr());
    }

    private Optional<PersoninfoArbeidsgiver> hentInformasjonFraTps(Arbeidsgiver arbeidsgiver) {
        try {
            return tpsTjeneste.hentPersoninfoArbeidsgiver(arbeidsgiver.getAktørId());
        } catch (VLException feil) {
            // Ønsker ikke å gi GUI problemer ved å eksponere exceptions
            return Optional.empty();
        }
    }
}
