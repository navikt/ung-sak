package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.List;
import java.util.stream.Collectors;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OverstyrInputBeregningTjeneste;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.beregninginput.OverstyrBeregningInputPeriode;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.BeregningsgrunnlagKoblingDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

/**
 * Beregningsgrunnlag knyttet til en behandling.
 */
@ApplicationScoped
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class BeregningsgrunnlagRestTjeneste {

    static public final String PATH = "/behandling/beregningsgrunnlag";
    static public final String PATH_KOBLINGER = "/behandling/beregningsgrunnlag/koblinger";
    static public final String PATH_ALLE = "/behandling/beregningsgrunnlag/alle";
    static public final String PATH_OVERSTYR_INPUT = "/behandling/beregningsgrunnlag/overstyrInput";
    private BehandlingRepository behandlingRepository;
    private BeregningTjeneste kalkulusTjeneste;
    private OverstyrInputBeregningTjeneste overstyrInputBeregningTjeneste;

    public BeregningsgrunnlagRestTjeneste() {
        // for resteasy
    }

    @Inject
    public BeregningsgrunnlagRestTjeneste(BehandlingRepository behandlingRepository,
                                          BeregningTjeneste kalkulusTjeneste,
                                          OverstyrInputBeregningTjeneste overstyrInputBeregningTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.overstyrInputBeregningTjeneste = overstyrInputBeregningTjeneste;
    }

    @GET
    @Operation(description = "Hent beregningsgrunnlag for angitt behandling", summary = ("Returnerer beregningsgrunnlag for behandling."), tags = "beregningsgrunnlag")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @Path(PATH)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public BeregningsgrunnlagDto hentBeregningsgrunnlag(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        return kalkulusTjeneste.hentBeregningsgrunnlagDtoer(BehandlingReferanse.fra(behandling)).stream().findFirst().orElse(null);
    }

    @GET
    @Operation(description = "Henter alle beregningsgrunnlag for angitt behandling", summary = ("Returnerer beregningsgrunnlag for behandling."), tags = "beregningsgrunnlag")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @Path(PATH_ALLE)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<BeregningsgrunnlagDto> hentBeregningsgrunnlagene(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        return kalkulusTjeneste.hentBeregningsgrunnlagDtoer(BehandlingReferanse.fra(behandling));
    }

    @GET
    @Operation(description = "Henter data for overstyring av input til beregning", summary = ("Returnerer data for overstyring av input til beregning."), tags = "beregningsgrunnlag")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @Path(PATH_OVERSTYR_INPUT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<OverstyrBeregningInputPeriode> hentOverstyrInputBeregning(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        return overstyrInputBeregningTjeneste.getPerioderForInputOverstyring(behandling);
    }

    @GET
    @Operation(description = "Henter alle koblingene for angitt behandling", summary = ("Henter alle koblingene for angitt behandling"), tags = "beregningsgrunnlag")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @Path(PATH_KOBLINGER)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<BeregningsgrunnlagKoblingDto> hentNøkkelknippe(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());

        return kalkulusTjeneste.hentKoblingerForInnvilgedePerioder(BehandlingReferanse.fra(behandling))
            .stream()
            .map(it -> new BeregningsgrunnlagKoblingDto(it.getSkjæringstidspunkt(), it.getReferanse(), it.getErTilVurdering()))
            .collect(Collectors.toList());
    }

}
