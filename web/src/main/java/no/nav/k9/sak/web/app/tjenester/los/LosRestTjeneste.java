package no.nav.k9.sak.web.app.tjenester.los;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.k9.sak.web.app.tjenester.los.LosRestTjeneste.BASE_PATH;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sikkerhet.context.SubjectHandler;

@ApplicationScoped
@Path(BASE_PATH)
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class LosRestTjeneste {

    public static final String BASE_PATH = "/los";
    public static final String MERKNAD = "/merknad";
    public static final String MERKNAD_PATH = BASE_PATH + MERKNAD;

    private LosSystemUserKlient losKlient;

    private HistorikkRepository historikkRepository;

    private BehandlingRepository behandlingRepository;

    public LosRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public LosRestTjeneste(
        LosSystemUserKlient losKlient,
        HistorikkRepository historikkRepository,
        BehandlingRepository behandlingRepository
    ) {
        this.losKlient = losKlient;
        this.historikkRepository = historikkRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @GET
    @Path(MERKNAD)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter merknad på oppgave i los", tags = "merknad")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getMerknad(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var merknad = losKlient.hentMerknad(behandlingUuid.getBehandlingUuid());
        var response = (merknad == null) ? Response.noContent() : Response.ok(merknad);
        return response.build();
    }

    @POST
    @Path(MERKNAD)
    @Operation(description = "Lagrer merknad på oppgave i los", tags = "merknad")
    @BeskyttetRessurs(action = READ, resource = FAGSAK) // Står som read så veileder har tilgang
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response postMerknad(@Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) MerknadEndretDto merknadEndret) {
        var merknad = losKlient.lagreMerknad(merknadEndret.overstyrSaksbehandlerIdent(getCurrentUserId()));
        if (merknad == null) {
            lagHistorikkinnslag(merknadEndret, HistorikkinnslagType.MERKNAD_FJERNET);
            return Response.noContent().build();
        } else {
            lagHistorikkinnslag(merknadEndret, HistorikkinnslagType.MERKNAD_NY);
            return Response.ok(merknad).build();
        }
    }

    private void lagHistorikkinnslag(MerknadEndretDto merknad, HistorikkinnslagType historikkinnslagType) {
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
        historikkinnslag.setBehandling(behandlingRepository.hentBehandling(merknad.behandlingUuid()));
        historikkinnslag.setType(historikkinnslagType);

        new HistorikkInnslagTekstBuilder()
            .medBegrunnelse(merknad.fritekst())
            .medHendelse(historikkinnslagType, String.join(",", merknad.merknadKoder()))
            .build(historikkinnslag);

        historikkRepository.lagre(historikkinnslag);
    }

    private static String getCurrentUserId() {
        return SubjectHandler.getSubjectHandler().getUid();
    }
}
