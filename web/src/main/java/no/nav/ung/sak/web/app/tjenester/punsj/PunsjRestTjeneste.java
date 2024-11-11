package no.nav.ung.sak.web.app.tjenester.punsj;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.sak.behandling.FagsakTjeneste;
import no.nav.ung.sak.behandling.prosessering.BehandlingsprosessApplikasjonTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.kontrakt.dokument.JournalpostIderDto;
import no.nav.ung.sak.punsj.PunsjRestKlient;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.ung.sak.kontrakt.behandling.SaksnummerDto;


@ApplicationScoped
@Transactional
@Path("/punsj")
@Produces(MediaType.APPLICATION_JSON)
public class PunsjRestTjeneste {

    private PunsjRestKlient klient;
    private FagsakTjeneste fagsakTjeneste;
    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste;

    public PunsjRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public PunsjRestTjeneste(PunsjRestKlient klient, FagsakTjeneste fagsakTjeneste, BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste) {
        this.klient = klient;
        this.fagsakTjeneste = fagsakTjeneste;
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
    }


    @GET
    @Path("/journalpost/uferdig")
    @Operation(description = "Henter uferdig journalposter fra punsj for en gitt behandlingUuid", tags = "journalposter", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer en liste med uferdig journalpostIder som ligger i punsj på gitt behandlingUuid.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = JournalpostIderDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @Deprecated
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getUferdigJournalpostIderPrAktoer(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingsprosessTjeneste.hentBehandling(behandlingUuid.getBehandlingUuid());
        AktørId søker = behandling.getAktørId();
        AktørId barnet = behandling.getFagsak().getPleietrengendeAktørId();

        String barnetAktørId = (barnet != null) ? barnet.getAktørId() : null;

        Optional<JournalpostIderDto> uferdigJournalpostIderPåAktør = klient.getUferdigJournalpostIderPåAktør(søker.getAktørId(), barnetAktørId);
        if (uferdigJournalpostIderPåAktør.isPresent()) {
            return Response.ok(uferdigJournalpostIderPåAktør.get()).build();
        }
        return Response.ok().build();
    }

    @GET
    @Path("/journalpost/uferdig/v2")
    @Operation(description = "Henter uferdige journalposter fra punsj for søker og pleietrengende", tags = "journalposter", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer en liste med uferdig journalpostIder som ligger i punsj på et gitt saksnummer.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = JournalpostIderDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getUferdigJournalpostIderPrAktoer(@NotNull @QueryParam(SaksnummerDto.NAME) @Parameter(description = SaksnummerDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto saksnummer) {
        Optional<Fagsak> fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer.getVerdi(), false);
        AktørId søker = fagsak.get().getAktørId();
        AktørId barnet = fagsak.get().getPleietrengendeAktørId();
        String barnetAktørId = (barnet != null) ? barnet.getAktørId() : null;

        Optional<JournalpostIderDto> uferdigJournalpostIderPåAktør = klient.getUferdigJournalpostIderPåAktør(søker.getAktørId(), barnetAktørId);
        if (uferdigJournalpostIderPåAktør.isPresent()) {
            return Response.ok(uferdigJournalpostIderPåAktør.get()).build();
        }
        return Response.ok().build();
    }
}
