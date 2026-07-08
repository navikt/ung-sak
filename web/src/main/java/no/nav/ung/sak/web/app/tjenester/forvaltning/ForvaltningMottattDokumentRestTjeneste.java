package no.nav.ung.sak.web.app.tjenester.forvaltning;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.*;
import no.nav.k9.felles.validering.InputValideringRegex;
import no.nav.k9.oppgave.OppgaveBekreftelse;
import no.nav.k9.oppgave.bekreftelse.ung.periodeendring.EndretSluttdatoBekreftelse;
import no.nav.k9.søknad.JsonUtils;
import no.nav.ung.fordel.repo.journalpost.JournalpostRepository;
import no.nav.ung.kodeverk.abac.StandardAbacAttributt;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.logg.DiagnostikkFagsakLogg;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;

import java.util.List;


/**
 * DENNE TJENESTEN ER BARE FOR MIDLERTIDIG BEHOV FOR TSFF-2756, OG SKAL AVVIKLES SÅ RASKT SOM MULIG.
 */
@Path("/TSFF-2756/forvaltning")
@ApplicationScoped
@Transactional
public class ForvaltningMottattDokumentRestTjeneste {

    private static final String JSON_UTF8 = "application/json; charset=UTF-8";

    private BehandlingRepository behandlingRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private EntityManager entityManager;
    private JournalpostRepository journalpostRepository;

    public ForvaltningMottattDokumentRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public ForvaltningMottattDokumentRestTjeneste(BehandlingRepository behandlingRepository,
                                                  MottatteDokumentRepository mottatteDokumentRepository,
                                                  EntityManager entityManager,
                                                  JournalpostRepository journalpostRepository) {
        this.behandlingRepository = behandlingRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.entityManager = entityManager;
        this.journalpostRepository = journalpostRepository;
    }

    @POST
    @Path("oppdater-mottatt-dokument")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "oppdaterer mottatt dokument ifm TSFF-2756", summary = ("oppdaterer mottatt dokument ifm TSFF-2756"), tags = "forvaltning")
    @Produces(JSON_UTF8)
    @BeskyttetRessurs(action = BeskyttetRessursActionType.UPDATE, resource = BeskyttetRessursResourceType.DRIFT)
    public Response oppdaterMottattDokument(@Valid @NotNull @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) OppdaterMottattDokumentRequest dto) {
        String nyUttalelse = dto.nyUttalelseFraBruker;
        if (nyUttalelse == null || nyUttalelse.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("nyUttalelseFraBruker må ha en verdi")
                .build();
        }

        Behandling behandling = behandlingRepository.hentBehandling(dto.behandlingId.getId());
        JournalpostId journalpostId = dto.journalpostId.getJournalpostId();
        List<MottattDokument> mottattDokuments = mottatteDokumentRepository.hentMottatteDokument(behandling.getFagsakId(), List.of(journalpostId)).stream()
            .filter(it -> it.getStatus() == DokumentStatus.MOTTATT).toList();

        if (mottattDokuments.size() != 1) {
            throw new IllegalArgumentException("Forventet 1 dokument fant "+mottattDokuments.size());
        }

        MottattDokument mottattDokument = mottattDokuments.getFirst();

        // Sjekk at dokumentet er av riktig type
        if (!Brevkode.UNGDOMSYTELSE_VARSEL_UTTALELSE.equals(mottattDokument.getType())) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Forventet brevkode " + Brevkode.UNGDOMSYTELSE_VARSEL_UTTALELSE + ", men dokumentet har: " + mottattDokument.getType())
                .build();
        }

        OppgaveBekreftelse oppgaveBekreftelse = JsonUtils.fromString(mottattDokument.getPayload(), OppgaveBekreftelse.class);

        if (!(oppgaveBekreftelse.getBekreftelse() instanceof EndretSluttdatoBekreftelse eksisterendeBekreftelse)) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Forventet bekreftelse av type EndretSluttdatoBekreftelse, men fikk: " + oppgaveBekreftelse.getBekreftelse().getType())
                .build();
        }

        if (!eksisterendeBekreftelse.harUttalelse()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Eksisterende payload har harUttalelse=false og kan derfor ikke patches")
                .build();
        }
        if (eksisterendeBekreftelse.getUttalelseFraBruker() != null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Eksisterende payload har allerede uttalelseFraBruker satt")
                .build();
        }

        EndretSluttdatoBekreftelse endretBekreftelse = new EndretSluttdatoBekreftelse(
            eksisterendeBekreftelse.getOppgaveReferanse(),
            eksisterendeBekreftelse.nySluttdato(),
            true,
            nyUttalelse,
            eksisterendeBekreftelse.dataBruktTilUtledning());

        oppgaveBekreftelse.setBekreftelse(endretBekreftelse);
        String nyPayload = JsonUtils.toString(oppgaveBekreftelse);

        // Oppdater MottattDokument (payload er updatable=false, så bruk native oppdatering)
        mottatteDokumentRepository.oppdaterPayload(mottattDokument.getId(), nyPayload);

        // Oppdater JournalpostMottattEntitet
        journalpostRepository.finnJournalpostMottatt(journalpostId).ifPresent(mottatt -> {
            mottatt.setPayload(nyPayload);
            journalpostRepository.lagreMottatt(mottatt);
        });

        // Oppdater JournalpostInnsendingEntitet
        journalpostRepository.finnJournalpostInnsending(journalpostId).ifPresent(innsending -> {
            innsending.setPayload(nyPayload);
            journalpostRepository.lagreInnsending(innsending);
        });

        entityManager.persist(new DiagnostikkFagsakLogg(
            behandling.getFagsakId(),
            "ForvaltningMottattDokumentRestTjeneste.oppdaterMottattDokument (midlertidig)",
            "Patching ifm TSFF-2756. Oppdatert uttalelseFraBruker i journalpost=%s payload i 3 tabeller: MOTTATT_DOKUMENT, FORDEL_JOURNALPOST_INNSENDING og FORDEL_JOURNALPOST_MOTTATT"
                .formatted(journalpostId.getVerdi())
        ));

        return Response.ok().build();
    }

    public record OppdaterMottattDokumentRequest(
        @Valid
        @StandardAbacAttributt(StandardAbacAttributtType.JOURNALPOST_ID)
        JournalpostId journalpostId,

        @JsonProperty(value = "behandlingId", required = true)
        @NotNull
        @Valid
        BehandlingIdDto behandlingId,

        @Valid
        @Size(max = 4000)
        @Pattern(regexp = InputValideringRegex.FRITEKST)
        String nyUttalelseFraBruker
    ) {

        @StandardAbacAttributt(StandardAbacAttributtType.BEHANDLING_ID)
        public Long getBehandlingId() {
            return behandlingId.getBehandlingId();
        }



    }


}
