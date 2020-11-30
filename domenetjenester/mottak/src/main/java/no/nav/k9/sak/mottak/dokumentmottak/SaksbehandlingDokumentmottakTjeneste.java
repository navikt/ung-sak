package no.nav.k9.sak.mottak.dokumentmottak;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
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

    public void dokumenterAnkommet(Collection<InngåendeSaksdokument> saksdokumenter) {

        int antallOk = 0;
        int antall = 0;

        var fagsakIder = saksdokumenter.stream().map(s -> s.getFagsakId()).collect(Collectors.toSet());
        if (fagsakIder.size() != 1) {
            throw new UnsupportedOperationException("Kan kun angi saksdokumenter for samme fagsak. Fikk: " + fagsakIder);
        }
        var fagsakId = fagsakIder.iterator().next();

        Set<String> mottattDokumentIder = new LinkedHashSet<>();

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
            builder.medKanalreferanse(saksdokument.getKanalreferanse());

            if (saksdokument.getJournalpostId() != null) {
                builder.medJournalPostId(new JournalpostId(saksdokument.getJournalpostId().getVerdi()));
            }
            MottattDokument mottattDokument = builder.build();

            boolean ok = valider(mottattDokument, saksdokument.getFagsakYtelseType());
            if (ok) {
                antallOk++;
            }

            Long mottattDokumentId = mottatteDokumentTjeneste.lagreMottattDokumentPåFagsak(mottattDokument);
            mottattDokumentIder.add(mottattDokumentId.toString());
        }

        if (antallOk == antall) {
            var prosessTaskData = new ProsessTaskData(HåndterMottattDokumentTask.TASKTYPE);
            prosessTaskData.setFagsakId(fagsakId);
            prosessTaskData.setProperty(HåndterMottattDokumentTask.MOTTATT_DOKUMENT_ID_KEY, String.join(",", mottattDokumentIder));
            prosessTaskData.setCallIdFraEksisterende();
            prosessTaskRepository.lagre(prosessTaskData);
        } else if (antallOk > 0 && antallOk < antall) {
            // blanda drops
            throw new IllegalArgumentException("Fikk noe gyldig [" + antallOk + "] og noen ugyldige [" + (antall - antallOk) + "] meldinger, av totalt [" + antall + "]. Kan ikke behandle videre.");
        }
    }

    private boolean valider(MottattDokument m, FagsakYtelseType ytelseType) {
        boolean valid = true;
        var dokumentmottaker = finnMottaker(m.getType().getKode(), ytelseType);
        try {
            dokumentmottaker.validerDokument(m, ytelseType);
        } catch (MottattInntektsmeldingException e) {
            String feilmelding = toFeilmelding(e);
            // skriver på feilmelding
            m.setFeilmelding(feilmelding);
            e.getFeil().log(log);
            valid = false;
        }

        return valid;
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

    private Dokumentmottaker finnMottaker(String brevkode, FagsakYtelseType fagsakYtelseType) {
        String fagsakYtelseTypeKode = fagsakYtelseType.getKode();
        Instance<Dokumentmottaker> selected = mottakere.select(new DokumentGruppeRef.DokumentGruppeRefLiteral(brevkode));

        return FagsakYtelseTypeRef.Lookup.find(selected, fagsakYtelseType)
            .orElseThrow(() -> new IllegalStateException("Har ikke Dokumentmottaker for ytelseType=" + fagsakYtelseTypeKode + ", dokumentgruppe=" + brevkode));
    }
}
