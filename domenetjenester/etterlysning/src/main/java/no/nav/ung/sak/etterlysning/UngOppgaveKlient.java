package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.periodeendring.EndretPeriodeOppgaveDTO;
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
    private final URI avbrytURI;
    private final URI utløptURI;
    private final URI opprettEndretStartdatoURI;
    private final URI opprettEndretSluttdatoURI;
    private final URI opprettEndretProgramperiodeURI;


    @Inject
    public UngOppgaveKlient(
        OidcRestClient restClient,
        @KonfigVerdi(value = "ungdomsprogramregister.url", defaultVerdi = "http://ung-deltakelse-opplyser.k9saksbehandling") String url) {
        this.restClient = restClient;
        opprettKontrollerRegisterInntektURI = tilUri(url, "oppgave/opprett/kontroll/registerinntekt");
        opprettEndretStartdatoURI = tilUri(url, "oppgave/opprett/endre/startdato");
        opprettEndretSluttdatoURI = tilUri(url, "oppgave/opprett/endre/sluttdato");
        opprettEndretProgramperiodeURI = tilUri(url, "oppgave/opprett/endre/programperiode");
        avbrytURI = tilUri(url, "oppgave/avbryt");
        utløptURI = tilUri(url, "oppgave/utløpt");
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

    public void oppgaveUtløpt(UUID eksternRef) {
        try {
            restClient.post(utløptURI, eksternRef);
        } catch (Exception e) {
            throw UngOppgavetjenesteFeil.FACTORY.feilVedKallTilUngOppgaveTjeneste(e).toException();
        }
    }

    public void opprettEndretStartdatoOppgave(EndretPeriodeOppgaveDTO endretPeriodeOppgaveDTO) {
        try {
            restClient.post(opprettEndretStartdatoURI, endretPeriodeOppgaveDTO);
        } catch (Exception e) {
            throw UngOppgavetjenesteFeil.FACTORY.feilVedKallTilUngOppgaveTjeneste(e).toException();
        }
    }

    public void opprettEndretSluttdatoOppgave(EndretPeriodeOppgaveDTO endretPeriodeOppgaveDTO) {
        try {
            restClient.post(opprettEndretSluttdatoURI, endretPeriodeOppgaveDTO);
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

