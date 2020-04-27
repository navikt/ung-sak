package no.nav.k9.sak.produksjonsstyring.arbeidsfordeling;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.binding.FinnAlleBehandlendeEnheterListeUgyldigInput;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.binding.FinnBehandlendeEnhetListeUgyldigInput;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.ArbeidsfordelingKriterier;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Diskresjonskoder;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Geografi;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Oppgavetyper;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Organisasjonsenhet;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Tema;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.meldinger.FinnAlleBehandlendeEnheterListeRequest;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.meldinger.FinnAlleBehandlendeEnheterListeResponse;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.meldinger.FinnBehandlendeEnhetListeRequest;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.meldinger.FinnBehandlendeEnhetListeResponse;
import no.nav.vedtak.felles.integrasjon.arbeidsfordeling.klient.ArbeidsfordelingConsumer;

@Dependent
public class ArbeidsfordelingTjeneste {

    private static final OrganisasjonsEnhet KLAGE_ENHET = new OrganisasjonsEnhet("4292", "NAV Klageinstans Midt-Norge", "AKTIV");

    private static final Logger logger = LoggerFactory.getLogger(ArbeidsfordelingTjeneste.class);

    private static final Map<FagsakYtelseType, String> TEMAER = Map.of(
        FagsakYtelseType.OMSORGSPENGER, "OMS", // Alt på gammelt Infotrygd Tema 'OMS'
        FagsakYtelseType.PLEIEPENGER_SYKT_BARN, "OMS", // Alt på gammelt Infotrygd Tema 'OMS'
        FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE, "OMS", // Alt på gammelt Infotrygd Tema 'OMS'
        FagsakYtelseType.OPPLÆRINGSPENGER, "OMS", // Alt på gammelt Infotrygd Tema 'OMS'
        FagsakYtelseType.FRISINN, "FRI" // Ny korona ytelse
    );

    private static final String OPPGAVETYPE = "BEH_SED"; // BEH_SED = behandle sak

    private ArbeidsfordelingConsumer consumer;

    @Inject
    public ArbeidsfordelingTjeneste(ArbeidsfordelingConsumer consumer) {
        this.consumer = consumer;
    }

    public OrganisasjonsEnhet finnBehandlendeEnhet(String geografiskTilknytning, String diskresjonskode, FagsakYtelseType ytelseType) {
        FinnBehandlendeEnhetListeRequest request = lagRequestForHentBehandlendeEnhet(ytelseType, diskresjonskode, geografiskTilknytning);

        try {
            FinnBehandlendeEnhetListeResponse response = consumer.finnBehandlendeEnhetListe(request);
            Organisasjonsenhet valgtEnhet = validerOgVelgBehandlendeEnhet(geografiskTilknytning, diskresjonskode, ytelseType, response);
            return new OrganisasjonsEnhet(valgtEnhet.getEnhetId(), valgtEnhet.getEnhetNavn());
        } catch (FinnBehandlendeEnhetListeUgyldigInput e) {
            throw ArbeidsfordelingFeil.FACTORY.finnBehandlendeEnhetListeUgyldigInput(e).toException();
        }
    }

    public List<OrganisasjonsEnhet> finnAlleBehandlendeEnhetListe(FagsakYtelseType ytelseType) {
        // NORG2 og ruting diskriminerer på TEMA, for tiden ikke på BehandlingTEMA
        FinnAlleBehandlendeEnheterListeRequest request = lagRequestForHentAlleBehandlendeEnheter(ytelseType, Optional.empty());

        try {
            FinnAlleBehandlendeEnheterListeResponse response = consumer.finnAlleBehandlendeEnheterListe(request);
            return tilOrganisasjonsEnhetListe(response, ytelseType, true);
        } catch (FinnAlleBehandlendeEnheterListeUgyldigInput e) {
            throw ArbeidsfordelingFeil.FACTORY.finnAlleBehandlendeEnheterListeUgyldigInput(e).toException();
        }
    }

    private FinnAlleBehandlendeEnheterListeRequest lagRequestForHentAlleBehandlendeEnheter(FagsakYtelseType ytelseType, Optional<String> diskresjonskode) {
        FinnAlleBehandlendeEnheterListeRequest request = new FinnAlleBehandlendeEnheterListeRequest();
        ArbeidsfordelingKriterier kriterier = new ArbeidsfordelingKriterier();

        diskresjonskode.ifPresent(kode -> {
            Diskresjonskoder diskresjonskoder = new Diskresjonskoder();
            diskresjonskoder.setValue(kode);
            kriterier.setDiskresjonskode(diskresjonskoder);
        });

        Tema tema = new Tema();
        tema.setValue(Objects.requireNonNull(TEMAER.get(ytelseType), "Har ikke mapping for ytelseType=" + ytelseType));
        kriterier.setTema(tema);

        Oppgavetyper oppgavetyper = new Oppgavetyper();
        oppgavetyper.setValue(OPPGAVETYPE);
        kriterier.setOppgavetype(oppgavetyper);

        request.setArbeidsfordelingKriterier(kriterier);
        return request;
    }

    private FinnBehandlendeEnhetListeRequest lagRequestForHentBehandlendeEnhet(FagsakYtelseType ytelseType, String diskresjonskode, String geografiskTilknytning) {
        FinnBehandlendeEnhetListeRequest request = new FinnBehandlendeEnhetListeRequest();
        ArbeidsfordelingKriterier kriterier = new ArbeidsfordelingKriterier();

        Tema tema = new Tema();
        tema.setValue(Objects.requireNonNull(TEMAER.get(ytelseType), "Har ikke mapping for ytelseType=" + ytelseType));
        kriterier.setTema(tema);

        Diskresjonskoder diskresjonskoder = new Diskresjonskoder();
        diskresjonskoder.setValue(diskresjonskode);
        kriterier.setDiskresjonskode(diskresjonskoder);

        Geografi geografi = new Geografi();
        geografi.setValue(geografiskTilknytning);
        kriterier.setGeografiskTilknytning(geografi);

        request.setArbeidsfordelingKriterier(kriterier);
        return request;
    }

    private Organisasjonsenhet validerOgVelgBehandlendeEnhet(String geografiskTilknytning,
                                                             String diskresjonskode,
                                                             FagsakYtelseType ytelseType,
                                                             FinnBehandlendeEnhetListeResponse response) {
        List<Organisasjonsenhet> behandlendeEnheter = response.getBehandlendeEnhetListe();

        // Vi forventer å få én behandlende enhet.
        if (behandlendeEnheter == null || behandlendeEnheter.isEmpty()) {
            throw ArbeidsfordelingFeil.FACTORY.finnerIkkeBehandlendeEnhet(geografiskTilknytning, diskresjonskode, ytelseType).toException();
        }

        // Vi forventer å få én behandlende enhet.
        Organisasjonsenhet valgtBehandlendeEnhet = behandlendeEnheter.get(0);
        if (behandlendeEnheter.size() > 1) {
            List<String> enheter = behandlendeEnheter.stream().map(Organisasjonsenhet::getEnhetId).collect(Collectors.toList());
            ArbeidsfordelingFeil.FACTORY.fikkFlereBehandlendeEnheter(geografiskTilknytning, diskresjonskode, ytelseType, enheter,
                valgtBehandlendeEnhet.getEnhetId()).log(logger);
        }
        return valgtBehandlendeEnhet;
    }

    private List<OrganisasjonsEnhet> tilOrganisasjonsEnhetListe(FinnAlleBehandlendeEnheterListeResponse response,
                                                                FagsakYtelseType ytelseType, boolean medKlage) {
        List<Organisasjonsenhet> responsEnheter = response.getBehandlendeEnhetListe();

        if (responsEnheter == null || responsEnheter.isEmpty()) {
            throw ArbeidsfordelingFeil.FACTORY.finnerIkkeAlleBehandlendeEnheter(ytelseType).toException();
        }

        List<OrganisasjonsEnhet> organisasjonsEnhetListe = responsEnheter.stream()
            .map(responsOrgEnhet -> new OrganisasjonsEnhet(responsOrgEnhet.getEnhetId(), responsOrgEnhet.getEnhetNavn(),
                responsOrgEnhet.getStatus().name()))
            .collect(Collectors.toList());

        if (medKlage) {
            // Hardkodet inn for Klageinstans da den ikke kommer med i response fra NORG. Fjern dette når det er validert på plass.
            organisasjonsEnhetListe.add(KLAGE_ENHET);
        }

        return organisasjonsEnhetListe;
    }

    public OrganisasjonsEnhet hentEnhetForDiskresjonskode(String kode, FagsakYtelseType ytelseType) {

        FinnAlleBehandlendeEnheterListeRequest request = lagRequestForHentAlleBehandlendeEnheter(ytelseType, Optional.of(kode));

        try {
            FinnAlleBehandlendeEnheterListeResponse response = consumer.finnAlleBehandlendeEnheterListe(request);
            return tilOrganisasjonsEnhetListe(response, ytelseType, false).get(0);
        } catch (FinnAlleBehandlendeEnheterListeUgyldigInput e) {
            throw ArbeidsfordelingFeil.FACTORY.finnAlleBehandlendeEnheterListeUgyldigInput(e).toException();
        }

    }

    public OrganisasjonsEnhet getKlageInstansEnhet() {
        return KLAGE_ENHET;
    }
}
