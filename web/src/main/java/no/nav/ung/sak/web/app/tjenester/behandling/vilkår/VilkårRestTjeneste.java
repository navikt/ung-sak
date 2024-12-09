package no.nav.ung.sak.web.app.tjenester.behandling.vilkår;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import jakarta.ws.rs.core.MediaType;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.inngangsvilkår.VilkårUtleder;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.kontrakt.vilkår.VilkårMedPerioderDto;
import no.nav.ung.sak.kontrakt.vilkår.VilkårUtfallSamlet;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.ung.sak.web.server.caching.CacheControl;

import java.util.*;
import java.util.stream.Collectors;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.ung.abac.BeskyttetRessursKoder.FAGSAK;

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

    public VilkårRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public VilkårRestTjeneste(BehandlingRepository behandlingRepository,
                              VilkårTjeneste vilkårTjeneste,
                              @Any Instance<VilkårUtleder> vilkårUtledere,
                              @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårTjeneste = vilkårTjeneste;
        this.vilkårUtledere = vilkårUtledere;
        this.vilkårsPerioderTilVurderingTjenester = vilkårsPerioderTilVurderingTjenester;
    }

    @GET
    @Path(V3_PATH)
    @Operation(description = "Hent informasjon om vilkår for en behandling", tags = "vilkår", responses = {
        @ApiResponse(description = "Returnerer vilkår på behandling, tom liste hvis ikke eksisterer")
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    @CacheControl()
    public List<VilkårMedPerioderDto> getVilkårV3(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        return getVilkårV3(behandlingUuid, false);
    }

    @GET
    @Path(FULL_V3_PATH)
    @Operation(description = "Forvaltning : Hent informasjon om vilkår for en behandling", summary = ("Returnerer info om vilkår, inkludert hvordan eventuelt kjørt (relevanteBarnPersoninformasjon og evaluering)."), tags = {"vilkår", "forvaltning"}, responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer vilkår på behandling, tom liste hvis ikke eksisterer")
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    @CacheControl()
    public List<VilkårMedPerioderDto> getVilkårFullV3(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        return getVilkårV3(behandlingUuid, true);
    }

    private List<VilkårMedPerioderDto> getVilkårV3(BehandlingUuidDto behandlingUuid, boolean inkluderVilkårkjøring) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        var vilkåreneOpt = vilkårTjeneste.hentHvisEksisterer(behandling.getId());
        var dto = vilkåreneOpt.map(vilkårene -> {
            var vilkårPeriodeMap = utledFaktiskeVilkårPerioder(behandling, vilkårene);
            return VilkårDtoMapper.lagVilkarMedPeriodeDto(behandling, inkluderVilkårkjøring, vilkårene, vilkårPeriodeMap);
        }).orElse(Collections.emptyList());
        return dto;
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
        return getVilkårUtleder(behandling).utledVilkår(BehandlingReferanse.fra(behandling)).getAlleAvklarte();
    }

    private Set<VilkårType> finnVilkårTyperPåPåBehandlingen(Vilkårene vilkårene) {
        return vilkårene.getVilkårene().stream().map(Vilkår::getVilkårType).collect(Collectors.toSet());
    }

    private NavigableSet<DatoIntervallEntitet> utledPeriodeTilVurdering(Behandling behandling, VilkårType vilkårType) {
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
        @ApiResponse(description = "Returnerer vilkår på behandling, tom liste hvis ikke eksisterer")
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    @CacheControl()
    public VilkårResultatContainer getVilkårSamlet(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        var samletVilkårResultat = vilkårTjeneste.samletVilkårsresultat(behandling.getId());

        return new VilkårResultatContainer(samletVilkårResultat);
    }

    @Schema
    public static class VilkårResultatContainer {


        @JsonProperty(value = "vilkårTidslinje")
        @Valid
        private LocalDateTimeline<VilkårUtfallSamlet> vilkårTidslinje;

        public VilkårResultatContainer(LocalDateTimeline<VilkårUtfallSamlet> vilkårTidslinje) {
            this.vilkårTidslinje = vilkårTidslinje;
        }

    }
}
