package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.SettTilUtløptDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.inntektsrapportering.InntektsrapporteringOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretSluttdatoOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretStartdatoOppgaveDTO;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

@Dependent
@ScopedRestIntegration(scopeKey = "ungdomsprogramregister.scope", defaultScope = "api://prod-gcp.k9saksbehandling.ung-deltakelse-opplyser/.default")
public class UngOppgaveKlient {
    private final OidcRestClient restClient;
    private final URI opprettKontrollerRegisterInntektURI;
    private final URI opprettInntektrapporteringURI;
    private final URI utløpForTypeOgPeriodeURI;
    private final URI avbrytURI;
    private final URI utløptURI;
    private final URI opprettEndretSluttdatoURI;
    private final URI opprettEndretStartdatoURI;


    @Inject
    public UngOppgaveKlient(
        OidcRestClient restClient,
        @KonfigVerdi(value = "ungdomsprogramregister.url", defaultVerdi = "http://ung-deltakelse-opplyser.k9saksbehandling") String url) {
        this.restClient = restClient;
        this.opprettKontrollerRegisterInntektURI = tilUri(url, "oppgave/opprett/kontroll/registerinntekt");
        this.opprettEndretStartdatoURI = tilUri(url, "oppgave/opprett/endret-startdato");
        this.opprettEndretSluttdatoURI = tilUri(url, "oppgave/opprett/endret-sluttdato");
        this.opprettInntektrapporteringURI = tilUri(url, "oppgave/opprett/inntektsrapportering");
        this.avbrytURI = tilUri(url, "oppgave/avbryt");
        this.utløptURI = tilUri(url, "oppgave/utlopt");
        this.utløpForTypeOgPeriodeURI = tilUri(url, "oppgave/utlopt/forTypeOgPeriode");
    }

    public void avbrytOppgave(UUID eksternRef) {
        try {
            restClient.post(avbrytURI, eksternRef);
        } catch (Exception e) {
            throw UngOppgavetjenesteFeil.FACTORY.feilVedKallTilUngOppgaveTjeneste(e).toException();
        }

    }

    public void opprettKontrollerRegisterInntektOppgave(RegisterInntektOppgaveDTO oppgave) {
        try {
            restClient.post(opprettKontrollerRegisterInntektURI, oppgave);
        } catch (Exception e) {
            throw UngOppgavetjenesteFeil.FACTORY.feilVedKallTilUngOppgaveTjeneste(e).toException();
        }
    }


    public void opprettInntektrapporteringOppgave(InntektsrapporteringOppgaveDTO oppgave) {
        try {
            restClient.post(opprettInntektrapporteringURI, oppgave);
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

    public void settOppgaveTilUtløpt(SettTilUtløptDTO dto) {
        try {
            restClient.post(utløpForTypeOgPeriodeURI, dto);
        } catch (Exception e) {
            throw UngOppgavetjenesteFeil.FACTORY.feilVedKallTilUngOppgaveTjeneste(e).toException();
        }
    }


    public void opprettEndretSluttdatoOppgave(EndretSluttdatoOppgaveDTO endretSluttdatoOppgaveDTO) {
        try {
            restClient.post(opprettEndretSluttdatoURI, endretSluttdatoOppgaveDTO);
        } catch (Exception e) {
            throw UngOppgavetjenesteFeil.FACTORY.feilVedKallTilUngOppgaveTjeneste(e).toException();
        }
    }

    public void opprettEndretStartdatoOppgave(EndretStartdatoOppgaveDTO endretStartdatoOppgaveDTO) {
        try {
            restClient.post(opprettEndretStartdatoURI, endretStartdatoOppgaveDTO);
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

