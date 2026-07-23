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
import no.nav.k9.felles.sikkerhet.abac.*;
import no.nav.ung.kodeverk.abac.StandardAbacAttributt;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.kontrakt.KortTekst;
import no.nav.ung.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.logg.DiagnostikkFagsakLogg;
import no.nav.ung.sak.web.server.abac.AbacAttributtEmptySupplier;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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

    private FagsakRepository fagsakRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private EntityManager entityManager;

    public ForvaltningMottattDokumentRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public ForvaltningMottattDokumentRestTjeneste(FagsakRepository fagsakRepository,
                                                  MottatteDokumentRepository mottatteDokumentRepository,
                                                  EntityManager entityManager) {
        this.fagsakRepository = fagsakRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
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
