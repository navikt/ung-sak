package no.nav.ung.sak.web.app.tjenester.forvaltning;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
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
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.felles.sikkerhet.abac.*;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.ytelse.ung.v1.Ungdomsytelse;
import no.nav.ung.kodeverk.abac.StandardAbacAttributt;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.sak.behandling.prosessering.task.TilbakeTilStartBehandlingTask;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.KortTekst;
import no.nav.ung.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.ung.sak.mottak.dokumentmottak.SøknadParser;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.logg.DiagnostikkFagsakLogg;
import no.nav.ung.sak.web.server.abac.AbacAttributtEmptySupplier;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;


@Path("/forvaltning")
@ApplicationScoped
@Transactional
public class ForvaltningMottattDokumentRestTjeneste {

    private static final String JSON_UTF8 = "application/json; charset=UTF-8";

    private static final Set<Brevkode> TILLATTE_BREVKODER = Set.of(
        Brevkode.UNGDOMSYTELSE_VARSEL_UTTALELSE,
        Brevkode.UNGDOMSYTELSE_INNTEKTRAPPORTERING
    );
    private static final Logger log = LoggerFactory.getLogger(ForvaltningMottattDokumentRestTjeneste.class);

    private static final Set<Brevkode> TILLATT_BREVKODE_MAKULERING = Set.of(
        Brevkode.UNGDOMSYTELSE_SOKNAD
    );

    private FagsakRepository fagsakRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private BehandlingRepository behandlingRepository;
    private ProsessTriggereRepository prosessTriggereRepository;
    private ProsessTaskTjeneste taskTjeneste;
    private SøknadParser søknadParser;
    private EntityManager entityManager;

    public ForvaltningMottattDokumentRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public ForvaltningMottattDokumentRestTjeneste(FagsakRepository fagsakRepository,
                                                  MottatteDokumentRepository mottatteDokumentRepository,
                                                  BehandlingRepository behandlingRepository,
                                                  ProsessTriggereRepository prosessTriggereRepository,
                                                  ProsessTaskTjeneste taskTjeneste,
                                                  SøknadParser søknadParser,
                                                  EntityManager entityManager) {
        this.fagsakRepository = fagsakRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.behandlingRepository = behandlingRepository;
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.taskTjeneste = taskTjeneste;
        this.søknadParser = søknadParser;
        this.entityManager = entityManager;
    }

    @POST
    @Path("/marker-ugyldig")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Markerer et mottatt dokument som ugyldig", summary = ("Markerer angitt dokument som ugyldig"), tags = "forvaltning")
    @Produces(JSON_UTF8)
    @BeskyttetRessurs(action = BeskyttetRessursActionType.UPDATE, resource = BeskyttetRessursResourceType.DRIFT)
    public Response markerMottattDokumentUgyldig(@Valid @NotNull @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) MarkerDokumentUgyldigRequest dto) {
        var fagsakOpt = fagsakRepository.hentSakGittSaksnummer(dto.saksnummer());
        if (fagsakOpt.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("Fant ikke fagsak for saksnummer: " + dto.saksnummer().getVerdi())
                .build();
        }
        Long fagsakId = fagsakOpt.get().getId();
        JournalpostId journalpostId = dto.journalpostId().getJournalpostId();
        List<MottattDokument> mottattDokuments = mottatteDokumentRepository.hentMottatteDokument(fagsakId, List.of(journalpostId));

        if (mottattDokuments.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("Fant ingen dokumenter for saksnummer " + dto.saksnummer().getVerdi() + " og journalpostId " + journalpostId.getVerdi())
                .build();
        }

        if (mottattDokuments.size() > 1) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Forventet maks 1 dokument, men fant " + mottattDokuments.size())
                .build();
        }

        MottattDokument mottattDokument = mottattDokuments.getFirst();

        if (!TILLATTE_BREVKODER.contains(mottattDokument.getType())) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Dokumentet har brevkode " + mottattDokument.getType() + ", bare dokumenter med brevkodene " + TILLATTE_BREVKODER + " kan settes ugyldige")
                .build();
        }

        if (mottattDokument.getStatus() != DokumentStatus.MOTTATT) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Forventet at dokument har status MOTTATT, men hadde: " + mottattDokument.getStatus())
                .build();
        }


        String formatertBegrunnelse = "Markert som ugyldig av teknisk forvaltning. Begrunnelse: %s".formatted(dto.begrunnelse().getTekst());
        mottattDokument.setFeilmeldingOgOppdaterStatus(formatertBegrunnelse);
        mottatteDokumentRepository.oppdater(mottattDokument);

        entityManager.persist(new DiagnostikkFagsakLogg(fagsakId, "/marker-ugyldig", formatertBegrunnelse));
        entityManager.flush();

        log.info("Manuelt markert mottatt dokument med journalpostId={} av type {} som ugyldig.", mottattDokument.getJournalpostId().getVerdi(), mottattDokument.getType());

        return Response.ok().build();
    }

    /**
     * Makulerer en duplikat søknad: markerer det mottatte dokumentet som ugyldig, fjerner den tilhørende
     * NY_SØKT_PERIODE-prosesstriggeren for behandlingen (utledet ved å reparse søknaden på samme måte som
     * ved opprinnelig mottak), og sender behandlingen tilbake til start slik at perioder/vilkår vurderes på
     * nytt uten den duplikate søknaden.
     * <p>
     * Brukes for tilfeller der en digital søknad viser seg å være et rent duplikat av en allerede
     * registrert (f.eks. papir-)søknad, og der den digitale søknadens trigger ellers vil føre til at
     * behandlingen krasjer i vilkårsvurderingen (jf. "Hadde startdato som ikke kunne matches med søknadsperiode").
     */
    @POST
    @Path("/makuler-duplikat-soknad")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Makulerer en duplikat søknad: markerer dokumentet ugyldig, fjerner tilhørende prosesstrigger og sender behandlingen tilbake til start.",
        summary = ("Makulerer duplikat søknad og resetter behandlingen"), tags = "forvaltning")
    @Produces(JSON_UTF8)
    @BeskyttetRessurs(action = BeskyttetRessursActionType.UPDATE, resource = BeskyttetRessursResourceType.DRIFT)
    public Response makulerDuplikatSøknad(@Valid @NotNull @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) MarkerDokumentUgyldigRequest dto) {
        log.info("Starter makulering av duplikat søknad: saksnummer={}, journalpostId={}", dto.saksnummer().getVerdi(), dto.journalpostId().getJournalpostId().getVerdi());

        var fagsakOpt = fagsakRepository.hentSakGittSaksnummer(dto.saksnummer());
        if (fagsakOpt.isEmpty()) {
            return feil(Response.Status.NOT_FOUND, "Fant ikke fagsak for saksnummer: " + dto.saksnummer().getVerdi());
        }
        Fagsak fagsak = fagsakOpt.get();
        JournalpostId journalpostId = dto.journalpostId().getJournalpostId();

        List<MottattDokument> dokumenter = mottatteDokumentRepository.hentMottatteDokument(fagsak.getId(), List.of(journalpostId));
        var dokumentSettFeil = validerDokumentSett(dokumenter, dto.saksnummer(), journalpostId);
        if (dokumentSettFeil.isPresent()) return dokumentSettFeil.get();

        MottattDokument mottattDokument = dokumenter.getFirst();
        log.info("Fant mottatt dokument: behandlingId={}, journalpostId={}, status={}, type={}",
            mottattDokument.getBehandlingId(), journalpostId.getVerdi(), mottattDokument.getStatus(), mottattDokument.getType());

        var dokumentFeil = validerDokumentForMakulering(mottattDokument, journalpostId);
        if (dokumentFeil.isPresent()) return dokumentFeil.get();

        var behandlingFeil = validerBehandlingÅpen(mottattDokument.getBehandlingId());
        if (behandlingFeil.isPresent()) return behandlingFeil.get();

        // --- Utfør makulering ---
        Long behandlingId = mottattDokument.getBehandlingId();
        DatoIntervallEntitet periode = parseSøknadsperiode(mottattDokument, journalpostId, behandlingId);
        ugyldiggjørDokument(mottattDokument, dto.begrunnelse().getTekst(), journalpostId);
        prosessTriggereRepository.fjern(behandlingId, BehandlingÅrsakType.NY_SØKT_PERIODE, periode);
        opprettTilbakeTilStartTask(fagsak, behandlingId);
        loggDiagnostikk(fagsak.getId(), journalpostId, behandlingId, periode, dto.begrunnelse().getTekst());

        log.info("Makulering fullført for saksnummer={}, journalpostId={}, behandlingId={}: dokument=UGYLDIG, trigger-fjerning forsøkt for periode={}",
            dto.saksnummer().getVerdi(), journalpostId.getVerdi(), behandlingId, periode);

        return Response.ok().build();
    }

    private Optional<Response> validerDokumentSett(List<MottattDokument> dokumenter, Saksnummer saksnummer, JournalpostId journalpostId) {
        if (dokumenter.isEmpty()) {
            return Optional.of(feil(Response.Status.NOT_FOUND,
                "Fant ingen dokumenter for saksnummer " + saksnummer.getVerdi() + " og journalpostId " + journalpostId.getVerdi()));
        }
        if (dokumenter.size() > 1) {
            return Optional.of(feil(Response.Status.BAD_REQUEST,
                "Forventet maks 1 dokument, men fant " + dokumenter.size()));
        }
        return Optional.empty();
    }

    private Optional<Response> validerDokumentForMakulering(MottattDokument dokument, JournalpostId journalpostId) {
        if (!TILLATT_BREVKODE_MAKULERING.contains(dokument.getType())) {
            return Optional.of(feil(Response.Status.BAD_REQUEST,
                "Dokumentet har brevkode " + dokument.getType() + ", makulering av duplikat er bare støttet for brevkodene " + TILLATT_BREVKODE_MAKULERING));
        }
        if (dokument.getStatus() != DokumentStatus.GYLDIG) {
            return Optional.of(feil(Response.Status.BAD_REQUEST,
                "Forventet at dokument har status GYLDIG (ferdigbehandlet søknad), men hadde: " + dokument.getStatus()));
        }
        if (dokument.getBehandlingId() == null) {
            return Optional.of(feil(Response.Status.BAD_REQUEST,
                "Dokumentet med journalpostId=" + journalpostId.getVerdi() + " har ingen tilknyttet behandlingId"));
        }
        return Optional.empty();
    }

    private Optional<Response> validerBehandlingÅpen(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        if (behandling.getStatus().erFerdigbehandletStatus()) {
            return Optional.of(feil(Response.Status.CONFLICT,
                "Behandling " + behandlingId + " er allerede ferdigbehandlet (status=" + behandling.getStatus() + "), kan ikke makulere søknad"));
        }
        return Optional.empty();
    }

    private DatoIntervallEntitet parseSøknadsperiode(MottattDokument dokument, JournalpostId journalpostId, Long behandlingId) {
        log.info("Reparser søknad for journalpostId={} for å utlede periode for trigger-fjerning", journalpostId.getVerdi());
        Søknad søknad = søknadParser.parseSøknad(dokument);
        var søknadsperiode = ((Ungdomsytelse) søknad.getYtelse()).getSøknadsperiode();
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiode.getFraOgMed(), søknadsperiode.getTilOgMed());
        log.info("Utledet søknadsperiode={} fra journalpostId={} for behandlingId={}", periode, journalpostId.getVerdi(), behandlingId);
        return periode;
    }

    private void ugyldiggjørDokument(MottattDokument dokument, String begrunnelse, JournalpostId journalpostId) {
        String formatertBegrunnelse = "Makulert som duplikat søknad av teknisk forvaltning. Begrunnelse: %s".formatted(begrunnelse);
        dokument.setFeilmeldingOgOppdaterStatus(formatertBegrunnelse);
        mottatteDokumentRepository.oppdater(dokument);
        log.info("Satt dokument journalpostId={} fra status GYLDIG til UGYLDIG.", journalpostId.getVerdi());
    }

    private void opprettTilbakeTilStartTask(Fagsak fagsak, Long behandlingId) {
        log.info("Oppretter TilbakeTilStartBehandlingTask for behandlingId={} (manueltOpprettet=true)", behandlingId);
        ProsessTaskData prosessTaskData = ProsessTaskData.forProsessTask(TilbakeTilStartBehandlingTask.class);
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskData.setBehandling(fagsak.getId(), behandlingId, fagsak.getAktørId().getId());
        prosessTaskData.setProperty(TilbakeTilStartBehandlingTask.PROPERTY_MANUELT_OPPRETTET, Boolean.toString(true));
        taskTjeneste.lagre(prosessTaskData);
        log.info("Opprettet TilbakeTilStartBehandlingTask for behandlingId={}", behandlingId);
    }

    private void loggDiagnostikk(Long fagsakId, JournalpostId journalpostId, Long behandlingId, DatoIntervallEntitet periode, String begrunnelse) {
        String diagnostikkTekst = ("Makulert duplikat søknad (journalpostId=%s): dokument satt til UGYLDIG, tilhørende prosesstrigger " +
            "(NY_SØKT_PERIODE, periode=%s) forsøkt fjernet, behandling sendt tilbake til start. Begrunnelse: %s")
            .formatted(journalpostId.getVerdi(), periode, begrunnelse);
        entityManager.persist(new DiagnostikkFagsakLogg(fagsakId, "/makuler-duplikat-soknad", diagnostikkTekst));
        entityManager.flush();
    }

    private Response feil(Response.Status status, String melding) {
        log.warn(melding);
        return Response.status(status).entity(melding).build();
    }

    public record MarkerDokumentUgyldigRequest(
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
        @NotNull
        JournalpostId journalpostId,

        @StandardAbacAttributt(StandardAbacAttributtType.SAKSNUMMER)
        @JsonProperty(value = SaksnummerDto.NAME, required = true)
        @NotNull
        @Valid
        Saksnummer saksnummer,

        @NotNull
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class)
        KortTekst begrunnelse
    ) {}


}
