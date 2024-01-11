package no.nav.k9.sak.web.app.tjenester.forvaltning;

import static no.nav.k9.abac.BeskyttetRessursKoder.DRIFT;
import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandling.prosessering.task.TilbakeTilStartBehandlingTask;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.k9.sak.trigger.ProsessTriggerForvaltningTjeneste;
import no.nav.k9.sak.trigger.ProsessTriggereRepository;
import no.nav.k9.sak.trigger.Trigger;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.logg.DiagnostikkFagsakLogg;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.pleiepengerbarn.iverksett.EndringAnnenOmsorgspersonUtleder;

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

    public ForvaltningUttakRestTjeneste() {
    }

    @Inject
    public ForvaltningUttakRestTjeneste(BehandlingRepository behandlingRepository,
                                        EntityManager entityManager,
                                        ProsessTriggereRepository prosessTriggereRepository,
                                        EndringAnnenOmsorgspersonUtleder endringAnnenOmsorgspersonUtleder, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.entityManager = entityManager;
        this.prosessTriggerForvaltningTjeneste = new ProsessTriggerForvaltningTjeneste(entityManager);
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.endringAnnenOmsorgspersonUtleder = endringAnnenOmsorgspersonUtleder;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
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

        var utdaterteIntervaller = utdaterteEndringer.compress().getLocalDateIntervals();

        var skjæringstidspunkterSomKanFjernes = perioderMedEndringAnnenOmsorgsperson.stream()
            .filter(p -> utdaterteIntervaller.stream().anyMatch(i -> i.getFomDato().equals(p.getFomDato()) && i.getTomDato().equals(p.getTomDato())))
            .map(DatoIntervallEntitet::getFomDato)
            .collect(Collectors.toSet());

        loggForvaltningTjeneste(fagsak, "/fjern-prosesstrigger-endring-annen-omsorgsperson", "fjerner prosesstrigger RE_ANNEN_SAK for skjæringstidspunkter " + skjæringstidspunkterSomKanFjernes);

        skjæringstidspunkterSomKanFjernes.forEach(stp -> prosessTriggerForvaltningTjeneste.fjern(behandlingIdDto.getBehandlingId(), stp, BehandlingÅrsakType.RE_ENDRING_FRA_ANNEN_OMSORGSPERSON));

        log.info("Fjernet triggere for skjæringstidspunkter " + skjæringstidspunkterSomKanFjernes + " for behandling " + behandling.getId());

        flyttTilbakeTilStart(behandling);
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