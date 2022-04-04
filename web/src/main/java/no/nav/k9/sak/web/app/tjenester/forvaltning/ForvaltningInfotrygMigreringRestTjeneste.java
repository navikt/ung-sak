package no.nav.k9.sak.web.app.tjenester.forvaltning;

import static no.nav.k9.abac.BeskyttetRessursKoder.DRIFT;
import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.List;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtEmptySupplier;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.PSBVilkårsPerioderTilVurderingTjeneste;

@ApplicationScoped
@Transactional
@Path("/infotrygdmigrering")
public class ForvaltningInfotrygMigreringRestTjeneste {

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private PSBVilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;

    public ForvaltningInfotrygMigreringRestTjeneste() {
    }

    @Inject
    public ForvaltningInfotrygMigreringRestTjeneste(FagsakRepository fagsakRepository,
                                                    BehandlingRepository behandlingRepository,
                                                    @FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN) PSBVilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
    }

    @GET
    @Path("/skjæringstidspunkter")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent skjæringstidspunkter for infotrygdmigrering for saker", tags = "infotrygdmigrering", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer alle skjæringstidspunkt som har blitt lagret på sak",
            content = @Content(array = @ArraySchema(uniqueItems = true, arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = MigrertSkjæringstidspunktDto.class)), mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = READ, resource = DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getSkjæringstidspunkter(@QueryParam("saksnummer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) SaksnummerDto saksnummerDto) { // NOSONAR
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummerDto.getVerdi());
        var infotrygdMigreringer = fagsak.map(Fagsak::getId)
            .stream()
            .flatMap(id -> fagsakRepository.hentAlleSakInfotrygdMigreringer(id).stream())
            .map(migrering -> new MigrertSkjæringstidspunktDto(migrering.getSkjæringstidspunkt(), migrering.getAktiv()))
            .collect(Collectors.toList());
        return Response.ok(infotrygdMigreringer).build();
    }

    @POST
    @Path("/leggTilSkjæringstidspunkt")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Legger til migrert skjæringstidspunkt fra infotrygd for gitt sak", tags = "infotrygdmigrering")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public void leggTilSkjærinstidspunkt(@Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) MigrerFraInfotrygdDto migrerFraInfotrygdDto) { // NOSONAR
        var fagsak = fagsakRepository.hentSakGittSaksnummer(migrerFraInfotrygdDto.getSaksnummer()).orElseThrow();

        var behandling = behandlingRepository.hentSisteBehandlingForFagsakId(fagsak.getId()).orElseThrow();

        if (behandling.erStatusFerdigbehandlet()) {
            throw new IllegalStateException("Må ha åpen behandling");
        }

        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(behandling.getId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        if (perioderTilVurdering.stream().map(DatoIntervallEntitet::getFomDato).noneMatch(migrerFraInfotrygdDto.getSkjæringstidspunkt()::equals)) {
            throw new IllegalStateException("Fant ingen periode til vurdering med oppgitt skjæringstidspunkt");
        }

        fagsakRepository.opprettInfotrygdmigrering(fagsak.getId(), migrerFraInfotrygdDto.getSkjæringstidspunkt());
    }

}
