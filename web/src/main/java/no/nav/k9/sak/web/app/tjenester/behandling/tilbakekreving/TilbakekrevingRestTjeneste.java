package no.nav.k9.sak.web.app.tjenester.behandling.tilbakekreving;

import static no.nav.k9.sak.web.app.tjenester.behandling.tilbakekreving.TilbakekrevingRestTjeneste.BASE_PATH;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.økonomi.tilbakekreving.modell.TilbakekrevingRepository;
import no.nav.foreldrepenger.økonomi.tilbakekreving.modell.TilbakekrevingValg;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.økonomi.tilbakekreving.TilbakekrevingValgDto;
import no.nav.k9.sak.kontrakt.økonomi.tilbakekreving.VarseltekstDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

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
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
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
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
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
