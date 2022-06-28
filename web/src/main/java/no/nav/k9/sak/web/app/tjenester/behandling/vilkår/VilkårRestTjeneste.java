package no.nav.k9.sak.web.app.tjenester.behandling.vilkår;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.vilkår.VilkårDto;
import no.nav.k9.sak.kontrakt.vilkår.VilkårMedPerioderDto;
import no.nav.k9.sak.kontrakt.vilkår.VilkårUtfallSamlet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.vilkår.VilkårTjeneste;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class VilkårRestTjeneste {

    public static final String V3_PATH = "/behandling/vilkar-v3";
    public static final String VILKÅR_SAMLET_PATH = "/behandling/vilkar/samlet";
    public static final String FULL_V3_PATH = "/behandling/vilkar/full-v3";

    private BehandlingRepository behandlingRepository;
    private VilkårTjeneste vilkårTjeneste;
    private Instance<VilkårUtleder> vilkårUtledere;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;

    public VilkårRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public VilkårRestTjeneste(BehandlingRepository behandlingRepository,
                              VilkårTjeneste vilkårTjeneste,
                              @Any Instance<VilkårUtleder> vilkårUtledere,
                              @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester,
                              BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårTjeneste = vilkårTjeneste;
        this.vilkårUtledere = vilkårUtledere;
        this.vilkårsPerioderTilVurderingTjenester = vilkårsPerioderTilVurderingTjenester;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
    }

    @GET
    @Path(V3_PATH)
    @Operation(description = "Hent informasjon om vilkår for en behandling", tags = "vilkår", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer vilkår på behandling, tom liste hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = VilkårDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getVilkårV3(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        return getVilkårV3(behandlingUuid, false);
    }

    @GET
    @Path(FULL_V3_PATH)
    @Operation(description = "Forvaltning : Hent informasjon om vilkår for en behandling", summary = ("Returnerer info om vilkår, inkludert hvordan eventuelt kjørt (input og evaluering)."), tags = {"vilkår", "forvaltning"}, responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer vilkår på behandling, tom liste hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = VilkårMedPerioderDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getVilkårFullV3(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        return getVilkårV3(behandlingUuid, true);
    }

    private Response getVilkårV3(BehandlingUuidDto behandlingUuid, boolean inkluderVilkårkjøring) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        var vilkårene = vilkårTjeneste.hentHvisEksisterer(behandling.getId()).orElse(null);
        var vilkårPeriodeMap = utledFaktiskeVilkårPerioder(behandling, vilkårene);

        var dto = VilkårDtoMapper.lagVilkarMedPeriodeDto(behandling, inkluderVilkårkjøring, vilkårene, vilkårPeriodeMap);
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok(dto).cacheControl(cc).build();
    }

    private Map<VilkårType, Set<DatoIntervallEntitet>> utledFaktiskeVilkårPerioder(Behandling behandling, Vilkårene vilkårene) {
        Set<VilkårType> aktuelleVilkårTyper = finnAktuelleVilkårTyper(behandling, vilkårene);
        Map<VilkårType, Set<DatoIntervallEntitet>> resultat = new EnumMap<>(VilkårType.class);
        for (VilkårType vilkårType : aktuelleVilkårTyper) {
            resultat.put(vilkårType, utledPeriodeTilVurdering(behandling, vilkårType));
        }
        return resultat;
    }

    private Set<VilkårType> finnAktuelleVilkårTyper(Behandling behandling, Vilkårene vilkårene) {
        if (behandling.erAvsluttet()) {
            return finnVilkårTyperPåPåBehandlingen(vilkårene);
        } else {
            Set<VilkårType> vilkår = EnumSet.noneOf(VilkårType.class);
            vilkår.addAll(utledVilkårTyperForBehandlingen(behandling));
            vilkår.addAll(finnVilkårTyperPåPåBehandlingen(vilkårene));
            return vilkår;
        }
    }

    private Set<VilkårType> utledVilkårTyperForBehandlingen(Behandling behandling) {
        return getVilkårUtleder(behandling).utledVilkår(behandling).getAlleAvklarte();
    }

    private Set<VilkårType> finnVilkårTyperPåPåBehandlingen(Vilkårene vilkårene) {
        return vilkårene.getVilkårene().stream().map(Vilkår::getVilkårType).collect(Collectors.toSet());
    }

    private NavigableSet<DatoIntervallEntitet> utledPeriodeTilVurdering(Behandling behandling, VilkårType vilkårType) {
        if (vilkårType.equals(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)) {
            return beregningsgrunnlagVilkårTjeneste.utledPerioderTilVurdering(BehandlingReferanse.fra(behandling));
        }
        return getPerioderTilVurderingTjeneste(behandling).utled(behandling.getId(), vilkårType);
    }

    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(Behandling behandling) {
        return BehandlingTypeRef.Lookup.find(VilkårsPerioderTilVurderingTjeneste.class, vilkårsPerioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType())
            .orElseThrow(() -> new UnsupportedOperationException("VilkårsPerioderTilVurderingTjeneste ikke implementert for ytelse [" + behandling.getFagsakYtelseType() + "], behandlingtype [" + behandling.getType() + "]"));
    }

    private VilkårUtleder getVilkårUtleder(Behandling behandling) {
        return BehandlingTypeRef.Lookup.find(VilkårUtleder.class, vilkårUtledere, behandling.getFagsakYtelseType(), behandling.getType())
            .orElseThrow(() -> new UnsupportedOperationException("VilkårUtleder ikke implementert for ytelse [" + behandling.getFagsakYtelseType() + "], behandlingtype [" + behandling.getType() + "]"));
    }

    @GET
    @Path(VILKÅR_SAMLET_PATH)
    @Operation(description = "Hent informasjon om vilkår samlet for en behandling", tags = "vilkår", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer vilkår på behandling, tom liste hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = VilkårResultatContainer.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getVilkårSamlet(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        var samletVilkårResultat = vilkårTjeneste.samletVilkårsresultat(behandling.getId());

        var dto = new VilkårResultatContainer(samletVilkårResultat);
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok(dto).cacheControl(cc).build();
    }

    @Schema
    public static class VilkårResultatContainer {


        @JsonProperty(value = "vilkårTidslinje")
        private LocalDateTimeline<VilkårUtfallSamlet> vilkårTidslinje;

        public VilkårResultatContainer(LocalDateTimeline<VilkårUtfallSamlet> vilkårTidslinje) {
            this.vilkårTidslinje = vilkårTidslinje;
        }

    }
}
