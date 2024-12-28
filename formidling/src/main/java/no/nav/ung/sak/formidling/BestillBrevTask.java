package no.nav.ung.sak.formidling;

import java.util.List;
import java.util.UUID;

import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.IdType;
import no.nav.ung.kodeverk.formidling.RolleType;
import no.nav.ung.sak.behandling.revurdering.etterkontroll.saksbehandlingstid.ForsinketSaksbehandlingEtterkontrollOppretterTask;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.formidling.dokarkiv.DokArkivKlient;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostRequest;
import no.nav.ung.sak.formidling.dokdist.DokDistKlient;
import no.nav.ung.sak.formidling.domene.GenerertBrev;
import no.nav.ung.sak.formidling.dto.Brevbestilling;
import no.nav.ung.sak.formidling.dto.PartRequestDto;

/**
 * https://confluence.adeo.no/pages/viewpage.action?pageId=377701645
 * <p>
 * https://dokarkiv-q2.dev.intern.nav.no/swagger-ui/index.html#/
 */
//@ApplicationScoped
@ProsessTask(value = ForsinketSaksbehandlingEtterkontrollOppretterTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class BestillBrevTask implements ProsessTaskHandler {


    private BrevGenerererTjeneste brevGenerererTjeneste;
    private DokArkivKlient dokArkivKlient;
    private DokDistKlient dokDistKlient;

    // @Inject
    public BestillBrevTask(BrevGenerererTjeneste brevGenerererTjeneste, DokArkivKlient dokArkivKlient, DokDistKlient dokDistKlient) {
        this.brevGenerererTjeneste = brevGenerererTjeneste;
        this.dokArkivKlient = dokArkivKlient;
        this.dokDistKlient = dokDistKlient;
    }

    BestillBrevTask() {
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

        var dokArkivRequest = opprettJournalpostRequest(prosessTaskData, generertBrev);

        var opprettJournalpostResponse = dokArkivKlient.opprettJournalpost(dokArkivRequest);
    }

    private OpprettJournalpostRequest opprettJournalpostRequest(ProsessTaskData prosessTaskData, GenerertBrev generertBrev) {
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
            OpprettJournalpostRequest.TILLEGGSOPPLYSNING_EKSTERNREF_NOKKEL,
            prosessTaskData.getBehandlingUuid().toString());

        var sak = OpprettJournalpostRequest.Sak.forSaksnummer(prosessTaskData.getSaksnummer());

        var dokument = OpprettJournalpostRequest.Dokument.lagDokumentMedPdf(
            tittel,
            generertBrev.dokument().pdf(),
            generertBrev.malType().getKode());

        var dokArkivRequest = new OpprettJournalpostRequest(
            "UTGAAENDE",
            avsenderMottaker,
            bruker,
            OpprettJournalpostRequest.OMSORG_PLEIE_OPPLAERINGSPENGER_TEMA,
            OpprettJournalpostRequest.OMSORG_PLEIE_OPPLAERINGSPENGER_BEHANDLINGSTEMA,
            tittel,
            null,
            OpprettJournalpostRequest.AUTOMATISK_JOURNALFORENDE_ENHET,
            UUID.randomUUID().toString(), //DokumentbestillingId
            List.of(tilleggsopplysninger),
            sak,
            List.of(dokument));
        return dokArkivRequest;
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
