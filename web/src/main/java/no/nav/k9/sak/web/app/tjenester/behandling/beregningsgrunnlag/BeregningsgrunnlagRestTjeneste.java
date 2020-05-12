package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusTjeneste;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

/**
 * Beregningsgrunnlag knyttet til en behandling.
 */
@ApplicationScoped
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class BeregningsgrunnlagRestTjeneste {

    static public final String PATH = "/behandling/beregningsgrunnlag";
    static public final String PATH_ALLE = "/behandling/beregningsgrunnlag/alle";
    private BehandlingRepository behandlingRepository;
    private OpptjeningRepository opptjeningRepository;
    private BeregningTjeneste kalkulusTjeneste;

    public BeregningsgrunnlagRestTjeneste() {
        // for resteasy
    }

    @Inject
    public BeregningsgrunnlagRestTjeneste(BehandlingRepository behandlingRepository,
                                          OpptjeningRepository opptjeningRepository,
                                          BeregningTjeneste kalkulusTjeneste) {
        this.opptjeningRepository = opptjeningRepository;
        this.behandlingRepository = behandlingRepository;
        this.kalkulusTjeneste = kalkulusTjeneste;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent beregningsgrunnlag for angitt behandling", summary = ("Returnerer beregningsgrunnlag for behandling."), tags = "beregningsgrunnlag")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @Path(PATH)
    @Deprecated
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public BeregningsgrunnlagDto hentBeregningsgrunnlag(@NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto behandlingId) {
        Long id = behandlingId.getBehandlingId();
        var behandling = id != null
            ? behandlingRepository.hentBehandling(id)
            : behandlingRepository.hentBehandling(behandlingId.getBehandlingUuid());
        final var opptjening = opptjeningRepository.finnOpptjening(behandling.getId());
        if (opptjening.isEmpty()) {
            return null;
        }
        return kalkulusTjeneste.hentBeregningsgrunnlagDtoer(BehandlingReferanse.fra(behandling)).stream().findFirst().orElse(null);
    }

    @GET
    @Operation(description = "Hent beregningsgrunnlag for angitt behandling", summary = ("Returnerer beregningsgrunnlag for behandling."), tags = "beregningsgrunnlag")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @Path(PATH)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public BeregningsgrunnlagDto hentBeregningsgrunnlag(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        if (FagsakYtelseType.FRISINN.equals(behandling.getFagsakYtelseType())) {
            // behandlinger for ytelse FRISINN har ikke opptjening
            return kalkulusTjeneste.hentBeregningsgrunnlagDtoer(BehandlingReferanse.fra(behandling)).stream().findFirst().orElse(null);
        }
        final var opptjening = opptjeningRepository.finnOpptjening(behandling.getId());
        if (opptjening.isEmpty()) {
            return null;
        }
        return kalkulusTjeneste.hentBeregningsgrunnlagDtoer(BehandlingReferanse.fra(behandling)).stream().findFirst().orElse(null);
    }

    @GET
    @Operation(description = "Henter alle beregningsgrunnlag for angitt behandling", summary = ("Returnerer beregningsgrunnlag for behandling."), tags = "beregningsgrunnlag")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @Path(PATH_ALLE)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<BeregningsgrunnlagDto> hentBeregningsgrunnlagene(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        if (FagsakYtelseType.FRISINN.equals(behandling.getFagsakYtelseType())) {
            // behandlinger for ytelse FRISINN har ikke opptjening
            return kalkulusTjeneste.hentBeregningsgrunnlagDtoer(BehandlingReferanse.fra(behandling));
        }
        final var opptjening = opptjeningRepository.finnOpptjening(behandling.getId());
        if (opptjening.isEmpty()) {
            return null;
        }
        return kalkulusTjeneste.hentBeregningsgrunnlagDtoer(BehandlingReferanse.fra(behandling));
    }

}
