package no.nav.k9.sak.mottak.dokumentmottak;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@Dependent
public class SaksbehandlingDokumentmottakTjeneste {

    private ProsessTaskRepository prosessTaskRepository;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;

    public SaksbehandlingDokumentmottakTjeneste() {
        // for CDI, jaja
    }

    @Inject
    public SaksbehandlingDokumentmottakTjeneste(ProsessTaskRepository prosessTaskRepository,
                                                MottatteDokumentTjeneste mottatteDokumentTjeneste) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
    }

    public void dokumentAnkommet(InngåendeSaksdokument saksdokument) {

        var builder = new MottattDokument.Builder()
            .medMottattDato(LocalDate.parse(saksdokument.getForsendelseMottatt().toString()))
            .medPayload(saksdokument.getPayload())
            .medType(saksdokument.getType())
            .medFagsakId(saksdokument.getFagsakId());

        if (saksdokument.getForsendelseMottattTidspunkt() == null) {
            builder.medMottattTidspunkt(LocalDateTime.now());
        } else {
            builder.medMottattTidspunkt(LocalDateTime.parse(saksdokument.getForsendelseMottattTidspunkt().toString()));
        }
        builder.medKanalreferanse(saksdokument.getKanalreferanse());

        if (saksdokument.getJournalpostId() != null) {
            builder.medJournalPostId(new JournalpostId(saksdokument.getJournalpostId().getVerdi()));
        }
        MottattDokument mottattDokument = builder.build();

        Long mottattDokumentId = mottatteDokumentTjeneste.lagreMottattDokumentPåFagsak(mottattDokument);

        var prosessTaskData = new ProsessTaskData(HåndterMottattDokumentTask.TASKTYPE);
        prosessTaskData.setFagsakId(saksdokument.getFagsakId());
        prosessTaskData.setProperty(HåndterMottattDokumentTask.MOTTATT_DOKUMENT_ID_KEY, mottattDokumentId.toString());
        settÅrsakHvisDefinert(saksdokument.getBehandlingÅrsakType(), prosessTaskData);
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }

    private void settÅrsakHvisDefinert(BehandlingÅrsakType behandlingÅrsakType, ProsessTaskData prosessTaskData) {
        if (behandlingÅrsakType != null && !BehandlingÅrsakType.UDEFINERT.equals(behandlingÅrsakType)) {
            prosessTaskData.setProperty(HåndterMottattDokumentTask.BEHANDLING_ÅRSAK_TYPE_KEY, behandlingÅrsakType.getKode());
        }
    }
}
