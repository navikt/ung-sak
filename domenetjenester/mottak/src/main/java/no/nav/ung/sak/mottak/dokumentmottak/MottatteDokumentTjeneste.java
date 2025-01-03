package no.nav.ung.sak.mottak.dokumentmottak;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;

@Dependent
public class MottatteDokumentTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(MottatteDokumentTjeneste.class);

    private MottatteDokumentRepository mottatteDokumentRepository;

    @Inject
    public MottatteDokumentTjeneste(MottatteDokumentRepository mottatteDokumentRepository) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;

    }


    Long lagreMottattDokumentPåFagsak(MottattDokument dokument) {
        var eksisterende = mottatteDokumentRepository.hentMottatteDokument(dokument.getFagsakId(), dokument.getJournalpostId() != null ? List.of(dokument.getJournalpostId()) : List.of())
            .stream()
            .filter(it -> Objects.equals(it.getType(), dokument.getType()))
            .findFirst();
        if (eksisterende.isPresent()) {
            logger.info("Dokument med journalpostId {} er allerede lagret på fagsak.", dokument.getJournalpostId());
            return eksisterende.get().getId();
        }

        DokumentStatus nyStatus = DokumentStatus.MOTTATT;
        if (dokument.getStatus() == DokumentStatus.UGYLDIG) {
            logger.info("Mottok ugyldig dokument med jounalpostId={} på fagsak={}", dokument.getJournalpostId().getVerdi(), dokument.getFagsakId());
            nyStatus = DokumentStatus.UGYLDIG;
        }
        MottattDokument mottattDokument = mottatteDokumentRepository.lagre(dokument, nyStatus);
        return mottattDokument.getId();
    }

    List<MottattDokument> hentMottatteDokumentPåFagsak(long fagsakId, boolean taSkriveLås, DokumentStatus... statuser) {
        return mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(fagsakId, taSkriveLås, statuser);
    }

    void oppdaterStatus(List<MottattDokument> mottatteDokumenter, DokumentStatus nyStatus) {
        mottatteDokumentRepository.oppdaterStatus(mottatteDokumenter, nyStatus);
    }

}
