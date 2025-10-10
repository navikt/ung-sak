package no.nav.ung.sak.web.app.tjenester.fordeling;


import io.swagger.v3.oas.annotations.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.domenetjenester.arkiv.ArkivTjeneste;
import no.nav.ung.domenetjenester.arkiv.JournalpostInfo;
import no.nav.ung.domenetjenester.arkiv.journal.TilJournalføringTjeneste;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.produksjonsstyring.OmrådeTema;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.dokument.arkiv.DokumentArkivTjeneste;
import no.nav.ung.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostResponse;
import no.nav.ung.sak.kontrakt.søknad.JournalførPapirSøknadDto;
import no.nav.ung.sak.kontrakt.søknad.SendInnPapirsøknadopplysningerRequestDto;
import no.nav.ung.sak.mottak.dokumentmottak.UngdomsytelseSøknadMottaker;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;

import java.io.ByteArrayInputStream;
import java.util.Optional;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;
import static no.nav.ung.sak.web.app.tjenester.fordeling.PapirSøknadRestTjeneste.BASE_PATH;

@Path(BASE_PATH)
@ApplicationScoped
@Transactional
public class PapirSøknadRestTjeneste {
    static final String BASE_PATH = "/papir";
    static final String PAPIRSØKNAD_TAG = "papirsøknad";

    private TilJournalføringTjeneste journalføringTjeneste;
    private UngdomsytelseSøknadMottaker ungdomsytelseSøknadMottaker;
    private PersoninfoAdapter personinfoAdapter;
    private PapirsøknadHåndteringTjeneste papirsøknadHåndteringTjeneste;
    private ArkivTjeneste arkivTjeneste;
    private DokumentArkivTjeneste dokumentArkivTjeneste;


    public PapirSøknadRestTjeneste() {// For Rest-CDI

    }

    @Inject
    public PapirSøknadRestTjeneste(
        TilJournalføringTjeneste journalføringTjeneste,
        @FagsakYtelseTypeRef(UNGDOMSYTELSE) UngdomsytelseSøknadMottaker ungdomsytelseSøknadMottaker,
        PersoninfoAdapter personinfoAdapter,
        PapirsøknadHåndteringTjeneste papirsøknadHåndteringTjeneste, ArkivTjeneste arkivTjeneste, DokumentArkivTjeneste dokumentArkivTjeneste) {
        this.journalføringTjeneste = journalføringTjeneste;
        this.ungdomsytelseSøknadMottaker = ungdomsytelseSøknadMottaker;
        this.personinfoAdapter = personinfoAdapter;
        this.papirsøknadHåndteringTjeneste = papirsøknadHåndteringTjeneste;
        this.arkivTjeneste = arkivTjeneste;
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
    }

    @POST
    @Path("/steg-1/hent-søknad-pdf")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(description = "Henter og viser papirsøknad. Husk å slette dokumentet lokalt etter at du er ferdig.", summary = ("Henter og viser papirsøknad"), tags = PAPIRSØKNAD_TAG)
    @BeskyttetRessurs(action = BeskyttetRessursActionType.READ, resource = BeskyttetRessursResourceType.DRIFT)
    // Kan bruke drift fordi kallet mot SAF gjør tilgangskontroll uansett.
    public Response hentPapirSøknad(@NotNull @QueryParam("journalpostId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) JournalpostId journalpostId) {
        try {
            Response.ResponseBuilder responseBuilder = Response.ok(new ByteArrayInputStream(papirsøknadHåndteringTjeneste.hentDokumentForJournalpostId(journalpostId)));
            responseBuilder.header("Content-Disposition", "inline; filename=\"" + papirsøknadHåndteringTjeneste.hentFilnavnForJournalpostId(journalpostId) + "\"");
            return responseBuilder.build();
        } catch (Exception e) {
            return Response.serverError().entity("Klarte ikke å generere PDF: " + e.getMessage()).build();
        }
    }

    @POST
    @Path("/steg-2/journalfør-papir-søknad-mot-fagsak")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppretter fagsak hvis det ikke allerede finnes en, og gjøre en endelig journalføring av papirsøknaden med fagsakstilknytning.", summary = ("Oppretter fagsak og journalfører papirsøknad"), tags = PAPIRSØKNAD_TAG)
    @BeskyttetRessurs(action = BeskyttetRessursActionType.CREATE, resource = BeskyttetRessursResourceType.FAGSAK)
    public Response journalførPapirSøknad(@NotNull @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) JournalførPapirSøknadDto journalførPapirSøknadDto) {
        Periode periode = new Periode(journalførPapirSøknadDto.startDato(), null);

        AktørId aktørId = personinfoAdapter.hentAktørIdForPersonIdent(PersonIdent.fra(journalførPapirSøknadDto.deltakerIdent()))
            .orElseThrow(() -> new IllegalArgumentException("Finner ikke aktørId for deltakerIdent"));

        Fagsak fagsak = ungdomsytelseSøknadMottaker.finnEllerOpprettFagsakForIkkeDigitalBruker(FagsakYtelseType.UNGDOMSYTELSE, aktørId, periode.getFom(), periode.getTom());

        var journalpostId = journalførPapirSøknadDto.journalpostId();
        if (journalpostId != null && journalføringTjeneste.erAlleredeJournalført(journalpostId)) {
            throw new IllegalStateException("Journalpost er allerede journalført");
        } else {
            try {
                boolean ferdigJournalført = journalføringTjeneste.tilJournalføring(journalpostId, Optional.of(fagsak.getSaksnummer().getVerdi()), OmrådeTema.UNG, aktørId.getAktørId());
                if (!ferdigJournalført) {
                    throw new IllegalStateException("Journalpost kunne ikke journalføres");
                }

                String response = """
                    {
                      "saksnummer": "%s"
                    }
                    """.formatted(fagsak.getSaksnummer().getVerdi());

                return Response.ok()
                    .entity(response)
                    .build();
            } catch (Exception e) {
                return Response.serverError().entity("Kan ikke ferdigstille journalpost: " + e.getMessage()).build();
            }
        }
    }

    @POST
    @Path("/steg-3/send-inn-papirsøknadopplysninger")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Mapper til strukturert søknadsopplysninger og oppretter journalpost.", summary = ("Mapper til strukturert søknadsopplysninger og oppretter journalpost."), tags = PAPIRSØKNAD_TAG)
    @BeskyttetRessurs(action = BeskyttetRessursActionType.CREATE, resource = BeskyttetRessursResourceType.FAGSAK)
    public OpprettJournalpostResponse sendInnPapirsøknadopplysninger(@NotNull @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SendInnPapirsøknadopplysningerRequestDto dto) {
        return papirsøknadHåndteringTjeneste.journalførPapirsøknad(
            PersonIdent.fra(dto.deltakerIdent()),
            dto.startdato(),
            dto.journalpostIdForPapirsøknad()
        );
    }
}
