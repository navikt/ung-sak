package no.nav.ung.sak.formidling;

import java.util.List;
import java.util.UUID;

import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.IdType;
import no.nav.ung.kodeverk.formidling.RolleType;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.formidling.dokarkiv.DokArkivKlient;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostRequest;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostRequestBuilder;
import no.nav.ung.sak.formidling.dokdist.DokDistKlient;
import no.nav.ung.sak.formidling.domene.BehandlingBrevbestillingEntitet;
import no.nav.ung.sak.formidling.domene.BrevMottaker;
import no.nav.ung.sak.formidling.domene.BrevbestillingEntitetBuilder;
import no.nav.ung.sak.formidling.domene.GenerertBrev;
import no.nav.ung.sak.formidling.dto.Brevbestilling;
import no.nav.ung.sak.formidling.dto.PartRequestDto;

/**
 * https://confluence.adeo.no/pages/viewpage.action?pageId=377701645
 * <p>
 * https://dokarkiv-q2.dev.intern.nav.no/swagger-ui/index.html#/
 */
//@ApplicationScoped
@ProsessTask(value = BrevbestillingTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class BrevbestillingTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "formidling.brevbestlling";

    private BrevGenerererTjeneste brevGenerererTjeneste;
    private BrevbestillingRepository brevbestillingRepository;
    private DokArkivKlient dokArkivKlient;
    private DokDistKlient dokDistKlient;

    // @Inject
    public BrevbestillingTask(
        BrevGenerererTjeneste brevGenerererTjeneste,
        BrevbestillingRepository brevbestillingRepository,
        DokArkivKlient dokArkivKlient,
        DokDistKlient dokDistKlient) {
        this.brevGenerererTjeneste = brevGenerererTjeneste;
        this.brevbestillingRepository = brevbestillingRepository;
        this.dokArkivKlient = dokArkivKlient;
        this.dokDistKlient = dokDistKlient;
    }

    BrevbestillingTask() {
    }


    @Override
    public void doTask(ProsessTaskData prosessTaskData) {

        var generertBrev = brevGenerererTjeneste.generer(
            new Brevbestilling(
                Long.valueOf(prosessTaskData.getBehandlingId()),
                DokumentMalType.INNVILGELSE_DOK,
                prosessTaskData.getSaksnummer(),
                new PartRequestDto(prosessTaskData.getAktørId(), IdType.AKTØRID, RolleType.BRUKER),
                null)
        );

        var bestilling = new BrevbestillingEntitetBuilder()
            .dokumentMalType(DokumentMalType.INNVILGELSE_DOK)
            .mottaker(new BrevMottaker(prosessTaskData.getAktørId(), IdType.AKTØRID))
            .saksnummer(prosessTaskData.getSaksnummer())
            .build();

        var behandlingBestilling = new BehandlingBrevbestillingEntitet(
            Long.valueOf(prosessTaskData.getBehandlingId()),
            true,
            bestilling
        );

        brevbestillingRepository.lagre(behandlingBestilling);

        var dokArkivRequest = opprettJournalpostRequest(prosessTaskData, bestilling.getBrevbestillingUuid(), generertBrev);
        var opprettJournalpostResponse = dokArkivKlient.opprettJournalpost(dokArkivRequest);
        //TODO vurder å putte templateType i builder istedenfor her...
        bestilling.generertOgJournalført(generertBrev.templateType(), opprettJournalpostResponse.journalpostId());

        brevbestillingRepository.lagre(behandlingBestilling);
    }

    private OpprettJournalpostRequest opprettJournalpostRequest(ProsessTaskData prosessTaskData, UUID brevbestillingUuid, GenerertBrev generertBrev) {
        String tittel = utledTittel(generertBrev.malType());

        var avsenderMottaker = new OpprettJournalpostRequest.AvsenderMottaker(
            generertBrev.mottaker().fnr(),
            null,
            null,
            OpprettJournalpostRequest.AvsenderMottaker.IdType.FNR

        );
        var bruker = new OpprettJournalpostRequest.Bruker(
            generertBrev.mottaker().fnr(),
            OpprettJournalpostRequest.Bruker.BrukerIdType.FNR
        );

        var tilleggsopplysninger = new OpprettJournalpostRequest.Tilleggsopplysning(
            "ung.formidling.eRef",
            prosessTaskData.getBehandlingUuid().toString());

        var sak = OpprettJournalpostRequest.Sak.forSaksnummer(prosessTaskData.getSaksnummer());

        var dokument = OpprettJournalpostRequest.Dokument.lagDokumentMedPdf(
            tittel,
            generertBrev.dokument().pdf(),
            generertBrev.malType().getKode());


        return new OpprettJournalpostRequestBuilder()
            .journalpostType("UTGAAENDE")
            .avsenderMottaker(avsenderMottaker)
            .bruker(bruker)
            .tema("OMS") //TODO endre for ung
            .behandlingstema("ab0271")
            .tittel(tittel)
            .kanal(null)
            .journalfoerendeEnhet("9999")
            .eksternReferanseId(brevbestillingUuid.toString())
            .tilleggsopplysninger(List.of(tilleggsopplysninger))
            .sak(sak)
            .dokumenter(List.of(dokument))
            .build();
    }

    private String utledTittel(DokumentMalType dokumentMalType) {
        var prefix = "Ungdomsytelse ";
        String fraMal = switch (dokumentMalType) {
            case HENLEGG_BEHANDLING_DOK -> "Henleggelse";
            case INNVILGELSE_DOK -> "Innvilgelse";
            case OPPHØR_DOK -> "Opphør";
            case AVSLAG__DOK -> "Avslag";
            case MANUELT_VEDTAK_DOK -> "Fritekstvedtak";
        };
        return prefix + fraMal;
    }
}
