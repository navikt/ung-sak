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


    public PapirSøknadRestTjeneste() {// For Rest-CDI

    }

    @Inject
    public PapirSøknadRestTjeneste(
        TilJournalføringTjeneste journalføringTjeneste,
        @FagsakYtelseTypeRef(UNGDOMSYTELSE) UngdomsytelseSøknadMottaker ungdomsytelseSøknadMottaker,
        PersoninfoAdapter personinfoAdapter,
        PapirsøknadHåndteringTjeneste papirsøknadHåndteringTjeneste, ArkivTjeneste arkivTjeneste) {
        this.journalføringTjeneste = journalføringTjeneste;
        this.ungdomsytelseSøknadMottaker = ungdomsytelseSøknadMottaker;
        this.personinfoAdapter = personinfoAdapter;
        this.papirsøknadHåndteringTjeneste = papirsøknadHåndteringTjeneste;
        this.arkivTjeneste = arkivTjeneste;
    }

    @POST
    @Path("/steg-1/hent-deltakerident-fra-journalpost")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter deltakerIdent fra journalpostId", summary = ("Henter personIdent fra journalpostId"), tags = PAPIRSØKNAD_TAG)
    @BeskyttetRessurs(action = BeskyttetRessursActionType.READ, resource = BeskyttetRessursResourceType.DRIFT)
    public String hentPersonIdent(@NotNull @QueryParam("journalpostId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) JournalpostId journalpostId) {
        JournalpostInfo journalpostInfo = arkivTjeneste.hentJournalpostInfo(journalpostId);
        AktørId aktørId = journalpostInfo.getAktørId().orElseThrow(() -> new IllegalArgumentException("Finner ikke aktørId for journalpost"));
        PersonIdent personIdent = personinfoAdapter.hentIdentForAktørId(aktørId).orElseThrow(() -> new IllegalArgumentException("Finner ikke personIdent for aktørId"));
        return """
            {
              "steg-2": "Sjekk deltakerens mottatte dokument i Gosys.",
              "deltakerIdent": "%s"
            }
            """.formatted(personIdent.getIdent());
    }

    @POST
    @Path("/steg-3/journalfør-papir-søknad-mot-fagsak")
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
    @Path("/steg-4/send-inn-papirsøknadopplysninger")
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
