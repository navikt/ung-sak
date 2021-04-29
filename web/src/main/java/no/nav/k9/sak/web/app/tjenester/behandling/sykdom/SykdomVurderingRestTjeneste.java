package no.nav.k9.sak.web.app.tjenester.behandling.sykdom;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.sykdom.SykdomPeriodeMedEndringDto;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingDto;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingEndringDto;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingEndringResultatDto;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingIdDto;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingOpprettelseDto;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingOversikt;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.web.app.tjenester.behandling.sykdom.SykdomVurderingMapper.Sporingsinformasjon;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokumentRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomPeriodeMedEndring;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomPerson;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurdering;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingService;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingService.SykdomVurderingerOgPerioder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingVersjon;
import no.nav.k9.felles.sikkerhet.abac.AbacDataAttributter;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sikkerhet.context.SubjectHandler;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path(SykdomVurderingRestTjeneste.BASE_PATH)
@Transactional
public class SykdomVurderingRestTjeneste {

    public static final String BASE_PATH = "/behandling/sykdom/vurdering";
    public static final String VURDERING = "/";
    public static final String VURDERING_VERSJON = "/versjon";
    public static final String VURDERING_PATH = BASE_PATH;
    public static final String VURDERING_VERSJON_PATH = BASE_PATH + VURDERING_VERSJON;
    private static final String VURDERING_OVERSIKT_KTP = "/oversikt/KONTINUERLIG_TILSYN_OG_PLEIE";
    private static final String VURDERING_OVERSIKT_TOO = "/oversikt/TO_OMSORGSPERSONER";
    public static final String VURDERING_OVERSIKT_KTP_PATH = BASE_PATH + VURDERING_OVERSIKT_KTP;
    public static final String VURDERING_OVERSIKT_TOO_PATH = BASE_PATH + VURDERING_OVERSIKT_TOO;

    private BehandlingRepository behandlingRepository;
    private SykdomVurderingOversiktMapper sykdomVurderingOversiktMapper = new SykdomVurderingOversiktMapper();
    private SykdomVurderingMapper sykdomVurderingMapper = new SykdomVurderingMapper();
    private SykdomVurderingRepository sykdomVurderingRepository;
    private SykdomDokumentRepository sykdomDokumentRepository;
    private SykdomVurderingService sykdomVurderingService;
    private BasisPersonopplysningTjeneste personopplysningTjeneste;


    public SykdomVurderingRestTjeneste() {
    }


    @Inject
    public SykdomVurderingRestTjeneste(BehandlingRepository behandlingRepository, SykdomVurderingRepository sykdomVurderingRepository,
            SykdomDokumentRepository sykdomDokumentRepository, SykdomVurderingService sykdomVurderingService, BasisPersonopplysningTjeneste personopplysningTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.sykdomVurderingRepository = sykdomVurderingRepository;
        this.sykdomDokumentRepository = sykdomDokumentRepository;
        this.sykdomVurderingService = sykdomVurderingService;
        this.personopplysningTjeneste = personopplysningTjeneste;
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

        final var behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingUuid.getBehandlingUuid()).orElseThrow();
        final SykdomVurderingerOgPerioder sykdomUtlededePerioder = sykdomVurderingService.hentVurderingerForKontinuerligTilsynOgPleie(behandling);
        final LocalDate pleietrengendesFødselsdato = finnPleietrengendesFødselsdato(behandling);

        return sykdomVurderingOversiktMapper.map(behandling.getUuid(), behandling.getFagsak().getSaksnummer(), sykdomUtlededePerioder, pleietrengendesFødselsdato);
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

        final var behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingUuid.getBehandlingUuid()).orElseThrow();
        final SykdomVurderingerOgPerioder sykdomUtlededePerioder = sykdomVurderingService.hentVurderingerForToOmsorgspersoner(behandling);
        final LocalDate pleietrengendesFødselsdato = finnPleietrengendesFødselsdato(behandling);
        
        return sykdomVurderingOversiktMapper.map(behandling.getUuid(), behandling.getFagsak().getSaksnummer(), sykdomUtlededePerioder, pleietrengendesFødselsdato);
    }

    @GET
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
        final AktørId pleietrengende = behandling.getFagsak().getPleietrengendeAktørId();

        final List<SykdomDokument> alleDokumenter = sykdomDokumentRepository.hentAlleDokumenterFor(pleietrengende);

        final List<SykdomVurderingVersjon> versjoner;
        if (behandling.getStatus().erFerdigbehandletStatus() || behandling.getStatus().equals(BehandlingStatus.FATTER_VEDTAK)) {
            versjoner = sykdomVurderingRepository.hentVurderingMedVersjonerForBehandling(behandling.getUuid(), Long.valueOf(vurderingId.getSykdomVurderingId()));
        } else {
            versjoner = sykdomVurderingRepository.hentVurdering(pleietrengende, Long.valueOf(vurderingId.getSykdomVurderingId()))
                    .get()
                    .getSykdomVurderingVersjoner();
        }

        // TODO: Bedre løsning:
        final SykdomVurderingerOgPerioder sykdomUtlededePerioder;
        if (versjoner.get(0).getSykdomVurdering().getType() == SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE) {
            sykdomUtlededePerioder = sykdomVurderingService.hentVurderingerForKontinuerligTilsynOgPleie(behandling);
        } else {
            sykdomUtlededePerioder = sykdomVurderingService.hentVurderingerForToOmsorgspersoner(behandling);
        }

        return sykdomVurderingMapper.map(behandling.getFagsak().getAktørId(), behandling.getUuid(), versjoner, alleDokumenter, sykdomUtlededePerioder);
    }

    @POST
    @Path(VURDERING_VERSJON)
    @Consumes(MediaType.APPLICATION_JSON)
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
        if (behandling.getStatus().erFerdigbehandletStatus() || behandling.getStatus().equals(BehandlingStatus.FATTER_VEDTAK)) {
            throw new IllegalStateException("Behandlingen er ikke åpen for endringer.");
        }
        
        if (sykdomVurderingOppdatering.getTilknyttedeDokumenter().isEmpty()) {
            throw new IllegalStateException("En vurdering må minimum ha ett dokument tilknyttet.");
        }
        
        sikreAtOppdateringIkkeKrysser18årsdag(behandling, sykdomVurderingOppdatering.getPerioder());

        final var sporingsinformasjon = lagSporingsinformasjon(behandling);
        final SykdomVurdering sykdomVurdering = sykdomVurderingRepository.hentVurdering(behandling.getFagsak().getPleietrengendeAktørId(), Long.parseLong(sykdomVurderingOppdatering.getId())).orElseThrow();
        final List<SykdomDokument> alleDokumenter = sykdomDokumentRepository.hentAlleDokumenterFor(behandling.getFagsak().getPleietrengendeAktørId());
        final SykdomVurderingVersjon nyVersjon = sykdomVurderingMapper.map(sykdomVurdering, sykdomVurderingOppdatering, sporingsinformasjon, alleDokumenter);

        final List<SykdomPeriodeMedEndring> endringer = finnEndringer(behandling, nyVersjon);
        if (!sykdomVurderingOppdatering.isDryRun()) {
            sykdomVurderingRepository.lagre(nyVersjon);
            fjernOverlappendePerioderFraOverskyggendeVurderinger(endringer, sporingsinformasjon, nyVersjon.getEndretTidspunkt());
        }

        return toSykdomVurderingEndringResultatDto(endringer);
    }

    private void sikreAtOppdateringIkkeKrysser18årsdag(Behandling behandling, List<Periode> perioder) {
        final LocalDate pleietrengendesFødselsdato = finnPleietrengendesFødselsdato(behandling);
        if (isPerioderInneholderFørOgEtter18år(perioder, pleietrengendesFødselsdato)) {
            throw new IllegalStateException("En sykdomsvurdering kan ikke gjelde både før og etter at barnet har fylt 18 år. For å kunne lagre må vurderingen splittes i to.");
        }
    }

    static boolean isPerioderInneholderFørOgEtter18år(List<Periode> perioder, final LocalDate pleietrengendesFødselsdato) {
        final LocalDate blir18år = pleietrengendesFødselsdato.plusYears(18);
        final boolean vurderingUnder18år = perioder.stream().anyMatch(p -> p.getFom().isBefore(blir18år));
        final boolean vurdering18år = perioder.stream().anyMatch(p -> p.getTom().isAfter(blir18år) || p.getTom().isEqual(blir18år));
        boolean perioderInneholderFørOgEtter18år = vurderingUnder18år && vurdering18år;
        return perioderInneholderFørOgEtter18år;
    }

    private Sporingsinformasjon lagSporingsinformasjon(final Behandling behandling) {
        final SykdomPerson endretForPerson = sykdomVurderingRepository.hentEllerLagrePerson(behandling.getAktørId());
        return new SykdomVurderingMapper.Sporingsinformasjon(getCurrentUserId(), behandling.getUuid(), behandling.getFagsak().getSaksnummer().getVerdi(), endretForPerson);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
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
        if (behandling.getStatus().erFerdigbehandletStatus() || behandling.getStatus().equals(BehandlingStatus.FATTER_VEDTAK)) {
            throw new IllegalStateException("Behandlingen er ikke åpen for endringer.");
        }
        
        if (sykdomVurderingOpprettelse.getTilknyttedeDokumenter().isEmpty()) {
            throw new IllegalStateException("En vurdering må minimum ha ett dokument tilknyttet.");
        }
        
        sikreAtOppdateringIkkeKrysser18årsdag(behandling, sykdomVurderingOpprettelse.getPerioder());

        final var sporingsinformasjon = lagSporingsinformasjon(behandling);
        final List<SykdomDokument> alleDokumenter = sykdomDokumentRepository.hentAlleDokumenterFor(behandling.getFagsak().getPleietrengendeAktørId());
        final SykdomVurdering nyVurdering = sykdomVurderingMapper.map(sykdomVurderingOpprettelse, sporingsinformasjon, alleDokumenter);
        final List<SykdomPeriodeMedEndring> endringer = finnEndringer(behandling, nyVurdering.getSisteVersjon());
        if (!sykdomVurderingOpprettelse.isDryRun()) {
            sykdomVurderingRepository.lagre(nyVurdering, behandling.getFagsak().getPleietrengendeAktørId());
            fjernOverlappendePerioderFraOverskyggendeVurderinger(endringer, sporingsinformasjon, nyVurdering.getOpprettetTidspunkt());
        }

        return toSykdomVurderingEndringResultatDto(endringer);
    }
    
    private LocalDate finnPleietrengendesFødselsdato(Behandling behandling) {
        final var personopplysningerAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonPåTidspunkt(
            behandling.getId(),
            behandling.getFagsak().getAktørId(),
            behandling.getFagsak().getPeriode().getFomDato()
        );
        var pleietrengendePersonopplysning = personopplysningerAggregat.getPersonopplysning(behandling.getFagsak().getPleietrengendeAktørId());
        return pleietrengendePersonopplysning.getFødselsdato();
    }

    void fjernOverlappendePerioderFraOverskyggendeVurderinger(List<SykdomPeriodeMedEndring> endringer, Sporingsinformasjon sporing, LocalDateTime opprettetTidspunkt) {
        Map<SykdomVurderingVersjon, List<Periode>> perioderSomSkalFjernesFraVurdering = finnPerioderSomSkalFjernesPerVurdering(endringer);

        for (Map.Entry<SykdomVurderingVersjon, List<Periode>> vurderingPerioder : perioderSomSkalFjernesFraVurdering.entrySet()) {
            SykdomVurderingVersjon vurdering = vurderingPerioder.getKey();

            LocalDateTimeline<Boolean> tidslinjeSomSkalTrekkesFra = SykdomUtils.toLocalDateTimeline(vurderingPerioder.getValue());

            LocalDateTimeline<Boolean> gammelTidslinje = SykdomUtils.toLocalDateTimeline(
                vurdering.getPerioder().stream().map(p -> new Periode(p.getFom(), p.getTom())).collect(Collectors.toList())
            );

            LocalDateTimeline<Boolean> nyePerioder = SykdomUtils.kunPerioderSomIkkeFinnesI(gammelTidslinje, tidslinjeSomSkalTrekkesFra);

            List<Periode> vurderingPerioderTilLagring = SykdomUtils.toPeriodeList(nyePerioder);

            SykdomVurderingVersjon tilLagring = new SykdomVurderingVersjon(
                vurdering.getSykdomVurdering(),
                vurdering.getTekst(),
                vurdering.getResultat(),
                vurdering.getVersjon()+1,
                sporing.getEndretAv(),
                opprettetTidspunkt,
                sporing.getEndretBehandlingUuid(),
                sporing.getEndretSaksnummer(),
                sporing.getEndretForPerson(),
                null,
                vurdering.getDokumenter(),
                vurderingPerioderTilLagring);

            sykdomVurderingRepository.lagre(tilLagring);
        }
    }

    private HashMap<SykdomVurderingVersjon, List<Periode>> finnPerioderSomSkalFjernesPerVurdering(List<SykdomPeriodeMedEndring> endringer) {
        HashMap<SykdomVurderingVersjon, List<Periode>> perioderSomSkalFjernesFraVurdering = new HashMap<>();
        endringer.stream().filter(s -> s.isEndrerVurderingSammeBehandling()).forEach(v -> {
            var liste = perioderSomSkalFjernesFraVurdering.get(v.getGammelVersjon());
            if (liste == null) {
                liste = new ArrayList<>();
                perioderSomSkalFjernesFraVurdering.put(v.getGammelVersjon(), liste);
            }
            liste.add(v.getPeriode());
        });
        return perioderSomSkalFjernesFraVurdering;
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
        var vurderinger = sykdomVurderingService.hentVurderinger(nyEndring.getSykdomVurdering().getType(), behandling);
        return sykdomVurderingRepository.finnEndringer(vurderinger, nyEndring);
    }

    private static SykdomPeriodeMedEndringDto toSykdomPeriodeMedEndringDto(SykdomPeriodeMedEndring p) {
        return new SykdomPeriodeMedEndringDto(p.getPeriode(), p.isEndrerVurderingSammeBehandling(), p.isEndrerAnnenVurdering());
    }

    private static SykdomVurderingEndringResultatDto toSykdomVurderingEndringResultatDto(List<SykdomPeriodeMedEndring> perioderMedEndringer) {
        return new SykdomVurderingEndringResultatDto(perioderMedEndringer.stream().map(p -> toSykdomPeriodeMedEndringDto(p)).collect(Collectors.toList()));
    }
}
