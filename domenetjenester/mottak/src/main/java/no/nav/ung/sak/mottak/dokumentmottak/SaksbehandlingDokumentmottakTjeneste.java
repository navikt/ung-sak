package no.nav.ung.sak.mottak.dokumentmottak;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.typer.JournalpostId;

@Dependent
public class SaksbehandlingDokumentmottakTjeneste {

    private static final Logger log = LoggerFactory.getLogger(SaksbehandlingDokumentmottakTjeneste.class);

    private ProsessTaskTjeneste taskTjeneste;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private DokumentValidatorProvider dokumentValidatorProvider;

    SaksbehandlingDokumentmottakTjeneste() {
        // for CDI, jaja
    }

    @Inject
    public SaksbehandlingDokumentmottakTjeneste(ProsessTaskTjeneste taskTjeneste,
                                                DokumentValidatorProvider dokumentValidatorProvider,
                                                MottatteDokumentTjeneste mottatteDokumentTjeneste) {
        this.taskTjeneste = taskTjeneste;
        this.dokumentValidatorProvider = dokumentValidatorProvider;
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
    }

    public void dokumenterAnkommet(Collection<InngåendeSaksdokument> saksdokumenter) {

        int antallOk = 0;
        int antall = 0;

        var fagsakIder = saksdokumenter.stream().map(InngåendeSaksdokument::getFagsakId).collect(Collectors.toSet());
        if (fagsakIder.size() != 1) {
            throw new UnsupportedOperationException("Kan kun angi saksdokumenter for samme fagsak. Fikk: " + fagsakIder);
        }
        var fagsakId = fagsakIder.iterator().next();

        Set<String> mottattDokumentIder = new LinkedHashSet<>();
        Set<String> feilmeldinger = new LinkedHashSet<>();

        for (var saksdokument : saksdokumenter) {
            antall++;

            LocalDateTime mottattTidspunkt = Objects.requireNonNull(saksdokument.getForsendelseMottattTidspunkt(), "forsendelseMottattTidspunkt");
            LocalDate forsendelseMottatt = Objects.requireNonNull(saksdokument.getForsendelseMottatt(), "forsendelseMottattDato");
            var builder = new MottattDokument.Builder()
                .medMottattDato(LocalDate.parse(forsendelseMottatt.toString()))
                .medPayload(saksdokument.getPayload())
                .medType(saksdokument.getType())
                .medFagsakId(saksdokument.getFagsakId());

            builder.medMottattTidspunkt(mottattTidspunkt);

            if (saksdokument.getJournalpostId() != null) {
                builder.medJournalPostId(new JournalpostId(saksdokument.getJournalpostId().getVerdi()));
            }
            MottattDokument mottattDokument = builder.build();

            boolean ok = valider(mottattDokument);
            if (ok) {
                antallOk++;
            } else {
                feilmeldinger.add(mottattDokument.getFeilmelding());
            }

            Long mottattDokumentId = mottatteDokumentTjeneste.lagreMottattDokumentPåFagsak(mottattDokument);
            mottattDokumentIder.add(mottattDokumentId.toString());
        }

        if (antallOk == antall) {
            var prosessTaskData = ProsessTaskData.forProsessTask(HåndterMottattDokumentTask.class);
            prosessTaskData.setFagsakId(fagsakId);
            prosessTaskData.setProperty(HåndterMottattDokumentTask.MOTTATT_DOKUMENT_ID_KEY, String.join(",", mottattDokumentIder));
            prosessTaskData.setCallIdFraEksisterende();
            taskTjeneste.lagre(prosessTaskData);
        } else {
            //Hvis noe er ugyldig skal det feile i systemet som sender inn (fordel eller punsj)
            String melding = "Fikk [" + (antall - antallOk) + "] ugyldige dokumenter, av totalt [" + antall + "]. Kan ikke behandle videre.";
            if (feilmeldinger.size() == 1) {
                melding = melding + " Feilmelding: " + feilmeldinger.iterator().next();
            }
            throw new IllegalArgumentException(melding);
        }
    }

    private boolean valider(MottattDokument m) {
        DokumentValidator dokumentValidator = dokumentValidatorProvider.finnValidator(m.getType());
        try {
            dokumentValidator.validerDokument(m);
        } catch (DokumentValideringException e) {
            String feilmelding = toFeilmelding(e);
            m.setFeilmeldingOgOppdaterStatus(feilmelding);
            log.warn(e.getMessageWithoutLinebreaks(), e);
            return false;
        }
        return true;
    }

    private String toFeilmelding(DokumentValideringException e) {
        var sw = new StringWriter(1000);
        try (var pw = new PrintWriter(sw, true)) {
            e.printStackTrace(pw);
            pw.flush();
            return String.format("%s\n%s", e.getMessageWithoutLinebreaks(), sw);
        }
    }

}
