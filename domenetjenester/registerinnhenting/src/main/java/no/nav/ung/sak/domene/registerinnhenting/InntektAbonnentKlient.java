package no.nav.ung.sak.domene.registerinnhenting;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.exception.HttpStatuskodeException;
import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;
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
import java.util.Optional;

@Dependent
@ScopedRestIntegration(scopeKey = "inntektskomponenten.scope", defaultScope = "api://prod-fss.team-inntekt.ikomp/.default")
public class InntektAbonnentKlient {

    private static final Logger log = LoggerFactory.getLogger(InntektAbonnentKlient.class);
    public static final int INNTEKT_HENDELSE_LIMIT = 1000;

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
        try {
            var request = new OpprettAbonnementRequest(
                aktørId.getId(),
                formaal,
                filter,
                fomMaanedObservasjon,
                tomMaanedObservasjon,
                sisteBruksdag
            );
            AbonnementResponse response = oidcRestClient.post(opprettAbonnementURI, request, AbonnementResponse.class);

            var abonnement = new InntektAbonnement(String.valueOf(response.abonnementId()), aktørId);
            log.info("Opprettet abonnementId: {} for aktør: {}", response.abonnementId(), aktørId.getId());

            return abonnement;
        } catch (Exception e) {
            throw RestTjenesteFeil.FEIL.feilVedOpprettelseAvAbonnement(e).toException();
        }
    }

    public Optional<Long> hentStartSekvensnummer(LocalDate dato) {
        try {
            var request = new AbonnementHendelseStartApiInn(dato);
            AbonnementHendelseStartApiUt response = oidcRestClient.post(hendelseStartURI, request, AbonnementHendelseStartApiUt.class);
            return Optional.of(response.sekvensnummer());
        } catch (HttpStatuskodeException e) {
            if(e.getHttpStatuskode() == 404) {
                return Optional.empty();
            }
            throw RestTjenesteFeil.FEIL.feilVedHentingAvStartsekvensnummer(dato, e).toException();
        }
    }

    public List<AbonnementHendelse> hentAbonnentHendelser(long fraSekvensnummer, List<String> filter) {
        try {
            var request = new InntektHendelserRequest(fraSekvensnummer, INNTEKT_HENDELSE_LIMIT, filter);
            AbonnementHendelseApiUt response = oidcRestClient.post(hendelseURI, request, AbonnementHendelseApiUt.class);
            return response.data();
        } catch (Exception e) {
            throw RestTjenesteFeil.FEIL.feilVedHentingAvHendelser(fraSekvensnummer, e).toException();
        }
    }

    public void avsluttAbonnement(long abonnementId) {
        try {
            var uri = URI.create(String.format(avsluttAbonnementURI.toString(), abonnementId));
            oidcRestClient.delete(uri);
            log.info("Avslutter abonnement med id {}", abonnementId);
        } catch (Exception e) {
            throw RestTjenesteFeil.FEIL.feilVedAvsluttingAvAbonnement(abonnementId, e).toException();
        }
    }

    private static URI tilUri(String baseUrl, String path) {
        try {
            return new URI(baseUrl + "/" + path);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig konfigurasjon for inntektskomponenten.url", e);
        }
    }

    interface RestTjenesteFeil extends DeklarerteFeil {
        RestTjenesteFeil FEIL = FeilFactory.create(RestTjenesteFeil.class);

        @TekniskFeil(feilkode = "UNG-947528", feilmelding = "Feil ved opprettelse av abonnement", logLevel = LogLevel.WARN)
        Feil feilVedOpprettelseAvAbonnement(Throwable t);

        @TekniskFeil(feilkode = "UNG-440604", feilmelding = "Ingen hendelser funnet siden dato: %s", logLevel = LogLevel.INFO)
        Feil ingenHendelserFunnet(LocalDate dato);

        @TekniskFeil(feilkode = "UNG-440600", feilmelding = "Feil ved henting av startsekvensnummer for dato: %s", logLevel = LogLevel.WARN)
        Feil feilVedHentingAvStartsekvensnummer(LocalDate dato, Throwable t);

        @TekniskFeil(feilkode = "UNG-769025", feilmelding = "Feil ved henting av hendelser fra sekvensnummer: %s", logLevel = LogLevel.WARN)
        Feil feilVedHentingAvHendelser(long fraSekvensnummer, Throwable t);

        @TekniskFeil(feilkode = "UNG-328650", feilmelding = "Feil ved avslutning av abonnement: %s", logLevel = LogLevel.WARN)
        Feil feilVedAvsluttingAvAbonnement(long abonnementId, Throwable t);
    }

    private record OpprettAbonnementRequest(
        String norskident,
        String formaal,
        List<String> filter,
        String fomMaanedObservasjon,
        String tomMaanedObservasjon,
        LocalDate sisteBruksdag
    ) {}

    private record AbonnementResponse(String abonnementId) {
    }

    private record AbonnementHendelseStartApiInn(LocalDate dato) {}

    private record AbonnementHendelseStartApiUt(long sekvensnummer) {}

    private record InntektHendelserRequest(
        long fra,
        int antall,
        List<String> filter
    ) {}

    private record AbonnementHendelseApiUt(List<AbonnementHendelse> data) {}

    record AbonnementHendelse(
        long sekvensnummer,
        String norskident,
        String maaned,
        String behandlet,
        List<String> filter
    ) {}
}
