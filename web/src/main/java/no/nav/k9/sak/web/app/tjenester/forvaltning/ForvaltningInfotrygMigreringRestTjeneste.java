package no.nav.k9.sak.web.app.tjenester.forvaltning;

import static no.nav.k9.abac.BeskyttetRessursKoder.DRIFT;
import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.fagsak.SakInfotrygdMigrering;
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
    private VilkårResultatRepository vilkårResultatRepository;

    public ForvaltningInfotrygMigreringRestTjeneste() {
    }

    @Inject
    public ForvaltningInfotrygMigreringRestTjeneste(FagsakRepository fagsakRepository,
                                                    BehandlingRepository behandlingRepository,
                                                    @FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN) PSBVilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                                                    VilkårResultatRepository vilkårResultatRepository) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
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

    @POST
    @Path("/deaktiverSkjærinstidspunkt")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Deaktiverer migrert skjæringstidspunkt fra infotrygd for gitt sak", tags = "infotrygdmigrering")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.UPDATE, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public void deaktiverSkjærinstidspunkt(@Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) MigrerFraInfotrygdDto migrerFraInfotrygdDto) { // NOSONAR
        var fagsak = fagsakRepository.hentSakGittSaksnummer(migrerFraInfotrygdDto.getSaksnummer()).orElseThrow();

        var behandling = behandlingRepository.hentSisteBehandlingForFagsakId(fagsak.getId()).orElseThrow();

        if (behandling.erStatusFerdigbehandlet()) {
            throw new IllegalStateException("Må ha åpen behandling");
        }

        var alleSkjæringstidspunkt = finnAlleSkjæringstidspunkterForBehandling(behandling);

        if (alleSkjæringstidspunkt.stream().anyMatch(migrerFraInfotrygdDto.getSkjæringstidspunkt()::equals)) {
            throw new IllegalStateException("Støtter kun deaktivering for perioder som er fjernet");
        }

        fagsakRepository.deaktiverInfotrygdmigrering(fagsak.getId(), migrerFraInfotrygdDto.getSkjæringstidspunkt());

    }


    @GET
    @Path("/defaktiverteMigreringer")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent saker som har fått deaktivert migrering og har flere perioder", tags = "infotrygdmigrering", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer aker som har fått deaktivert migrering og har flere perioder",
            content = @Content(array = @ArraySchema(uniqueItems = true, arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = SaksnummerDto.class)), mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = READ, resource = DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response finnSakerMedDeaktivertMigrering() { // NOSONAR
        var grupperteMigreringer = fagsakRepository.hentAlleSakInfotrygdMigreringerForAlleFagsaker()
            .stream().collect(Collectors.groupingBy(SakInfotrygdMigrering::getFagsakId));

        var fagsakIder = grupperteMigreringer.keySet();

        var sakerMedFlerePerioderOgDeaktivertMigrering = fagsakIder.stream().filter(id -> behandlingRepository.hentSisteBehandlingForFagsakId(id)
                .map(b -> {
                    var alleSkjæringstidspunkt = finnAlleSkjæringstidspunkterForBehandling(b);
                    boolean harDeaktivertMigreringForStp = harDeaktivertMigreringForSkjæringstidspunkt(grupperteMigreringer, id, alleSkjæringstidspunkt);
                    return harDeaktivertMigreringForStp && alleSkjæringstidspunkt.size() > 1;
                }).orElse(false))
            .collect(Collectors.toSet());

        var saksnummer = sakerMedFlerePerioderOgDeaktivertMigrering.stream().map(fagsakRepository::finnEksaktFagsak)
            .map(Fagsak::getSaksnummer)
            .map(SaksnummerDto::new)
            .collect(Collectors.toList());
        return Response.ok(saksnummer).build();
    }

    @GET
    @Path("/feilbehandletMigrering")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent saker som feilaktig har blitt behandlet som ikke migrert fra infotrygd", tags = "infotrygdmigrering", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer saker som feilaktig har blitt behandlet som ikke migrert fra infotrygd",
            content = @Content(array = @ArraySchema(uniqueItems = true, arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = SaksnummerDto.class)), mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = READ, resource = DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response feilbehandledeMigreringer() { // NOSONAR
        var grupperteMigreringer = fagsakRepository.hentAlleSakInfotrygdMigreringerForAlleFagsaker()
            .stream().collect(Collectors.groupingBy(SakInfotrygdMigrering::getFagsakId));

        var fagsakIder = grupperteMigreringer.keySet();

        var sakerMedFjernetMigreringUtenOverstyrInntekt = fagsakIder.stream().filter(id -> behandlingRepository.hentSisteBehandlingForFagsakId(id)
                .map(b -> {
                    var alleSkjæringstidspunkt = finnAlleSkjæringstidspunkterForBehandling(b);
                    boolean harDeaktivertMigreringForStp = harDeaktivertMigreringForSkjæringstidspunkt(grupperteMigreringer, id, alleSkjæringstidspunkt);
                    var harAvbruttOverstyrInntekt = harAvbruttOverstyrInntektAksjonspunkt(b);
                    return harAvbruttOverstyrInntekt && harDeaktivertMigreringForStp && alleSkjæringstidspunkt.size() > 1;
                }).orElse(false))
            .collect(Collectors.toSet());

        var saksnummer = sakerMedFjernetMigreringUtenOverstyrInntekt.stream().map(fagsakRepository::finnEksaktFagsak)
            .map(Fagsak::getSaksnummer)
            .map(SaksnummerDto::new)
            .collect(Collectors.toList());
        return Response.ok(saksnummer).build();
    }

    private boolean harDeaktivertMigreringForSkjæringstidspunkt(Map<Long, List<SakInfotrygdMigrering>> grupperteMigreringer, Long id, Set<LocalDate> alleSkjæringstidspunkt) {
        var migreringer = grupperteMigreringer.get(id);
        return migreringer.stream().anyMatch(m -> !m.getAktiv() && alleSkjæringstidspunkt.stream().anyMatch(stp -> stp.equals(m.getSkjæringstidspunkt())));
    }

    private boolean harAvbruttOverstyrInntektAksjonspunkt(Behandling b) {
        return b.getAksjonspunkter().stream().anyMatch(a -> a.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.OVERSTYR_BEREGNING_INPUT) && a.getStatus().equals(AksjonspunktStatus.AVBRUTT));
    }

    private Set<LocalDate> finnAlleSkjæringstidspunkterForBehandling(Behandling behandling) {
        return vilkårResultatRepository.hentHvisEksisterer(behandling.getId()).flatMap(it -> it.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)).stream()
            .flatMap(v -> v.getPerioder().stream())
            .map(VilkårPeriode::getSkjæringstidspunkt)
            .collect(Collectors.toSet());
    }


}
