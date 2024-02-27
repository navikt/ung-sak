package no.nav.k9.sak.web.app.tjenester.forvaltning;

import static no.nav.k9.abac.BeskyttetRessursKoder.DRIFT;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.prosessering.task.TilbakeTilStartBehandlingTask;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.KortTekst;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.k9.sak.trigger.ProsessTriggerForvaltningTjeneste;
import no.nav.k9.sak.trigger.ProsessTriggereRepository;
import no.nav.k9.sak.trigger.Trigger;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.logg.DiagnostikkFagsakLogg;
import no.nav.k9.sak.web.server.abac.AbacAttributtEmptySupplier;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.pleiepengerbarn.iverksett.EndringAnnenOmsorgspersonUtleder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.PleiepengerVilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning.EndringsårsakUtbetaling;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning.PleiepengerEndretUtbetalingPeriodeutleder;

@ApplicationScoped
@Transactional
@Path("/uttak")
@Produces(MediaType.APPLICATION_JSON)
public class ForvaltningUttakRestTjeneste {

    private static Logger log = LoggerFactory.getLogger(ForvaltningUttakRestTjeneste.class);
    private BehandlingRepository behandlingRepository;
    private EntityManager entityManager;
    private ProsessTriggerForvaltningTjeneste prosessTriggerForvaltningTjeneste;
    private ProsessTriggereRepository prosessTriggereRepository;
    private EndringAnnenOmsorgspersonUtleder endringAnnenOmsorgspersonUtleder;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private PleiepengerEndretUtbetalingPeriodeutleder pleiepengerEndretUtbetalingPeriodeutleder;

    private PleiepengerVilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste;

    public ForvaltningUttakRestTjeneste() {
    }

    @Inject
    public ForvaltningUttakRestTjeneste(BehandlingRepository behandlingRepository,
                                        EntityManager entityManager,
                                        ProsessTriggereRepository prosessTriggereRepository,
                                        EndringAnnenOmsorgspersonUtleder endringAnnenOmsorgspersonUtleder, ProsessTaskTjeneste prosessTaskTjeneste,
                                        @FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN) @BehandlingTypeRef(BehandlingType.REVURDERING) PleiepengerEndretUtbetalingPeriodeutleder pleiepengerEndretUtbetalingPeriodeutleder,
                                        @FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN) PleiepengerVilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.entityManager = entityManager;
        this.prosessTriggerForvaltningTjeneste = new ProsessTriggerForvaltningTjeneste(entityManager);
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.endringAnnenOmsorgspersonUtleder = endringAnnenOmsorgspersonUtleder;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.pleiepengerEndretUtbetalingPeriodeutleder = pleiepengerEndretUtbetalingPeriodeutleder;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
    }


    @POST
    @Path("/hent-endret-uttak-revurdering-tidslinjer")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter tidslinje for revurdering av uttak", summary = ("Henter tidslinje for revurdering av uttak"), tags = "uttak")
    @BeskyttetRessurs(action = READ, resource = DRIFT)
    public Response hentEndringsperioderTidslinje(
        @Parameter(description = "Behandling-id")
        @FormParam("behandlingId")
        @NotNull
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
        BehandlingIdDto behandlingIdDto,
        @NotNull @FormParam("periode")
        @Parameter(description = "periode", required = true, example = "2020-01-01/2020-12-31")
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class)
        Periode periode) {
        var behandling = behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingId());

        var behandlingReferanse = BehandlingReferanse.fra(behandling);
        var perioderTilVurdering = vilkårsPerioderTilVurderingTjeneste.utled(behandling.getId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);

        var vilkårsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom());
        if (!perioderTilVurdering.contains(vilkårsperiode)) {
            throw new IllegalArgumentException("Oppgitt periode er ikke til vurdering i behandlingen. Perioder til vurdering er " + perioderTilVurdering);
        }

        var tidslinje = pleiepengerEndretUtbetalingPeriodeutleder.finnÅrsakstidslinje(behandlingReferanse, vilkårsperiode);
        var result = new HashMap<>();
        for (EndringsårsakUtbetaling v : EndringsårsakUtbetaling.values()) {
            var resultTidslinje = tidslinje.filterValue(it -> it.contains(v)).mapValue(it -> true);
            result.put(v, resultTidslinje.getLocalDateIntervals().stream().map(di -> DatoIntervallEntitet.fraOgMedTilOgMed(di.getFomDato(), di.getTomDato())).toList());
        }
        return Response.ok(result).build();

    }


    @POST
    @Path("/hent-endringstidslinjer-fra-annen-omsorgsperson")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter tidslinjer der annen omsorgsperson påvirket sak for sak med behandlingsårsak RE_ANNEN_SAK", summary = ("Henter tidslinjer som fører til endring i sak"), tags = "uttak")
    @BeskyttetRessurs(action = READ, resource = DRIFT)
    @Produces(MediaType.APPLICATION_JSON)
    public Response hentTidslinjerEndringFraAnnenOmsorgsperson(
        @Parameter(description = "Behandling-id")
        @NotNull
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
        BehandlingIdDto behandlingIdDto) {


        var behandling = behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingId());


        var originalBehandlingId = behandling.getOriginalBehandlingId();

        if (originalBehandlingId.isEmpty()) {
            throw new IllegalArgumentException("Saken har ingen revurdering");
        }

        var originalBehandling = behandlingRepository.hentBehandling(originalBehandlingId.get());


        var prosessTriggere = prosessTriggereRepository.hentGrunnlag(behandling.getId());
        var perioderMedEndringAnnenOmsorgsperson = prosessTriggere.stream()
            .flatMap(it -> it.getTriggere().stream())
            .filter(it -> it.getÅrsak().equals(BehandlingÅrsakType.RE_ENDRING_FRA_ANNEN_OMSORGSPERSON))
            .map(Trigger::getPeriode)
            .collect(Collectors.toSet());
        var segmenterEndringAnnenOmsorgsperson = perioderMedEndringAnnenOmsorgsperson.stream()
            .map(it -> new LocalDateSegment<>(it.toLocalDateInterval(), Boolean.TRUE))
            .toList();

        var endringAnnenOmsorgspersonTidslinje = new LocalDateTimeline<>(segmenterEndringAnnenOmsorgsperson);


        var endringstidslinjer = endringAnnenOmsorgspersonUtleder.utledTidslinjerForEndringSomPåvirkerSak(behandling.getFagsak().getSaksnummer(), behandling.getFagsak().getPleietrengendeAktørId(), originalBehandling, endringAnnenOmsorgspersonTidslinje);

        var endringsperioderDto = new EndringsperioderDto(
            lagPeriodeListe(endringstidslinjer.harEndretSykdomTidslinje()),
            lagPeriodeListe(endringstidslinjer.harEndretEtablertTilsynTidslinje()),
            lagPeriodeListe(endringstidslinjer.harEndretNattevåkOgBeredskapTidslinje()),
            lagPeriodeListe(endringstidslinjer.harEndretUttakTidslinje()),
            lagPeriodeListe(endringstidslinjer.harEndringSomPåvirkerSakTidslinje())
        );

        loggForvaltningTjeneste(behandling.getFagsak(), "/hent-endringstidslinjer-fra-annen-omsorgsperson", "henter endringstidslinjer for sak der sak ble påvirket av annen part");


        return Response.ok(endringsperioderDto).build();

    }

    private static List<Periode> lagPeriodeListe(LocalDateTimeline<Boolean> tidslinje) {
        return tidslinje.compress()
            .getLocalDateIntervals().stream()
            .map(it -> new Periode(it.getFomDato(), it.getTomDato()))
            .toList();
    }

    @POST
    @Path("/fjern-prosesstrigger-endring-annen-omsorgsperson")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Fjerner prosesstrigger for endring fra annen omsorgsperson", summary = ("Fjerner prosesstrigger for endring fra annen omsorgsperson"), tags = "uttak")
    @BeskyttetRessurs(action = CREATE, resource = DRIFT)
    public void fjernProsessTriggerForEndringFraAnnenOmsorgsperson(
        @Parameter(description = "Behandling-id")
        @NotNull
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
        BehandlingIdDto behandlingIdDto) {
        var behandling = behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingId());

        if (behandling.erSaksbehandlingAvsluttet()) {
            throw new IllegalArgumentException("Behandling med id " + behandling.getId() + " hadde avsluttet saksbehandling.");
        }

        var originalBehandlingId = behandling.getOriginalBehandlingId();

        if (originalBehandlingId.isEmpty()) {
            throw new IllegalArgumentException("Saken har ingen revurdering");
        }

        var originalBehandling = behandlingRepository.hentBehandling(originalBehandlingId.get());


        var prosessTriggere = prosessTriggereRepository.hentGrunnlag(behandling.getId());
        var perioderMedEndringAnnenOmsorgsperson = prosessTriggere.stream()
            .flatMap(it -> it.getTriggere().stream())
            .filter(it -> it.getÅrsak().equals(BehandlingÅrsakType.RE_ENDRING_FRA_ANNEN_OMSORGSPERSON))
            .map(Trigger::getPeriode)
            .collect(Collectors.toSet());
        var segmenterEndringAnnenOmsorgsperson = perioderMedEndringAnnenOmsorgsperson.stream()
            .map(it -> new LocalDateSegment<>(it.toLocalDateInterval(), Boolean.TRUE))
            .toList();

        var endringAnnenOmsorgspersonTidslinje = new LocalDateTimeline<>(segmenterEndringAnnenOmsorgsperson);


        var fagsak = behandling.getFagsak();
        var endringstidslinjer = endringAnnenOmsorgspersonUtleder.utledTidslinjerForEndringSomPåvirkerSak(fagsak.getSaksnummer(), behandling.getFagsak().getPleietrengendeAktørId(), originalBehandling, endringAnnenOmsorgspersonTidslinje);

        var utdaterteEndringer = endringAnnenOmsorgspersonTidslinje.disjoint(endringstidslinjer.harEndringSomPåvirkerSakTidslinje());

        var utdaterteIntervaller = utdaterteEndringer.getLocalDateIntervals().stream().map(p -> DatoIntervallEntitet.fraOgMedTilOgMed(p.getFomDato(), p.getTomDato())).collect(Collectors.toSet());


        if (!utdaterteIntervaller.isEmpty()) {
            loggForvaltningTjeneste(fagsak, "/fjern-prosesstrigger-endring-annen-omsorgsperson", "fjerner prosesstrigger RE_ANNEN_SAK for perioder " + utdaterteIntervaller);
            log.info("Forsøker fjerning av triggere for perioder " + utdaterteIntervaller + " for behandling " + behandling.getId());
            utdaterteIntervaller.forEach(periode -> prosessTriggerForvaltningTjeneste.fjern(behandlingIdDto.getBehandlingId(), periode, BehandlingÅrsakType.RE_ENDRING_FRA_ANNEN_OMSORGSPERSON));
            flyttTilbakeTilStart(behandling);
        } else {
            log.info("Fant ingen triggere for fjerning");
        }
    }

    @POST
    @Path("/fjern-prosesstrigger-endring-annen-omsorgsperson-gitt-periode")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Fjerner prosesstrigger for endring fra annen omsorgsperson", summary = ("Fjerner prosesstrigger for endring fra annen omsorgsperson"), tags = "uttak")
    @BeskyttetRessurs(action = CREATE, resource = DRIFT)
    public void fjernProsessTriggerForEndringFraAnnenOmsorgspersonMedGittPeriode(
        @Parameter(description = "Behandling-id")
        @FormParam("behandlingId")
        @NotNull
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
        BehandlingIdDto behandlingIdDto,
        @NotNull
        @FormParam("begrunnelse")
        @Parameter(description = "begrunnelse", allowEmptyValue = false, required = true, schema = @Schema(type = "string", maximum = "2000"))
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class)
        KortTekst begrunnelse,
        @NotNull @FormParam("periode")
        @Parameter(description = "periode", required = true, example = "2020-01-01/2020-12-31")
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class)
        Periode periode
    ) {
        var behandling = behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingId());

        if (behandling.erSaksbehandlingAvsluttet()) {
            throw new IllegalArgumentException("Behandling med id " + behandling.getId() + " hadde avsluttet saksbehandling.");
        }

        var fagsak = behandling.getFagsak();
        var fjernet = prosessTriggerForvaltningTjeneste.fjern(behandlingIdDto.getBehandlingId(), DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom()), BehandlingÅrsakType.RE_ENDRING_FRA_ANNEN_OMSORGSPERSON);
        if (fjernet) {
            loggForvaltningTjeneste(fagsak, "/fjern-prosesstrigger-endring-annen-omsorgsperson-gitt-periode", "Fjerner prosesstrigger RE_ANNEN_SAK for periode " + periode + " og behandling " + behandling.getId() +
                ". Begrunnelse: " + begrunnelse.getTekst());
        }

    }


    private void flyttTilbakeTilStart(Behandling behandling) {
        var prosessTaskData = ProsessTaskData.forProsessTask(TilbakeTilStartBehandlingTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId());
        prosessTaskTjeneste.lagre(prosessTaskData);
    }

    private void loggForvaltningTjeneste(Fagsak fagsak, String tjeneste, String begrunnelse) {
        /*
         * logger at tjenesten er kalt (er en forvaltnings tjeneste)
         */
        entityManager.persist(new DiagnostikkFagsakLogg(fagsak.getId(), tjeneste, begrunnelse));
        entityManager.flush();
    }


}
