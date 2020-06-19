package no.nav.k9.sak.mottak.dokumentmottak;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.mottak.inntektsmelding.MottattInntektsmeldingException;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@Dependent
public class SaksbehandlingDokumentmottakTjeneste {

    private static final Logger log = LoggerFactory.getLogger(SaksbehandlingDokumentmottakTjeneste.class);

    private ProsessTaskRepository prosessTaskRepository;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private Instance<Dokumentmottaker> mottakere;

    public SaksbehandlingDokumentmottakTjeneste() {
        // for CDI, jaja
    }

    @Inject
    public SaksbehandlingDokumentmottakTjeneste(ProsessTaskRepository prosessTaskRepository,
                                                @Any Instance<Dokumentmottaker> mottakere,
                                                MottatteDokumentTjeneste mottatteDokumentTjeneste) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.mottakere = mottakere;
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

        boolean ok = valider(mottattDokument, saksdokument.getFagsakYtelseType());

        Long mottattDokumentId = mottatteDokumentTjeneste.lagreMottattDokumentPåFagsak(mottattDokument);

        if (ok) {
            var prosessTaskData = new ProsessTaskData(HåndterMottattDokumentTask.TASKTYPE);
            prosessTaskData.setFagsakId(saksdokument.getFagsakId());
            prosessTaskData.setProperty(HåndterMottattDokumentTask.MOTTATT_DOKUMENT_ID_KEY, mottattDokumentId.toString());
            settÅrsakHvisDefinert(saksdokument.getBehandlingÅrsakType(), prosessTaskData);
            prosessTaskData.setCallIdFraEksisterende();
            prosessTaskRepository.lagre(prosessTaskData);
        }
    }

    private boolean valider(MottattDokument mottattDokument, FagsakYtelseType ytelseType) {
        var dokumentmottaker = finnMottaker(mottattDokument.getType(), ytelseType);
        try {
            dokumentmottaker.validerDokument(mottattDokument, ytelseType);
            return true;
        } catch (MottattInntektsmeldingException e) {
            String feilmelding = toFeilmelding(e);
            // skriver på feilmelding
            mottattDokument.setFeilmelding(feilmelding);
            e.getFeil().log(log);
            return false;
        }
    }

    private String toFeilmelding(TekniskException e) {
        var sw = new StringWriter(1000);
        try (var pw = new PrintWriter(sw, true)) {
            e.printStackTrace(pw);
            pw.flush();
            var f = e.getFeil();
            return String.format("%s: %s\n%s", f.getKode(), f.getFeilmelding(), sw);
        }
    }

    private void settÅrsakHvisDefinert(BehandlingÅrsakType behandlingÅrsakType, ProsessTaskData prosessTaskData) {
        if (behandlingÅrsakType != null && !BehandlingÅrsakType.UDEFINERT.equals(behandlingÅrsakType)) {
            prosessTaskData.setProperty(HåndterMottattDokumentTask.BEHANDLING_ÅRSAK_TYPE_KEY, behandlingÅrsakType.getKode());
        }
    }

    private Dokumentmottaker finnMottaker(Brevkode brevkode, FagsakYtelseType fagsakYtelseType) {
        String fagsakYtelseTypeKode = fagsakYtelseType.getKode();
        Instance<Dokumentmottaker> selected = mottakere.select(new DokumentGruppeRef.DokumentGruppeRefLiteral(brevkode));

        return FagsakYtelseTypeRef.Lookup.find(selected, fagsakYtelseType)
            .orElseThrow(() -> new IllegalStateException("Har ikke Dokumentmottaker for ytelseType=" + fagsakYtelseTypeKode + ", dokumentgruppe=" + brevkode));
    }
}
