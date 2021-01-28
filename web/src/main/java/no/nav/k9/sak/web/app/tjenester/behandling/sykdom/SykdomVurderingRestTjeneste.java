package no.nav.k9.sak.web.app.tjenester.behandling.sykdom;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;

import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.app.tjenester.behandling.sykdom.SykdomVurderingMapper.Sporingsinformasjon;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokumentRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomPeriodeMedEndring;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomPerson;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurdering;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingType;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingVersjon;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.context.SubjectHandler;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path(SykdomVurderingRestTjeneste.BASE_PATH)
@Transactional
public class SykdomVurderingRestTjeneste {

    public static final String BASE_PATH = "/behandling/sykdom/vurdering";
    public static final String VURDERING = "/";
    public static final String VURDERING_VERSJON = "/versjon";
    public static final String VURDERING_PATH = BASE_PATH + VURDERING;
    public static final String VURDERING_VERSJON_PATH = BASE_PATH + VURDERING_VERSJON;
    private static final String VURDERING_OVERSIKT_KTP = "/oversikt/KONTINUERLIG_TILSYN_OG_PLEIE";
    private static final String VURDERING_OVERSIKT_TOO = "/oversikt/TO_OMSORGSPERSONER";
    public static final String VURDERING_OVERSIKT_KTP_PATH = BASE_PATH + VURDERING_OVERSIKT_KTP;
    public static final String VURDERING_OVERSIKT_TOO_PATH = BASE_PATH + VURDERING_OVERSIKT_TOO;

    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;
    private BehandlingRepository behandlingRepository;
    private SykdomVurderingOversiktMapper sykdomVurderingOversiktMapper;
    private SykdomVurderingMapper sykdomVurderingMapper;
    private SykdomVurderingRepository sykdomVurderingRepository;
    private SykdomDokumentRepository sykdomDokumentRepository;


    public SykdomVurderingRestTjeneste() {
    }


    @Inject
    public SykdomVurderingRestTjeneste(@Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester,
            BehandlingRepository behandlingRepository, SykdomVurderingOversiktMapper sykdomVurderingOversiktMapper,
            SykdomVurderingMapper sykdomVurderingMapper, SykdomVurderingRepository sykdomVurderingRepository,
            SykdomDokumentRepository sykdomDokumentRepository) {
        this.vilkårsPerioderTilVurderingTjenester = vilkårsPerioderTilVurderingTjenester;
        this.behandlingRepository = behandlingRepository;
        this.sykdomVurderingOversiktMapper = sykdomVurderingOversiktMapper;
        this.sykdomVurderingMapper = sykdomVurderingMapper;
        this.sykdomVurderingRepository = sykdomVurderingRepository;
        this.sykdomDokumentRepository = sykdomDokumentRepository;
    }

    @GET
    @Path(VURDERING_OVERSIKT_KTP)
    @Operation(description = "En oversikt over sykdomsvurderinger for kontinuerlig tilsyn og pleie",
        summary = "En oversikt over sykdomsvurderinger for kontinuerlig tilsyn og pleie",
        tags = "sykdom",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = SykdomVurderingOversikt.class)))
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public SykdomVurderingOversikt hentSykdomsoversiktForKtp(
            @NotNull @QueryParam(BehandlingUuidDto.NAME)
            @Parameter(description = BehandlingUuidDto.DESC)
            @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
            BehandlingUuidDto behandlingUuid) {

        return hentSykdomsoversikt(behandlingUuid, SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE);
    }

    @GET
    @Path(VURDERING_OVERSIKT_TOO)
    @Operation(description = "En oversikt over sykdomsvurderinger for to omsorgspersoner",
        summary = "En oversikt over sykdomsvurderinger for to omsorgspersoner",
        tags = "sykdom",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = SykdomVurderingOversikt.class)))
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public SykdomVurderingOversikt hentSykdomsoversiktForToOmsorgspersoner(
            @NotNull @QueryParam(BehandlingUuidDto.NAME)
            @Parameter(description = BehandlingUuidDto.DESC)
            @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
            BehandlingUuidDto behandlingUuid) {

        return hentSykdomsoversikt(behandlingUuid, SykdomVurderingType.TO_OMSORGSPERSONER);
    }

    private SykdomVurderingOversikt hentSykdomsoversikt(BehandlingUuidDto behandlingUuid, SykdomVurderingType sykdomVurderingType) {
        final var behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingUuid.getBehandlingUuid()).orElseThrow();

        final LocalDateTimeline<SykdomVurderingVersjon> vurderinger = hentVurderinger(sykdomVurderingType, behandling);
        final LocalDateTimeline<HashSet<Saksnummer>> saksnummerForPerioder = sykdomVurderingRepository.hentSaksnummerForSøktePerioder(behandling.getFagsak().getPleietrengendeAktørId());

        return sykdomVurderingOversiktMapper.map(behandling.getUuid().toString(), vurderinger, saksnummerForPerioder);
    }

    private LocalDateTimeline<SykdomVurderingVersjon> hentVurderinger(SykdomVurderingType sykdomVurderingType, final Behandling behandling) {
        if (behandling.getStatus().erFerdigbehandletStatus()) {
            return sykdomVurderingRepository.getVurderingstidslinjeFor(sykdomVurderingType, behandling.getUuid());
        } else {
            return sykdomVurderingRepository.getSisteVurderingstidslinjeFor(sykdomVurderingType, behandling.getFagsak().getPleietrengendeAktørId());
        }
    }

    @GET
    @Path(VURDERING)
    @Operation(description = "Henter informasjon om én gitt vurdering.",
        summary = "En gitt vurdering angitt med ID.",
        tags = "sykdom",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = SykdomVurderingDto.class)))
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public SykdomVurderingDto hentSykdomsInformasjonFor(
            @QueryParam(BehandlingUuidDto.NAME)
            @Parameter(description = BehandlingUuidDto.DESC)
            @NotNull
            @Valid
            @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
            BehandlingUuidDto behandlingUuid,

            @QueryParam(SykdomVurderingIdDto.NAME)
            @Parameter(description = SykdomVurderingIdDto.DESC)
            @NotNull
            @Valid
            @TilpassetAbacAttributt(supplierClass = AbacDataSupplier.class)
            SykdomVurderingIdDto vurderingId) {
        final var behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingUuid.getBehandlingUuid()).orElseThrow();

        final List<SykdomVurderingVersjon> versjoner;
        if (behandling.getStatus().erFerdigbehandletStatus()) {
            versjoner = sykdomVurderingRepository.hentVurderingMedVersjonerForBehandling(behandling.getUuid(), Long.valueOf(vurderingId.getSykdomVurderingId()));
        } else {
            versjoner = sykdomVurderingRepository.hentVurdering(behandling.getFagsak().getPleietrengendeAktørId(), Long.valueOf(vurderingId.getSykdomVurderingId()))
                    .get()
                    .getSykdomVurderingVersjoner();
        }

        return sykdomVurderingMapper.map(versjoner);
    }

    @POST
    @Path(VURDERING_VERSJON)
    @Operation(description = "Oppdaterer en vurdering.",
        summary = "Oppdaterer en vurdering.",
        tags = "sykdom",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "Et resultatobjekt som viser vurderinger som blir erstattet.",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = SykdomVurderingEndringResultatDto.class)))
        })
    @BeskyttetRessurs(action = UPDATE, resource = FAGSAK)
    public SykdomVurderingEndringResultatDto oppdaterSykdomsVurdering(
            @Parameter
            @NotNull
            @Valid
            @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
            SykdomVurderingEndringDto sykdomVurderingOppdatering) {
        final var behandling = behandlingRepository.hentBehandlingHvisFinnes(sykdomVurderingOppdatering.getBehandlingUuid()).orElseThrow();
        if (behandling.getStatus().erFerdigbehandletStatus()) {
            throw new IllegalStateException("Behandlingen er ikke åpen for endringer.");
        }

        final var sporingsinformasjon = lagSporingsinformasjon(behandling);
        final SykdomVurdering sykdomVurdering = sykdomVurderingRepository.hentVurdering(behandling.getFagsak().getPleietrengendeAktørId(), Long.parseLong(sykdomVurderingOppdatering.getId())).orElseThrow();
        final List<SykdomDokument> alleDokumenter = sykdomDokumentRepository.hentAlleDokumenterFor(behandling.getFagsak().getPleietrengendeAktørId());
        final SykdomVurderingVersjon nyVersjon = sykdomVurderingMapper.map(sykdomVurdering, sykdomVurderingOppdatering, sporingsinformasjon, alleDokumenter);

        final List<SykdomPeriodeMedEndring> endringer = finnEndringer(behandling, nyVersjon);
        if (!sykdomVurderingOppdatering.isDryRun()) {
            sykdomVurderingRepository.lagre(nyVersjon);
            endringer.stream().filter(s -> s.isEndrerVurderingSammeBehandling()).forEach(v -> {
                // TODO: Fjern v.getPeriode() fra v.getGammelVersjon()
                throw new IllegalStateException("Det støttes ikke å erstatte vurderingsperioder som har blitt lagt inn i samme behandling. Fjern manuelt perioden fra den gamle vurderingen.");
            });
        }

        return toSykdomVurderingEndringResultatDto(endringer);
    }

    private Sporingsinformasjon lagSporingsinformasjon(final Behandling behandling) {
        final SykdomPerson endretForPerson = sykdomVurderingRepository.hentEllerLagrePerson(behandling.getAktørId());
        return new SykdomVurderingMapper.Sporingsinformasjon(getCurrentUserId(), behandling.getUuid(), behandling.getFagsak().getSaksnummer().getVerdi(), endretForPerson);
    }

    @POST
    @Path(VURDERING)
    @Operation(description = "Oppretter en ny vurdering.",
        summary = "Oppretter en ny vurdering.",
        tags = "sykdom",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "Et resultatobjekt som viser vurderinger som blir erstattet.",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = SykdomVurderingEndringResultatDto.class)))
        })
    @BeskyttetRessurs(action = UPDATE, resource = FAGSAK)
    public SykdomVurderingEndringResultatDto opprettSykdomsVurdering(
            @Parameter
            @NotNull
            @Valid
            @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
            SykdomVurderingOpprettelseDto sykdomVurderingOpprettelse) {
        final var behandling = behandlingRepository.hentBehandlingHvisFinnes(sykdomVurderingOpprettelse.getBehandlingUuid()).orElseThrow();
        if (behandling.getStatus().erFerdigbehandletStatus()) {
            throw new IllegalStateException("Behandlingen er ikke åpen for endringer.");
        }

        final var sporingsinformasjon = lagSporingsinformasjon(behandling);
        final List<SykdomDokument> alleDokumenter = sykdomDokumentRepository.hentAlleDokumenterFor(behandling.getFagsak().getPleietrengendeAktørId());
        final SykdomVurdering nyVurdering = sykdomVurderingMapper.map(sykdomVurderingOpprettelse, sporingsinformasjon, alleDokumenter);
       
        final List<SykdomPeriodeMedEndring> endringer = finnEndringer(behandling, nyVurdering.getSisteVersjon());
        if (!sykdomVurderingOpprettelse.isDryRun()) {
            sykdomVurderingRepository.lagre(nyVurdering, behandling.getFagsak().getPleietrengendeAktørId());
            endringer.stream().filter(s -> s.isEndrerVurderingSammeBehandling()).forEach(v -> {
                // TODO: Fjern v.getPeriode() fra v.getGammelVersjon()
                throw new IllegalStateException("Det støttes ikke å erstatte vurderingsperioder som har blitt lagt inn i samme behandling. Fjern manuelt perioden fra den gamle vurderingen.");
            });
        }

        return toSykdomVurderingEndringResultatDto(endringer);
    }

    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(Behandling behandling) {
        return BehandlingTypeRef.Lookup.find(VilkårsPerioderTilVurderingTjeneste.class, vilkårsPerioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType())
            .orElseThrow(() -> new UnsupportedOperationException("VilkårsPerioderTilVurderingTjeneste ikke implementert for ytelse [" + behandling.getFagsakYtelseType() + "], behandlingtype [" + behandling.getType() + "]"));
    }

    public static class AbacDataSupplier implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett();
        }
    }

    private static String getCurrentUserId() {
        return SubjectHandler.getSubjectHandler().getUid();
    }
    
    private List<SykdomPeriodeMedEndring> finnEndringer(Behandling behandling, SykdomVurderingVersjon nyEndring) {
        var vurderinger = hentVurderinger(nyEndring.getSykdomVurdering().getType(), behandling);
        return sykdomVurderingRepository.finnEndringer(vurderinger, nyEndring);
    }
    
    public SykdomVurderingEndringResultatDto toSykdomVurderingEndringResultatDto(List<SykdomPeriodeMedEndring> perioderMedEndringer) {
        return new SykdomVurderingEndringResultatDto(perioderMedEndringer.stream().map(p -> new SykdomPeriodeMedEndringDto(p)).collect(Collectors.toList()));
    }
}
