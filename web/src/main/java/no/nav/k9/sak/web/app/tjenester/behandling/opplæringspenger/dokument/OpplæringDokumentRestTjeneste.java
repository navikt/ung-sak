package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.dokument;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.Function;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
import no.nav.k9.felles.exception.ManglerTilgangException;
import no.nav.k9.felles.exception.TekniskException;
import no.nav.k9.felles.sikkerhet.abac.AbacDataAttributter;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.dokument.arkiv.DokumentArkivTjeneste;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.opplæringspenger.dokument.OpplæringDokumentDto;
import no.nav.k9.sak.kontrakt.sykdom.dokument.SykdomDokumentIdDto;
import no.nav.k9.sak.web.app.tjenester.dokument.DokumentRestTjenesteFeil;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.dokument.OpplæringDokument;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.dokument.OpplæringDokumentRepository;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path(OpplæringDokumentRestTjeneste.BASE_PATH)
@Transactional
public class OpplæringDokumentRestTjeneste {

    public static final String BASE_PATH = "/behandling/opplæring/dokument";
    private static final String DOKUMENT = "/";
    private static final String DOKUMENT_INNHOLD = "/innhold";
    public static final String DOKUMENT_PATH = BASE_PATH + DOKUMENT;
    public static final String DOKUMENT_INNHOLD_PATH = BASE_PATH + DOKUMENT_INNHOLD;
    private static final String DOKUMENT_OVERSIKT = "/oversikt";
    private static final String DOKUMENT_LISTE = "/liste";
    public static final String DOKUMENT_LISTE_PATH = BASE_PATH + DOKUMENT_LISTE;

    private BehandlingRepository behandlingRepository;
    private OpplæringDokumentMapper opplæringDokumentMapper;
    private OpplæringDokumentRepository opplæringDokumentRepository;
    private DokumentArkivTjeneste dokumentArkivTjeneste;

    public OpplæringDokumentRestTjeneste() {
    }

    @Inject
    public OpplæringDokumentRestTjeneste(BehandlingRepository behandlingRepository,
                                         OpplæringDokumentMapper opplæringDokumentMapper,
                                         OpplæringDokumentRepository opplæringDokumentRepository,
                                         DokumentArkivTjeneste dokumentArkivTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.opplæringDokumentMapper = opplæringDokumentMapper;
        this.opplæringDokumentRepository = opplæringDokumentRepository;
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
    }

    @GET
    @Path(DOKUMENT_LISTE)
    @Operation(description = "Henter en liste over dokumenter som kan brukes i vurdering.",
        summary = "Henter en liste over dokumenter som kan brukes i vurdering.",
        tags = "opplæringspenger",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = OpplæringDokumentDto.class)))
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public List<OpplæringDokumentDto> hentDokumenter(
        @QueryParam(BehandlingUuidDto.NAME)
        @Parameter(description = BehandlingUuidDto.DESC)
        @NotNull
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
        BehandlingUuidDto behandlingUuid) {
        final var behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingUuid.getBehandlingUuid()).orElseThrow();

        final List<OpplæringDokument> dokumenter = opplæringDokumentRepository.hentDokumenterForSak(behandling.getFagsak().getSaksnummer());
        return opplæringDokumentMapper.mapDokumenter(behandling.getUuid(), dokumenter);
    }

    @GET
    @Path(DOKUMENT_INNHOLD)
    @Operation(description = "Laster ned selve dokumentet (innholdet).", summary = ("Laster ned selve dokumentet (innholdet)."), tags = "opplæringspenger")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public Response hentDokumentinnhold(
        @NotNull @QueryParam(BehandlingUuidDto.NAME)
        @Parameter(description = BehandlingUuidDto.DESC)
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
        BehandlingUuidDto behandlingUuid,

        @QueryParam(SykdomDokumentIdDto.NAME)
        @Parameter(description = SykdomDokumentIdDto.DESC)
        @NotNull
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacDataSupplier.class)
        SykdomDokumentIdDto dokumentId) {
        final var behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingUuid.getBehandlingUuid()).orElseThrow();
        final var dokument = opplæringDokumentRepository.hentDokument(Long.valueOf(dokumentId.getSykdomDokumentId()), behandling.getFagsak().getSaksnummer()).orElseThrow();
        try {
            Response.ResponseBuilder responseBuilder = Response.ok(
                new ByteArrayInputStream(dokumentArkivTjeneste.hentDokumnet(dokument.getJournalpostId(), dokument.getDokumentInfoId())));
            responseBuilder.type("application/pdf");
            responseBuilder.header("Content-Disposition", "filename=dokument.pdf");
            return responseBuilder.build();
        } catch (TekniskException e) {
            throw DokumentRestTjenesteFeil.FACTORY.dokumentIkkeFunnet(dokument.getJournalpostId(), dokument.getDokumentInfoId(), e).toException();
        } catch (ManglerTilgangException e) {
            throw DokumentRestTjenesteFeil.FACTORY.applikasjonHarIkkeTilgangTilHentDokumentTjeneste(e).toException();
        }
    }

    public static class AbacDataSupplier implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett();
        }
    }
}
