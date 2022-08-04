package no.nav.k9.sak.web.app.tjenester.behandling.sykdom;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.sikkerhet.abac.AbacDataAttributter;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
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
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.PleietrengendeSykdomDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.PleietrengendeSykdomDokumentHarOppdatertVurderinger;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokumentRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomPeriodeMedEndring;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.Person;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.PleietrengendeSykdomVurdering;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingTjeneste.SykdomVurderingerOgPerioder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.PleietrengendeSykdomVurderingVersjon;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.PleietrengendeAlderPeriode;
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
    public static final String VURDERING_OVERSIKT_KTP_PATH = BASE_PATH + VURDERING_OVERSIKT_KTP;
    private static final String VURDERING_OVERSIKT_TOO = "/oversikt/TO_OMSORGSPERSONER";
    public static final String VURDERING_OVERSIKT_TOO_PATH = BASE_PATH + VURDERING_OVERSIKT_TOO;
    private static final String VURDERING_OVERSIKT_SLU = "/oversikt/I_LIVETS_SLUTT";
    public static final String VURDERING_OVERSIKT_SLU_PATH = BASE_PATH + VURDERING_OVERSIKT_SLU;

    private BehandlingRepository behandlingRepository;
    private SykdomVurderingOversiktMapper sykdomVurderingOversiktMapper = new SykdomVurderingOversiktMapper();
    private SykdomVurderingMapper sykdomVurderingMapper;
    private SykdomVurderingRepository sykdomVurderingRepository;
    private SykdomDokumentRepository sykdomDokumentRepository;
    private SykdomVurderingTjeneste sykdomVurderingTjeneste;

    private SykdomProsessDriver prosessDriver;

    public SykdomVurderingRestTjeneste() {
    }


    @Inject
    public SykdomVurderingRestTjeneste(BehandlingRepository behandlingRepository, SykdomVurderingRepository sykdomVurderingRepository,
                                       SykdomDokumentRepository sykdomDokumentRepository, SykdomVurderingTjeneste sykdomVurderingTjeneste,
                                       SykdomVurderingMapper sykdomVurderingMapper, SykdomProsessDriver prosessDriver) {
        this.behandlingRepository = behandlingRepository;
        this.sykdomVurderingRepository = sykdomVurderingRepository;
        this.sykdomDokumentRepository = sykdomDokumentRepository;
        this.sykdomVurderingTjeneste = sykdomVurderingTjeneste;
        this.sykdomVurderingMapper = sykdomVurderingMapper;
        this.prosessDriver = prosessDriver;
    }

    static boolean isPerioderInneholderFørOgEtter18år(List<Periode> perioder, final LocalDate pleietrengendesFødselsdato) {
        final LocalDate blir18år = pleietrengendesFødselsdato.plusYears(PleietrengendeAlderPeriode.ALDER_FOR_STRENGERE_PSB_VURDERING);
        final boolean vurderingUnder18år = perioder.stream().anyMatch(p -> p.getFom().isBefore(blir18år));
        final boolean vurdering18år = perioder.stream().anyMatch(p -> p.getTom().isAfter(blir18år) || p.getTom().isEqual(blir18år));
        boolean perioderInneholderFørOgEtter18år = vurderingUnder18år && vurdering18år;
        return perioderInneholderFørOgEtter18år;
    }

    private static String getCurrentUserId() {
        return SubjectHandler.getSubjectHandler().getUid();
    }

    private static SykdomPeriodeMedEndringDto toSykdomPeriodeMedEndringDto(SykdomPeriodeMedEndring p) {
        return new SykdomPeriodeMedEndringDto(p.getPeriode(), p.isEndrerVurderingSammeBehandling(), p.isEndrerAnnenVurdering());
    }

    private static SykdomVurderingEndringResultatDto toSykdomVurderingEndringResultatDto(List<SykdomPeriodeMedEndring> perioderMedEndringer) {
        return new SykdomVurderingEndringResultatDto(perioderMedEndringer.stream().map(p -> toSykdomPeriodeMedEndringDto(p)).collect(Collectors.toList()));
    }

    private static void validerYtelsetype(Behandling behandling, FagsakYtelseType fagsakYtelseType) {
        if (behandling.getFagsakYtelseType() != fagsakYtelseType) {
            throw new IllegalArgumentException("Tjenesten er ikke støttet for ytelsetype " + behandling.getFagsakYtelseType());
        }
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
        validerYtelsetype(behandling, FagsakYtelseType.PSB);
        SykdomVurderingerOgPerioder sykdomUtlededePerioder = sykdomVurderingTjeneste.hentVurderingerForKontinuerligTilsynOgPleie(behandling);
        final LocalDate pleietrengendesFødselsdato = sykdomVurderingTjeneste.finnPleietrengendesFødselsdato(behandling);

        return sykdomVurderingOversiktMapper.mapPSB(behandling.getUuid(), behandling.getFagsak().getSaksnummer(), sykdomUtlededePerioder, pleietrengendesFødselsdato);
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
        validerYtelsetype(behandling, FagsakYtelseType.PSB);
        final SykdomVurderingerOgPerioder sykdomUtlededePerioder = sykdomVurderingTjeneste.hentVurderingerForToOmsorgspersoner(behandling);
        final LocalDate pleietrengendesFødselsdato = sykdomVurderingTjeneste.finnPleietrengendesFødselsdato(behandling);

        return sykdomVurderingOversiktMapper.mapPSB(behandling.getUuid(), behandling.getFagsak().getSaksnummer(), sykdomUtlededePerioder, pleietrengendesFødselsdato);
    }

    @GET
    @Path(VURDERING_OVERSIKT_SLU)
    @Operation(description = "En oversikt over sykdomsvurderinger for i livets sluttfase",
        summary = "En oversikt over sykdomsvurderinger for i livets sluttfase",
        tags = "sykdom",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = SykdomVurderingOversikt.class)))
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public SykdomVurderingOversikt hentSykdomsoversiktForILivetsSluttase(
        @NotNull @QueryParam(BehandlingUuidDto.NAME)
        @Parameter(description = BehandlingUuidDto.DESC)
        @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
        BehandlingUuidDto behandlingUuid) {

        final var behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingUuid.getBehandlingUuid()).orElseThrow();
        validerYtelsetype(behandling, FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE);
        final SykdomVurderingerOgPerioder sykdomUtlededePerioder = sykdomVurderingTjeneste.hentVurderingerForILivetsSluttfase(behandling);

        return sykdomVurderingOversiktMapper.mapPPN(behandling.getUuid(), behandling.getFagsak().getSaksnummer(), sykdomUtlededePerioder);
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

        final List<PleietrengendeSykdomDokument> alleDokumenter = sykdomDokumentRepository.hentAlleDokumenterFor(pleietrengende);

        final List<PleietrengendeSykdomVurderingVersjon> versjoner;
        if (behandling.getStatus().erFerdigbehandletStatus() || behandling.getStatus().equals(BehandlingStatus.FATTER_VEDTAK)) {
            versjoner = sykdomVurderingRepository.hentVurderingMedVersjonerForBehandling(behandling.getUuid(), Long.valueOf(vurderingId.getSykdomVurderingId()));
        } else {
            versjoner = sykdomVurderingRepository.hentVurdering(pleietrengende, Long.valueOf(vurderingId.getSykdomVurderingId()))
                .get()
                .getSykdomVurderingVersjoner();
        }

        // TODO: Bedre løsning:
        var sykdomVurderingType = versjoner.get(0).getSykdomVurdering().getType();
        final SykdomVurderingerOgPerioder sykdomUtlededePerioder = switch (sykdomVurderingType) {
            case KONTINUERLIG_TILSYN_OG_PLEIE ->
                sykdomVurderingTjeneste.hentVurderingerForKontinuerligTilsynOgPleie(behandling);
            case TO_OMSORGSPERSONER -> sykdomVurderingTjeneste.hentVurderingerForToOmsorgspersoner(behandling);
            case LIVETS_SLUTTFASE -> sykdomVurderingTjeneste.hentVurderingerForILivetsSluttfase(behandling);
        };

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

        validerOppdatering(sykdomVurderingOppdatering, behandling);

        final var sporingsinformasjon = lagSporingsinformasjon(behandling);
        final PleietrengendeSykdomVurdering pleietrengendeSykdomVurdering = sykdomVurderingRepository.hentVurdering(behandling.getFagsak().getPleietrengendeAktørId(), Long.parseLong(sykdomVurderingOppdatering.getId())).orElseThrow();
        final List<PleietrengendeSykdomDokument> alleDokumenter = sykdomDokumentRepository.hentAlleDokumenterFor(behandling.getFagsak().getPleietrengendeAktørId());
        final PleietrengendeSykdomVurderingVersjon nyVersjon = sykdomVurderingMapper.map(pleietrengendeSykdomVurdering, sykdomVurderingOppdatering, sporingsinformasjon, alleDokumenter);

        final List<SykdomPeriodeMedEndring> endringer = finnEndringer(behandling, nyVersjon);
        if (!sykdomVurderingOppdatering.isDryRun()) {
            sykdomVurderingRepository.lagre(nyVersjon);
            fjernOverlappendePerioderFraOverskyggendeVurderinger(endringer, sporingsinformasjon, nyVersjon.getEndretTidspunkt());
        }

        return toSykdomVurderingEndringResultatDto(endringer);
    }

    private void validerOppdatering(SykdomVurderingEndringDto sykdomVurderingOppdatering, Behandling behandling) {
        if (behandling.getStatus().erFerdigbehandletStatus() || behandling.getStatus().equals(BehandlingStatus.FATTER_VEDTAK)) {
            throw new IllegalStateException("Behandlingen er ikke åpen for endringer.");
        }

        if (sykdomVurderingOppdatering.getTilknyttedeDokumenter().isEmpty()) {
            throw new IllegalStateException("En vurdering må minimum ha ett dokument tilknyttet.");
        }

        switch (behandling.getFagsakYtelseType()) {
            case PLEIEPENGER_SYKT_BARN ->
                sikreAtOppdateringIkkeKrysser18årsdag(behandling, sykdomVurderingOppdatering.getPerioder());
            case PLEIEPENGER_NÆRSTÅENDE -> ingenValidering();
            default -> throw new IllegalStateException("Ikke-støttet ytelsetype: " + behandling.getFagsakYtelseType());
        }

        prosessDriver.validerTilstand(behandling, sykdomVurderingOppdatering.isDryRun());
    }

    private void sikreAtOppdateringIkkeKrysser18årsdag(Behandling behandling, List<Periode> perioder) {
        final LocalDate pleietrengendesFødselsdato = sykdomVurderingTjeneste.finnPleietrengendesFødselsdato(behandling);
        if (isPerioderInneholderFørOgEtter18år(perioder, pleietrengendesFødselsdato)) {
            throw new IllegalStateException("En sykdomsvurdering kan ikke gjelde både før og etter at barnet har fylt 18 år. For å kunne lagre må vurderingen splittes i to.");
        }
    }

    private Sporingsinformasjon lagSporingsinformasjon(final Behandling behandling) {
        final Person endretForPerson = sykdomVurderingRepository.hentEllerLagrePerson(behandling.getAktørId());
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

        validerOpprettelse(behandling, sykdomVurderingOpprettelse);

        final var sporingsinformasjon = lagSporingsinformasjon(behandling);
        final List<PleietrengendeSykdomDokument> alleDokumenter = sykdomDokumentRepository.hentAlleDokumenterFor(behandling.getFagsak().getPleietrengendeAktørId());
        final PleietrengendeSykdomVurdering nyVurdering = sykdomVurderingMapper.map(sykdomVurderingOpprettelse, sporingsinformasjon, alleDokumenter);
        final List<SykdomPeriodeMedEndring> endringer = finnEndringer(behandling, nyVurdering.getSisteVersjon());
        if (!sykdomVurderingOpprettelse.isDryRun()) {

            Collection<PleietrengendeSykdomVurderingVersjon> eksisterendeVurderinger = sykdomVurderingRepository.hentSisteVurderingerFor(nyVurdering.getType(), behandling.getFagsak().getPleietrengendeAktørId());
            if (eksisterendeVurderinger.isEmpty()) {
                var nå = LocalDateTime.now();
                List<PleietrengendeSykdomDokument> dokumenter = sykdomDokumentRepository.hentDokumentSomIkkeHarOppdatertEksisterendeVurderinger(behandling.getFagsak().getPleietrengendeAktørId());
                for (PleietrengendeSykdomDokument dokument : dokumenter) {
                    sykdomDokumentRepository.kvitterDokumenterMedOppdatertEksisterendeVurderinger(new PleietrengendeSykdomDokumentHarOppdatertVurderinger(dokument, getCurrentUserId(), nå));
                }
            }

            sykdomVurderingRepository.lagre(nyVurdering, behandling.getFagsak().getPleietrengendeAktørId());
            fjernOverlappendePerioderFraOverskyggendeVurderinger(endringer, sporingsinformasjon, nyVurdering.getOpprettetTidspunkt());
        }

        return toSykdomVurderingEndringResultatDto(endringer);
    }

    private void validerOpprettelse(Behandling behandling, SykdomVurderingOpprettelseDto sykdomVurderingOpprettelse) {
        if (behandling.getStatus().erFerdigbehandletStatus() || behandling.getStatus().equals(BehandlingStatus.FATTER_VEDTAK)) {
            throw new IllegalStateException("Behandlingen er ikke åpen for endringer.");
        }

        if (sykdomVurderingOpprettelse.getTilknyttedeDokumenter().isEmpty()) {
            throw new IllegalStateException("En vurdering må minimum ha ett dokument tilknyttet.");
        }

        FagsakYtelseType fagsakYtelseType = behandling.getFagsakYtelseType();
        switch (fagsakYtelseType) {
            case PLEIEPENGER_SYKT_BARN ->
                sikreAtOppdateringIkkeKrysser18årsdag(behandling, sykdomVurderingOpprettelse.getPerioder());
            case PLEIEPENGER_NÆRSTÅENDE -> ingenValidering();
            default -> throw new IllegalStateException("Ikke-støttet ytelsetype: " + fagsakYtelseType);
        }

        switch (fagsakYtelseType) {
            case PLEIEPENGER_SYKT_BARN ->
                validerSykdomvurderingTyper(sykdomVurderingOpprettelse, Set.of(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE, SykdomVurderingType.TO_OMSORGSPERSONER));
            case PLEIEPENGER_NÆRSTÅENDE ->
                validerSykdomvurderingTyper(sykdomVurderingOpprettelse, Set.of(SykdomVurderingType.LIVETS_SLUTTFASE));
            default -> throw new IllegalStateException("Ikke-støttet ytelsetype: " + fagsakYtelseType);
        }

        prosessDriver.validerTilstand(behandling, sykdomVurderingOpprettelse.isDryRun());
    }

    private void ingenValidering() {
    }

    private void validerSykdomvurderingTyper(SykdomVurderingOpprettelseDto sykdomVurderingOpprettelse, Set<SykdomVurderingType> tillatteSykdomVurderingTyper) {
        if (!tillatteSykdomVurderingTyper.contains(sykdomVurderingOpprettelse.getType())) {
            throw new IllegalArgumentException("Ikke-støttet sykdomtype " + sykdomVurderingOpprettelse.getType() + " for aktuell ytelsetype");
        }
    }

    void fjernOverlappendePerioderFraOverskyggendeVurderinger(List<SykdomPeriodeMedEndring> endringer, Sporingsinformasjon sporing, LocalDateTime opprettetTidspunkt) {
        Map<PleietrengendeSykdomVurderingVersjon, List<Periode>> perioderSomSkalFjernesFraVurdering = finnPerioderSomSkalFjernesPerVurdering(endringer);

        for (Map.Entry<PleietrengendeSykdomVurderingVersjon, List<Periode>> vurderingPerioder : perioderSomSkalFjernesFraVurdering.entrySet()) {
            PleietrengendeSykdomVurderingVersjon vurdering = vurderingPerioder.getKey();

            LocalDateTimeline<Boolean> tidslinjeSomSkalTrekkesFra = TidslinjeUtil.tilTidslinjeKomprimert(vurderingPerioder.getValue());

            LocalDateTimeline<Boolean> gammelTidslinje = TidslinjeUtil.tilTidslinjeKomprimert(vurdering.getPerioder().stream().map(p -> new Periode(p.getFom(), p.getTom())).collect(Collectors.toList()));

            LocalDateTimeline<Boolean> nyePerioder = TidslinjeUtil.kunPerioderSomIkkeFinnesI(gammelTidslinje, tidslinjeSomSkalTrekkesFra);

            List<Periode> vurderingPerioderTilLagring = TidslinjeUtil.tilPerioder(nyePerioder);

            PleietrengendeSykdomVurderingVersjon tilLagring = new PleietrengendeSykdomVurderingVersjon(
                vurdering.getSykdomVurdering(),
                vurdering.getTekst(),
                vurdering.getResultat(),
                vurdering.getVersjon() + 1,
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

    private HashMap<PleietrengendeSykdomVurderingVersjon, List<Periode>> finnPerioderSomSkalFjernesPerVurdering(List<SykdomPeriodeMedEndring> endringer) {
        HashMap<PleietrengendeSykdomVurderingVersjon, List<Periode>> perioderSomSkalFjernesFraVurdering = new HashMap<>();
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

    private List<SykdomPeriodeMedEndring> finnEndringer(Behandling behandling, PleietrengendeSykdomVurderingVersjon nyEndring) {
        var vurderinger = sykdomVurderingTjeneste.hentVurderinger(nyEndring.getSykdomVurdering().getType(), behandling);
        return sykdomVurderingRepository.finnEndringer(vurderinger, nyEndring);
    }

    public static class AbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett();
        }

    }
}
