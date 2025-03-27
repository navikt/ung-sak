package no.nav.ung.sak.web.app.tjenester.forvaltning;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelseOppdrag;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

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
    private boolean isProd = Environment.current().isProd();

    @Inject
    public ForvaltningOppgaveRestTjeneste(EntityManager entityManager, BehandlingRepository behandlingRepository, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.entityManager = entityManager;
        this.behandlingRepository = behandlingRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    public ForvaltningOppgaveRestTjeneste() {
        // For Rest-CDI
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
