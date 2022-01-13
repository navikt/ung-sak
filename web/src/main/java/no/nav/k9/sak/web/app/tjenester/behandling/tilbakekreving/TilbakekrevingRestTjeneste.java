package no.nav.k9.sak.web.app.tjenester.behandling.tilbakekreving;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.økonomi.tilbakekreving.TilbakekrevingValgDto;
import no.nav.k9.sak.kontrakt.økonomi.tilbakekreving.VarseltekstDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.økonomi.tilbakekreving.modell.TilbakekrevingRepository;
import no.nav.k9.sak.økonomi.tilbakekreving.modell.TilbakekrevingValg;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;

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
import java.util.Optional;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.sak.web.app.tjenester.behandling.tilbakekreving.TilbakekrevingRestTjeneste.BASE_PATH;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

@Path(BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class TilbakekrevingRestTjeneste {

    public static final String POSTPATH_VARSELTEKST = "/varseltekst";
    static final String BASE_PATH = "/behandling/tilbakekreving";
    public static final String VARSELTEKST_PATH = BASE_PATH + POSTPATH_VARSELTEKST;
    private static final String POSTPATH_VALG = "/valg";
    public static final String VALG_PATH = BASE_PATH + POSTPATH_VALG;

    private BehandlingRepository behandlingRepository;
    private TilbakekrevingRepository tilbakekrevingRepository;

    public TilbakekrevingRestTjeneste() {
        //for CDI proxy
    }

    @Inject
    public TilbakekrevingRestTjeneste(BehandlingRepository behandlingRepository, TilbakekrevingRepository tilbakekrevingRepository) {
        this.behandlingRepository = behandlingRepository;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
    }

    private static TilbakekrevingValgDto map(TilbakekrevingValg valg) {
        return new TilbakekrevingValgDto(valg.getErTilbakekrevingVilkårOppfylt(), valg.getGrunnerTilReduksjon(), valg.getVidereBehandling(), valg.getVarseltekst());
    }

    @GET
    @Operation(description = "Hent tilbakekrevingsvalg for behandlingen", tags = "tilbakekrevingsvalg")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @Path(POSTPATH_VALG)
    public TilbakekrevingValgDto hentTilbakekrevingValg(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandlingId = behandlingUuid.getBehandlingUuid();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        Optional<TilbakekrevingValg> resultat = tilbakekrevingRepository.hent(behandling.getId());

        return resultat
            .map(TilbakekrevingRestTjeneste::map)
            .orElse(null);
    }

    @GET
    @Operation(description = "Henter varseltekst for tilbakekreving", tags = "tilbakekrevingsvalg")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @Path(POSTPATH_VARSELTEKST)
    public VarseltekstDto hentVarseltekst(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());

        Optional<TilbakekrevingValg> valgOpt = tilbakekrevingRepository.hent(behandling.getId());
        String varseltekst = valgOpt.map(TilbakekrevingValg::getVarseltekst).orElse(null);

        if (varseltekst == null || varseltekst.isEmpty()) {
            return null;
        }
        return new VarseltekstDto(varseltekst);
    }
}
