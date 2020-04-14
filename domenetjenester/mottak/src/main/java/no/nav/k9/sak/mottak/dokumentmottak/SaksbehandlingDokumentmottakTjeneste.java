package no.nav.k9.sak.mottak.dokumentmottak;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.dokument.DokumentKategori;
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

        MottattDokument.Builder builder = new MottattDokument.Builder()
            .medDokumentKategori(saksdokument.getDokumentKategori() != null ? saksdokument.getDokumentKategori() : DokumentKategori.UDEFINERT)
            .medMottattDato(LocalDate.parse(saksdokument.getForsendelseMottatt().toString()))
            .medPayload(saksdokument.getPayload())
            .medFagsakId(saksdokument.getFagsakId())
            .medJournalFørendeEnhet(saksdokument.getJournalEnhet());

        if (saksdokument.getForsendelseMottattTidspunkt() == null) {
            builder.medMottattTidspunkt(LocalDateTime.now());
        } else {
            builder.medMottattTidspunkt(LocalDateTime.parse(saksdokument.getForsendelseMottattTidspunkt().toString()));
        }
        if (saksdokument.getKanalreferanse() != null) {
            builder.medKanalreferanse(saksdokument.getKanalreferanse());
        }

        if (saksdokument.getJournalpostId() != null) {
            builder.medJournalPostId(new JournalpostId(saksdokument.getJournalpostId().getVerdi()));
        }
        builder.medDokumentTypeId(saksdokument.getDokumentTypeId());
        if (saksdokument.getForsendelseId() != null) {
            builder.medForsendelseId(UUID.fromString(saksdokument.getForsendelseId().toString()));
        }
        MottattDokument mottattDokument = builder.build();

        Long mottattDokumentId = mottatteDokumentTjeneste.lagreMottattDokumentPåFagsak(mottattDokument);

        ProsessTaskData prosessTaskData = new ProsessTaskData(HåndterMottattDokumentTaskProperties.TASKTYPE);
        prosessTaskData.setFagsakId(saksdokument.getFagsakId());
        prosessTaskData.setProperty(HåndterMottattDokumentTaskProperties.MOTTATT_DOKUMENT_ID_KEY, mottattDokumentId.toString());
        settÅrsakHvisDefinert(saksdokument.getBehandlingÅrsakType(), prosessTaskData);
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }

    private void settÅrsakHvisDefinert(BehandlingÅrsakType behandlingÅrsakType, ProsessTaskData prosessTaskData) {
        if (behandlingÅrsakType != null && !BehandlingÅrsakType.UDEFINERT.equals(behandlingÅrsakType)) {
            prosessTaskData.setProperty(HåndterMottattDokumentTaskProperties.BEHANDLING_ÅRSAK_TYPE_KEY, behandlingÅrsakType.getKode());
        }
    }
}
