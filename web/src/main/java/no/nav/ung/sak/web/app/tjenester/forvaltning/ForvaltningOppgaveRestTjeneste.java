package no.nav.ung.sak.web.app.tjenester.forvaltning;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask;
import no.nav.ung.sak.behandling.revurdering.inntektskontroll.OpprettOppgaveForInntektsrapporteringTask;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;
import java.util.UUID;

import static no.nav.ung.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask.BEHANDLING_ÅRSAK;
import static no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask.PERIODER;

@Path("/oppgave/forvaltning")
@ApplicationScoped
@Transactional
public class ForvaltningOppgaveRestTjeneste {

    private EntityManager entityManager;
    private BehandlingRepository behandlingRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private FagsakRepository fagsakRepository;
    private MånedsvisTidslinjeUtleder månedsvisTidslinjeUtleder;
    private boolean isProd = Environment.current().isProd();

    @Inject
    public ForvaltningOppgaveRestTjeneste(EntityManager entityManager, BehandlingRepository behandlingRepository, ProsessTaskTjeneste prosessTaskTjeneste, FagsakRepository fagsakRepository, MånedsvisTidslinjeUtleder månedsvisTidslinjeUtleder) {
        this.entityManager = entityManager;
        this.behandlingRepository = behandlingRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.månedsvisTidslinjeUtleder = månedsvisTidslinjeUtleder;
    }

    public ForvaltningOppgaveRestTjeneste() {
        // For Rest-CDI
    }



    @POST
    @Path("opprett-inntektsrapportering")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(description = "Oppretter oppgave for inntektsrapportering for gitt sak", summary = ("Oppretter oppgave for inntektsrapportering"), tags = "oppgave")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FAGSAK)
    public Response opprettInntektsrapportering(@NotNull @FormParam("saksnummer") @Parameter(description = "saksnummer", required = true) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) Saksnummer saksnummer,
                                                @NotNull @QueryParam("måned") @Parameter(description = "måned", required = true) MånedForRapportering måned) {
        if (isProd) {
            throw new IllegalArgumentException("Kan ikke kjøre denne tjenesten i prod");
        }

        final var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer, false);

        final var periode = finnPeriode(måned, fagsak);

        final var startRapporteringTask = ProsessTaskData.forProsessTask(OpprettOppgaveForInntektsrapporteringTask.class);
        startRapporteringTask.setAktørId(fagsak.get().getAktørId().getAktørId());
        startRapporteringTask.setProperty(OpprettOppgaveForInntektsrapporteringTask.PERIODE_FOM, periode.getFomDato().format(DateTimeFormatter.ISO_LOCAL_DATE));
        startRapporteringTask.setProperty(OpprettOppgaveForInntektsrapporteringTask.PERIODE_TOM, periode.getTomDato().format(DateTimeFormatter.ISO_LOCAL_DATE));
        startRapporteringTask.setProperty(OpprettOppgaveForInntektsrapporteringTask.OPPGAVE_REF, UUID.randomUUID().toString());
        prosessTaskTjeneste.lagre(startRapporteringTask);

        return Response.ok().build();
    }

    private LocalDateInterval finnPeriode(MånedForRapportering måned, Optional<Fagsak> fagsak) {
        final var sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.get().getId()).get();

        final var periodisertMånedvis = månedsvisTidslinjeUtleder.periodiserMånedsvis(sisteBehandling.getId());

        final var periode = periodisertMånedvis.stream()
            // Enum ordinal er 0-indeksert og montvalue er 1-indeksert
            .filter(it -> it.getFom().getMonthValue() == måned.ordinal() + 1)
            .findFirst().map(LocalDateSegment::getLocalDateInterval).get();
        return periode;
    }

    @POST
    @Path("start-inntektskontroll")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(description = "Starter inntektskontroll for sak", summary = ("Starter inntektskontroll for sak"), tags = "oppgave")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FAGSAK)
    public Response startInntektskontroll(@NotNull @FormParam("saksnummer") @Parameter(description = "saksnummer", required = true) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) Saksnummer saksnummer,
                                          @NotNull @QueryParam("måned") @Parameter(description = "måned", required = true) MånedForRapportering måned) {
        if (isProd) {
            throw new IllegalArgumentException("Kan ikke kjøre denne tjenesten i prod");
        }

        final var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer, false);
        final var periode = finnPeriode(måned, fagsak);

        final var startKontrollTask = ProsessTaskData.forProsessTask(OpprettRevurderingEllerOpprettDiffTask.class);
        startKontrollTask.setFagsakId(fagsak.get().getId());
        startKontrollTask.setProperty(BEHANDLING_ÅRSAK, BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT.getKode());
        startKontrollTask.setProperty(PERIODER, periode.getFomDato().format(DateTimeFormatter.ISO_LOCAL_DATE) + "/" + periode.getTomDato().format(DateTimeFormatter.ISO_LOCAL_DATE));
        prosessTaskTjeneste.lagre(startKontrollTask);
        return Response.ok().build();
    }



    @POST
    @Path("fjern-uttalelse")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Fjerner alle uttalelser om register inntekt og avbryter alle eksisterende etterlysninger for behandling", summary = ("Fjerner alle uttalelser om register inntekt og avbryter alle eksisterende etterlysninger for behandling"), tags = "oppgave")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.DELETE, resource = FAGSAK)
    public Response fjernUttalelser(@NotNull @QueryParam("behandlingId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto behandlingIdDto) {
        if (isProd) {
            throw new IllegalArgumentException("Kan ikke kjøre denne tjenesten i prod");
        }

        final var behandlingId = Long.parseLong(behandlingIdDto.getId());
        entityManager.createNativeQuery("DELETE FROM UTTALELSE WHERE etterlysning_id in (select id from ETTERLYSNING where behandling_id = :behandlingId)")
            .setParameter("behandlingId", behandlingId)
            .executeUpdate();


        entityManager.createNativeQuery("DELETE FROM ETTERLYSNING WHERE behandling_id = :behandlingId")
            .setParameter("behandlingId", behandlingId)
            .executeUpdate();


        entityManager.createNativeQuery("DELETE FROM KONTROLLERT_INNTEKT_PERIODE WHERE kontrollert_inntekt_perioder_id in (select id from KONTROLLERT_INNTEKT_PERIODER where behandling_id = :behandlingId)")
            .setParameter("behandlingId", behandlingId)
            .executeUpdate();

        entityManager.createNativeQuery("DELETE FROM KONTROLLERT_INNTEKT_PERIODER WHERE behandling_id = :behandlingId")
            .setParameter("behandlingId", behandlingId)
            .executeUpdate();

        opprettProsessTask(behandlingId);
        return Response.ok().build();
    }

    private void opprettProsessTask(Long behandlingId) {
        final var behandling = behandlingRepository.hentBehandling(behandlingId);

        var fagsakId = behandling.getFagsakId();

        var fom = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        var tom = fom.with(TemporalAdjusters.lastDayOfMonth());

        ProsessTaskData tilVurderingTask = ProsessTaskData.forProsessTask(OpprettRevurderingEllerOpprettDiffTask.class);
        tilVurderingTask.setFagsakId(fagsakId);
        tilVurderingTask.setProperty(PERIODER, fom + "/" + tom);
        tilVurderingTask.setProperty(BEHANDLING_ÅRSAK, BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT.getKode());

        prosessTaskTjeneste.lagre(tilVurderingTask);
    }

}
