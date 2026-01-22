package no.nav.ung.sak.domene.registerinnhenting;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.exception.HttpStatuskodeException;
import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.IntegrasjonFeil;
import no.nav.k9.felles.integrasjon.rest.DefaultJsonMapper;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.sak.felles.typer.PersonIdent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Dependent
@ScopedRestIntegration(scopeKey = "inntektskomponenten.scope", defaultScope = "api://prod-fss.team-inntekt.ikomp/.default")
public class InntektAbonnentKlient {

    private static final Logger log = LoggerFactory.getLogger(InntektAbonnentKlient.class);
    public static final int INNTEKT_HENDELSE_LIMIT = 100;

    private final OidcRestClient oidcRestClient;
    private final URI opprettAbonnementURI;
    private final URI avsluttAbonnementURI;
    private final URI hendelseStartURI;
    private final URI hendelseURI;

    @Inject
    public InntektAbonnentKlient(
        @KonfigVerdi(value = "inntektskomponenten.url",
            defaultVerdi = "https://ikomp.prod-fss-pub.nais.io//rest/v2/abonnement") String baseUrl,
        OidcRestClient oidcRestClient) {
        this.oidcRestClient = oidcRestClient;
        this.opprettAbonnementURI = tilUri(baseUrl, "administrasjon/opprett");
        this.avsluttAbonnementURI = tilUri(baseUrl, "administrasjon/opphoer");
        this.hendelseStartURI = tilUri(baseUrl, "hendelse/start");
        this.hendelseURI = tilUri(baseUrl, "hendelse");
    }

    public long opprettAbonnement(PersonIdent personIdent, String formaal, List<String> filter,
                                               YearMonth månedFom, YearMonth månedTom,
                                               LocalDate sisteBruksdag, int bevaringstid) {
        try {
            var request = new AbonnementAdministrasjonOpprettApiInn(
                personIdent.getIdent(),
                filter,
                formaal,
                månedFom,
                månedTom,
                sisteBruksdag,
                bevaringstid
            );

            if (Environment.current().isDev()){
                String requestJson = DefaultJsonMapper.getObjectMapper().writeValueAsString(request);
                log.info("Oppretter abonnement i Inntektskomponenten med request Base64: {}", Base64.getEncoder().encodeToString(requestJson.getBytes(StandardCharsets.UTF_8)));
                log.info("Oppretter abonnement i Inntektskomponenten med request: {}", requestJson);
            }

            AbonnementAdministrasjonOpprettApiUt response = oidcRestClient.post(opprettAbonnementURI, request, AbonnementAdministrasjonOpprettApiUt.class);

            log.info("Opprettet abonnementId: {}", response.abonnementId());
            return response.abonnementId();
        } catch (Exception e) {
            throw InntektAbonnentKlientFeil.FEIL.feilVedOpprettelseAvAbonnement(e).toException();
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
            throw InntektAbonnentKlientFeil.FEIL.feilVedHentingAvStartsekvensnummer(dato, e).toException();
        }
    }

    public List<AbonnementHendelse> hentAbonnentHendelser(long fraSekvensnummer, List<String> filter) {
        try {
            var request = new InntektHendelserRequest(fraSekvensnummer, INNTEKT_HENDELSE_LIMIT, filter);
            AbonnementHendelseApiUt response = oidcRestClient.post(hendelseURI, request, AbonnementHendelseApiUt.class);
            return response.data();
        } catch (Exception e) {
            throw InntektAbonnentKlientFeil.FEIL.feilVedHentingAvHendelser(fraSekvensnummer, e).toException();
        }
    }

    public void avsluttAbonnement(long abonnementId) {
        try {
            var request = new AbonnementAdministrasjonOpphoerApiInn(abonnementId);
            oidcRestClient.post(avsluttAbonnementURI, request, AbonnementAdministrasjonOpphoerApiUt.class);
            log.info("Avslutter abonnement med id {}", abonnementId);
        } catch (Exception e) {
            throw InntektAbonnentKlientFeil.FEIL.feilVedAvsluttingAvAbonnement(abonnementId, e).toException();
        }
    }

    private static URI tilUri(String baseUrl, String path) {
        try {
            return new URI(baseUrl + "/" + path);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig konfigurasjon for inntektskomponenten.url", e);
        }
    }

    interface InntektAbonnentKlientFeil extends DeklarerteFeil {
        InntektAbonnentKlientFeil FEIL = FeilFactory.create(InntektAbonnentKlientFeil.class);

        @IntegrasjonFeil(feilkode = "UNG-947528", feilmelding = "Feil ved opprettelse av abonnement", logLevel = LogLevel.WARN)
        Feil feilVedOpprettelseAvAbonnement(Throwable t);

        @IntegrasjonFeil(feilkode = "UNG-440600", feilmelding = "Feil ved henting av startsekvensnummer for dato: %s", logLevel = LogLevel.WARN)
        Feil feilVedHentingAvStartsekvensnummer(LocalDate dato, Throwable t);

        @IntegrasjonFeil(feilkode = "UNG-769025", feilmelding = "Feil ved henting av hendelser fra sekvensnummer: %s", logLevel = LogLevel.WARN)
        Feil feilVedHentingAvHendelser(long fraSekvensnummer, Throwable t);

        @IntegrasjonFeil(feilkode = "UNG-328650", feilmelding = "Feil ved avslutning av abonnement: %s", logLevel = LogLevel.WARN)
        Feil feilVedAvsluttingAvAbonnement(long abonnementId, Throwable t);
    }

    private record AbonnementAdministrasjonOpprettApiInn(
        String norskident,
        List<String> filter,
        String formaal,
        YearMonth maanedFom,
        YearMonth maanedTom,
        LocalDate sisteBruksdag,
        int bevaringstid
    ) {}

    private record AbonnementAdministrasjonOpprettApiUt(long abonnementId) {
    }

    private record AbonnementAdministrasjonOpphoerApiInn(long abonnementId) {}

    private record AbonnementAdministrasjonOpphoerApiUt() {}

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
        YearMonth maaned,
        LocalDateTime behandlet,
        List<String> filter
    ) {}
}
