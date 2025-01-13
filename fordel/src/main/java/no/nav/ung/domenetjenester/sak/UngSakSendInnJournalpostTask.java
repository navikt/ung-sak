package no.nav.ung.domenetjenester.sak;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.log.mdc.MdcExtendedLogContext;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.fordel.repo.journalpost.JournalpostInnsendingEntitet;
import no.nav.ung.fordel.repo.journalpost.JournalpostRepository;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.FagsakTjeneste;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.kontrakt.mottak.JournalpostMottakDto;
import no.nav.ung.sak.mottak.dokumentmottak.InngåendeSaksdokument;
import no.nav.ung.sak.mottak.dokumentmottak.SaksbehandlingDokumentmottakTjeneste;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Saksnummer;

@ApplicationScoped
@ProsessTask(value = UngSakSendInnJournalpostTask.TASKTYPE, maxFailedRuns = 1)
public class UngSakSendInnJournalpostTask implements ProsessTaskHandler {
    private static final Logger log = LoggerFactory.getLogger(UngSakSendInnJournalpostTask.class);

    private static final MdcExtendedLogContext LOG_CONTEXT = MdcExtendedLogContext.getContext("prosess");

    public static final String TASKTYPE = "ung.innsending.journalposter";

    public static final String PROPERTY_YTELSE_TYPE = "ytelseType";

    private JournalpostRepository innsendingRepository;
    private FagsakTjeneste fagsakTjeneste;
    private SaksbehandlingDokumentmottakTjeneste dokumentMottakTjeneste;

    protected UngSakSendInnJournalpostTask() {
        // for proxy
    }

    @Inject
    public UngSakSendInnJournalpostTask(JournalpostRepository innsendingRepository, FagsakTjeneste fagsakTjeneste, SaksbehandlingDokumentmottakTjeneste dokumentMottakTjeneste) {
        this.innsendingRepository = innsendingRepository;
        this.fagsakTjeneste = fagsakTjeneste;
        this.dokumentMottakTjeneste = dokumentMottakTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {

        Saksnummer saksnummer = new Saksnummer(Objects.requireNonNull(prosessTaskData.getSaksnummer(), "Mangler saksnummer"));
        FagsakYtelseType ytelseType = FagsakYtelseType
                .fraKode(Objects.requireNonNull(prosessTaskData.getPropertyValue(PROPERTY_YTELSE_TYPE), "Mangler ytelseType"));

        leggTilMdc("saksnummer", saksnummer.getVerdi());
        leggTilMdc("ytelseType", ytelseType.getKode());

        List<JournalpostInnsendingEntitet> journalposterKlarForInnsending = innsendingRepository.markerOgHentJournalposterKlarForInnsending(
                ytelseType,
                new Saksnummer(saksnummer.getVerdi())
        );

        List<JournalpostMottakDto> dtoer = new ArrayList<>();
        for (var journalpost : journalposterKlarForInnsending) {
            dtoer.add(new JournalpostMottakDto(
                    saksnummer,
                    new JournalpostId(journalpost.getJournalpostId().getVerdi()),
                    ytelseType,
                    journalpost.getBrevkode(),
                    journalpost.getInnsendingstidspunkt(),
                    journalpost.getPayload()));
        }

        if (!dtoer.isEmpty()) {
            List<InngåendeSaksdokument> saksdokumenter = dtoer.stream()
                .map(this::mapJournalpost)
                .sorted(Comparator.comparing(InngåendeSaksdokument::getForsendelseMottatt))
                .collect(Collectors.toList());

            dokumentMottakTjeneste.dokumenterAnkommet(saksdokumenter);
            log.info("Sendt inn dokumenter OK til ung-sak for sak={}, behandlingstema={}, journalposter={} og behandling håndteres videre i ung-sak.",
                    saksnummer, ytelseType, getJournalpostIds(journalposterKlarForInnsending));
        } else {
            log.info("Ingenting å sende inn, allerede håndtert for sak={}, behandlingstema={}.", saksnummer, ytelseType);
        }

    }

    private InngåendeSaksdokument mapJournalpost(JournalpostMottakDto mottattJournalpost) {
        Saksnummer saksnummer = mottattJournalpost.getSaksnummer();
        Optional<Fagsak> fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, false);
        if (fagsak.isEmpty()) {
            throw new IllegalStateException("Finner ingen fagsak for saksnummer " + saksnummer);
        }
        var f = fagsak.get();

        String payload = mottattJournalpost.getBase64EncodedPayload();
        InngåendeSaksdokument.Builder builder = InngåendeSaksdokument.builder()
            .medFagsak(f.getId(), f.getYtelseType())
            .medElektroniskSøknad(true)
            .medType(mottattJournalpost.getType())
            .medJournalpostId(mottattJournalpost.getJournalpostId());

        builder.medPayload(payload);

        LocalDateTime mottattTidspunkt = Optional.ofNullable(mottattJournalpost.getForsendelseMottattTidspunkt())
            .orElseThrow(() -> new IllegalArgumentException("Mangler forsendelseMottattTidspunkt"));
        builder.medForsendelseMottatt(mottattTidspunkt); // NOSONAR
        builder.medForsendelseMottatt(mottattJournalpost.getForsendelseMottatt().orElse(mottattTidspunkt.toLocalDate())); // NOSONAR

        return builder.build();
    }



    private void leggTilMdc(String key, String val) {
        LOG_CONTEXT.add(key, val);
    }

    private List<JournalpostId> getJournalpostIds(List<JournalpostInnsendingEntitet> journalposter) {
        return journalposter.stream()
                .map(JournalpostInnsendingEntitet::getJournalpostId)
                .map(jp -> new JournalpostId(jp.getJournalpostId().getVerdi()))
                .toList();
    }
}
