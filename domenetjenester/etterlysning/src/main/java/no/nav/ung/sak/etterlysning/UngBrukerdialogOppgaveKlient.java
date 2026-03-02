package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.UriBuilder;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.EndreFristDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.EndreOppgaveStatusDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OpprettOppgaveDto;
import no.nav.ung.brukerdialog.typer.AktørId;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.UUID;

@Dependent
@ScopedRestIntegration(scopeKey = "ung.brukerdialog.scope", defaultScope = "api://prod-gcp.k9saksbehandling.ung-brukerdialog/.default")
public class UngBrukerdialogOppgaveKlient implements OppgaveForSaksbehandlingGrensesnitt {

    private final OidcRestClient restClient;
    private final URI opprettURI;
    private final URI utløpForTypeOgPeriodeURI;
    private final URI avbrytForTypeOgPeriodeURI;
    private final URI avbrytURI;
    private final URI utløptURI;
    private final URI løsSøkYtelseBaseURI;
    private final URI endreFristURI;

    @Inject
    public UngBrukerdialogOppgaveKlient(
        OidcRestClient restClient,
        @KonfigVerdi(value = "ung.brukerdialog.url", defaultVerdi = "http://ung-brukerdialog.k9saksbehandling") String url) {
        this.restClient = restClient;
        this.opprettURI = tilUri(url, "saksbehandling/oppgave/opprett");
        this.avbrytURI = tilUri(url, "saksbehandling/oppgave/sett-avbrutt");
        this.utløptURI = tilUri(url, "saksbehandling/oppgave/sett-utlopt");
        this.utløpForTypeOgPeriodeURI = tilUri(url, "saksbehandling/oppgave/sett-utlopt");
        this.avbrytForTypeOgPeriodeURI = tilUri(url, "saksbehandling/oppgave/sett-avbrutt");
        this.endreFristURI = tilUri(url, "saksbehandling/oppgave/endre-frist");
        this.løsSøkYtelseBaseURI = tilUri(url, "saksbehandling/oppgave/los-sok-ytelse");
    }

    @Override
    public void avbrytOppgave(UUID eksternRef) {
        try {
            URI uri = UriBuilder.fromUri(avbrytURI).path(eksternRef.toString()).build();
            restClient.post(uri, null);
        } catch (Exception e) {
            throw UngOppgavetjenesteFeil.FACTORY.feilVedKallTilUngOppgaveTjeneste(e).toException();
        }
    }

    @Override
    public void opprettOppgave(OpprettOppgaveDto oppgave) {
        try {
            restClient.post(opprettURI, oppgave);
        } catch (Exception e) {
            throw UngOppgavetjenesteFeil.FACTORY.feilVedKallTilUngOppgaveTjeneste(e).toException();
        }
    }

    @Override
    public void oppgaveUtløpt(UUID eksternRef) {
        try {
            URI uri = UriBuilder.fromUri(utløptURI).path(eksternRef.toString()).build();
            restClient.post(uri, null);
        } catch (Exception e) {
            throw UngOppgavetjenesteFeil.FACTORY.feilVedKallTilUngOppgaveTjeneste(e).toException();
        }
    }

    @Override
    public void settOppgaveTilUtløpt(EndreOppgaveStatusDto dto) {
        try {
            restClient.post(utløpForTypeOgPeriodeURI, dto);
        } catch (Exception e) {
            throw UngOppgavetjenesteFeil.FACTORY.feilVedKallTilUngOppgaveTjeneste(e).toException();
        }
    }

    @Override
    public void settOppgaveTilAvbrutt(EndreOppgaveStatusDto dto) {
        try {
            restClient.post(avbrytForTypeOgPeriodeURI, dto);
        } catch (Exception e) {
            throw UngOppgavetjenesteFeil.FACTORY.feilVedKallTilUngOppgaveTjeneste(e).toException();
        }
    }

    @Override
    public void løsSøkYtelseOppgave(AktørId aktørId) {
        try {
            URI uri = UriBuilder.fromUri(løsSøkYtelseBaseURI).queryParam("aktørId", aktørId).build();
            restClient.post(uri, null);
        } catch (Exception e) {
            throw UngOppgavetjenesteFeil.FACTORY.feilVedKallTilUngOppgaveTjeneste(e).toException();
        }
    }

    @Override
    public void endreFrist(AktørId aktørId, UUID eksternReferanse, LocalDateTime frist) {
        try {
            restClient.post(endreFristURI, new EndreFristDto(aktørId, eksternReferanse, frist));
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
