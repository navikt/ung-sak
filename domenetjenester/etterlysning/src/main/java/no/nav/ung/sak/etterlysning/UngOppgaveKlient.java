package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.periodeendring.EndretProgamperiodeOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektOppgaveDTO;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

@Dependent
@ScopedRestIntegration(scopeKey = "ungdomsprogramregister.scope", defaultScope = "api://prod-gcp.k9saksbehandling.ung-deltakelse-opplyser/.default")
public class UngOppgaveKlient {
    private final OidcRestClient restClient;
    private final URI opprettKontrollerRegisterInntektURI;
    private final URI opprettInntektrapporteringURI;
    private final URI avbrytURI;
    private final URI utløptURI;
    private final URI opprettEndretProgramperiodeURI;


    @Inject
    public UngOppgaveKlient(
        OidcRestClient restClient,
        @KonfigVerdi(value = "ungdomsprogramregister.url", defaultVerdi = "http://ung-deltakelse-opplyser.k9saksbehandling") String url) {
        this.restClient = restClient;
        this.opprettKontrollerRegisterInntektURI = tilUri(url, "oppgave/opprett/kontroll/registerinntekt");
        this.opprettEndretProgramperiodeURI = tilUri(url, "oppgave/opprett/endre/programperiode");
        this.opprettInntektrapporteringURI = tilUri(url, "oppgave/opprett/inntektsrapportering");
        this.avbrytURI = tilUri(url, "oppgave/avbryt");
        this.utløptURI = tilUri(url, "oppgave/utløpt");
    }

    public void avbrytOppgave(UUID eksternRef) {
        try {
            restClient.post(avbrytURI, eksternRef);
        } catch (Exception e) {
            throw UngOppgavetjenesteFeil.FACTORY.feilVedKallTilUngOppgaveTjeneste(e).toException();
        }

    }

    public void opprettKontrollerRegisterInntektOppgave(RegisterInntektOppgaveDTO oppgaver) {
        try {
            restClient.post(opprettKontrollerRegisterInntektURI, oppgaver);
        } catch (Exception e) {
            throw UngOppgavetjenesteFeil.FACTORY.feilVedKallTilUngOppgaveTjeneste(e).toException();
        }
    }


    public void opprettInntektrapporteringOppgave(RegisterInntektOppgaveDTO oppgaver) {
        try {
            restClient.post(opprettInntektrapporteringURI, oppgaver);
        } catch (Exception e) {
            throw UngOppgavetjenesteFeil.FACTORY.feilVedKallTilUngOppgaveTjeneste(e).toException();
        }
    }

    public void oppgaveUtløpt(UUID eksternRef) {
        try {
            restClient.post(utløptURI, eksternRef);
        } catch (Exception e) {
            throw UngOppgavetjenesteFeil.FACTORY.feilVedKallTilUngOppgaveTjeneste(e).toException();
        }
    }

    public void opprettEndretSluttdatoOppgave(EndretProgamperiodeOppgaveDTO endretPeriodeOppgaveDTO) {
        try {
            restClient.post(opprettEndretProgramperiodeURI, endretPeriodeOppgaveDTO);
        } catch (Exception e) {
            throw UngOppgavetjenesteFeil.FACTORY.feilVedKallTilUngOppgaveTjeneste(e).toException();
        }
    }


    private static URI tilUri(String baseUrl, String path) {
        try {
            return new URI(baseUrl + "/" + path);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig konfigurasjon for ungdomsprogram.register.url", e);
        }
    }

}

