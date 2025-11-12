package no.nav.ung.sak.domene.registerinnhenting;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.exception.TekniskException;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.InntektAbonnement;
import no.nav.ung.sak.typer.AktørId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.List;

@Dependent
//TODO Endre default verdi til produksjonsurl når vi går til prod
@ScopedRestIntegration(scopeKey = "inntektskomponenten.scope", defaultScope = "api://ikomp-q2.intern.dev.nav.no/.default")
public class InntektAbonnentKlient {

    private static final Logger LOG = LoggerFactory.getLogger(InntektAbonnentKlient.class);

    private OidcRestClient oidcRestClient;
    private final URI opprettAbonnementURI;
    private final URI avsluttAbonnementURI;
    private final URI hendelseStartURI;
    private final URI hendelseURI;

    @Inject
    public InntektAbonnentKlient(
        @KonfigVerdi(value = "inntektskomponenten.url",
            defaultVerdi = "http://ikomp.team-inntekt") String baseUrl,
        OidcRestClient oidcRestClient) {
        this.oidcRestClient = oidcRestClient;
        this.opprettAbonnementURI = tilUri(baseUrl, "rs/api/v1/abonnement");
        this.avsluttAbonnementURI = tilUri(baseUrl, "rs/api/v1/abonnement/%s");
        this.hendelseStartURI = tilUri(baseUrl, "rest/v2/abonnement/hendelse/start");
        this.hendelseURI = tilUri(baseUrl, "rest/v2/abonnement/hendelse");
    }

    public InntektAbonnement opprettAbonnement(AktørId aktørId, String formaal, List<String> filter,
                                               String fomMaanedObservasjon, String tomMaanedObservasjon,
                                               LocalDate sisteBruksdag) {
        var request = new OpprettAbonnementRequest(
            aktørId.getId(),
            formaal,
            filter,
            fomMaanedObservasjon,
            tomMaanedObservasjon,
            sisteBruksdag
        );
        AbonnementResponse response;
        try {
            response = oidcRestClient.post(opprettAbonnementURI, request, AbonnementResponse.class);
        } catch (Exception e) {
            throw new TekniskException("UNG-947528", "Feil ved opprettelse av abonnement", e);
        }

        var abonnement = new InntektAbonnement(String.valueOf(response.abonnementId()), aktørId);
        LOG.info("Opprettet abonnementId: {} for aktør: {}", response.abonnementId(), aktørId.getId());
        return abonnement;
    }

    public long hentStartSekvensnummer(LocalDate dato) {
        var request = new HendelseStartRequest(dato);
        HendelseStartResponse response;
        try {
            response = oidcRestClient.post(hendelseStartURI, request, HendelseStartResponse.class);
        } catch (Exception e) {
            throw new TekniskException("UNG-440600", "Feil ved henting av startsekvensnummer", e);
        }
        return response.sekvensnummer();
    }

    public HendelserResponse sjekkNyeHendelser(long fraSekvensnummer, int antall, List<String> filter) {
        var request = new HendelserRequest(fraSekvensnummer, antall, filter);
        HendelserResponse response;
        try {
            response = oidcRestClient.post(hendelseURI, request, HendelserResponse.class);
        } catch (Exception e) {
            throw new TekniskException("UNG-769025", "Feil ved henting av nye hendelser", e);
        }
        return response;
    }

    public void avsluttAbonnement(long abonnementId) {
        try {
            var uri = URI.create(String.format(avsluttAbonnementURI.toString(), abonnementId));
            // TODO fikse delete kall mot inntektskomponenten
            oidcRestClient.delete(uri);
        } catch (Exception e) {
            throw new TekniskException("UNG-328650", "Feil ved avslutning av abonnement", e);
        }
        LOG.info("Avsluttet abonnement {}", abonnementId);
    }

    private static URI tilUri(String baseUrl, String path) {
        try {
            return new URI(baseUrl + "/" + path);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig konfigurasjon for inntektskomponenten.url", e);
        }
    }

    public record OpprettAbonnementRequest(
        String norskident,
        String formaal,
        List<String> filter,
        String fomMaanedObservasjon,
        String tomMaanedObservasjon,
        LocalDate sisteBruksdag
    ) {}

    public record AbonnementResponse(String abonnementId) {
    }

    public record HendelseStartRequest(LocalDate dato) {}

    public record HendelseStartResponse(long sekvensnummer) {}

    public record HendelserRequest(
        long fra,
        int antall,
        List<String> filter
    ) {}

    public record HendelserResponse(List<HendelseData> data) {}

    public record HendelseData(
        long sekvensnummer,
        String norskident,
        String maaned,
        String behandlet,
        List<String> filter
    ) {}






}
