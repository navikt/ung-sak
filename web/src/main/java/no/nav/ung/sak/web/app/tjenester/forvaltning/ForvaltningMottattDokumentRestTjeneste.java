package no.nav.ung.sak.web.app.tjenester.forvaltning;


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
import no.nav.k9.felles.sikkerhet.abac.*;
import no.nav.ung.kodeverk.abac.StandardAbacAttributt;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.ung.sak.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Saksnummer;
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

    private FagsakRepository fagsakRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private EntityManager entityManager;

    public ForvaltningMottattDokumentRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public ForvaltningMottattDokumentRestTjeneste(FagsakRepository fagsakRepository,
                                                  MottatteDokumentRepository mottatteDokumentRepository,
                                                  MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                                  EntityManager entityManager) {
        this.fagsakRepository = fagsakRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
        this.entityManager = entityManager;
    }

    @POST
    @Path("ugyldigjor-mottatt-dokument")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "ugyldiggjør mottatt dokument ifm TSFF-2756", summary = ("ugyldiggjør mottatt dokument ifm TSFF-2756"), tags = "forvaltning")
    @Produces(JSON_UTF8)
    @BeskyttetRessurs(action = BeskyttetRessursActionType.UPDATE, resource = BeskyttetRessursResourceType.DRIFT)
    public Response ugyldigjorMottattDokument(@Valid @NotNull @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) UgyldigjorMottattDokumentRequest dto) {
        Fagsak fagsak = fagsakRepository.hentSakGittSaksnummer(dto.saksnummer.getVerdi())
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke fagsak for saksnummer: " + dto.saksnummer.getVerdi()));
        Long fagsakId = fagsak.getId();
        JournalpostId journalpostId = dto.journalpostId.getJournalpostId();
        List<MottattDokument> mottattDokuments = mottatteDokumentRepository.hentMottatteDokument(fagsakId, List.of(journalpostId));

        if (mottattDokuments.size() > 1) {
            throw new IllegalArgumentException("Forventet maks 1 dokument");
        }

        MottattDokument mottattDokument = mottattDokuments.getFirst();

        if (!Brevkode.UNGDOMSYTELSE_VARSEL_UTTALELSE.equals(mottattDokument.getType())) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Forventet brevkode " + Brevkode.UNGDOMSYTELSE_VARSEL_UTTALELSE + ", men dokumentet har: " + mottattDokument.getType())
                .build();
        }

        if (mottattDokument.getStatus() != DokumentStatus.MOTTATT) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Forventet at dokument har status MOTTATT, men hadde: " + mottattDokument.getStatus())
                .build();
        }

        mottattDokument.setFeilmeldingOgOppdaterStatus("Ugyldiggjort manuelt ifm TSFF-2756");
        mottatteDokumentTjeneste.lagreMottattDokumentPåFagsak(mottattDokument);

        entityManager.persist(new DiagnostikkFagsakLogg(
            fagsakId,
            "ForvaltningMottattDokumentRestTjeneste.ugyldigjorMottattDokument (midlertidig)",
            "Patching ifm TSFF-2756. Satt mottatt dokument for journalpost=%s til UGYLDIG."
                .formatted(journalpostId.getVerdi())
        ));

        return Response.ok().build();
    }

    public record UgyldigjorMottattDokumentRequest(
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
        JournalpostId journalpostId,

        @NotNull
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
        SaksnummerDto saksnummer
    ) {
        @StandardAbacAttributt(StandardAbacAttributtType.SAKSNUMMER)
        public Saksnummer getSaksnummer() {
            return saksnummer.getVerdi();
        }

    }


}
