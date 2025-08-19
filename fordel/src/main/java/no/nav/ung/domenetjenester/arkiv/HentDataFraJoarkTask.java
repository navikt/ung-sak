package no.nav.ung.domenetjenester.arkiv;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.exception.TekniskException;
import no.nav.k9.felles.integrasjon.saf.Tema;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.ung.domenetjenester.arkiv.journalpostvurderer.AlleredeMottattJournalpost;
import no.nav.ung.domenetjenester.arkiv.journalpostvurderer.IgnorertJournalpost;
import no.nav.ung.domenetjenester.arkiv.journalpostvurderer.StrukturertJournalpost;
import no.nav.ung.domenetjenester.arkiv.journalpostvurderer.UhåndtertJournalpost;
import no.nav.ung.fordel.handler.FordelProsessTaskTjeneste;
import no.nav.ung.fordel.handler.MottattMelding;
import no.nav.ung.fordel.handler.WrappedProsessTaskHandler;
import no.nav.ung.fordel.kodeverdi.BrevkodeInformasjonUtleder;
import no.nav.ung.fordel.repo.journalpost.JournalpostMottattEntitet;
import no.nav.ung.fordel.repo.journalpost.JournalpostRepository;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;

@ApplicationScoped
@ProsessTask(value = HentDataFraJoarkTask.TASKTYPE, maxFailedRuns = 1)
public class HentDataFraJoarkTask extends WrappedProsessTaskHandler {
    public static final String TASKTYPE = "arkiv.hentData";
    private static final Logger log = LoggerFactory.getLogger(HentDataFraJoarkTask.class);

    private IgnorertJournalpost ignorertJournalpost;
    private UhåndtertJournalpost uhåndtertJournalpost;
    private AlleredeMottattJournalpost alleredeMottattJournalpost;
    private StrukturertJournalpost strukturertJournalpost;

    private ArkivTjeneste arkivTjeneste;
    private JournalpostRepository journalpostRepository;

    @Inject
    public HentDataFraJoarkTask(
        FordelProsessTaskTjeneste fordelProsessTaskTjeneste,
        ArkivTjeneste arkivTjeneste,
        JournalpostRepository journalpostRepository,
        StrukturertJournalpost strukturertJournalpost,
        AlleredeMottattJournalpost alleredeMottattJournalpost,
        IgnorertJournalpost ignorertJournalpost,
        UhåndtertJournalpost uhåndtertJournalpost) {
        super(fordelProsessTaskTjeneste);
        this.arkivTjeneste = arkivTjeneste;
        this.journalpostRepository = journalpostRepository;
        this.alleredeMottattJournalpost = alleredeMottattJournalpost;
        this.strukturertJournalpost = strukturertJournalpost;
        this.ignorertJournalpost = ignorertJournalpost;
        this.uhåndtertJournalpost = uhåndtertJournalpost;
    }

    @Override
    public void precondition(MottattMelding dataWrapper) {

    }

    @Override
    public MottattMelding doTask(MottattMelding dataWrapper) {
        var journalPostId = dataWrapper.getJournalPostId();
        var hendelsetype = dataWrapper.getJournalføringHendelsetype();
        JournalpostInfo journalpostInfo = hentOgHåndterJournalpost(dataWrapper, journalPostId);

        var vurdertJournalpost = ignorertJournalpost.vurder(dataWrapper, journalpostInfo);
        if (vurdertJournalpost.erHåndtert())
            return registerMottattJournalpostTilBehandling(vurdertJournalpost.håndtertMelding(), journalpostInfo, "ignorerer");

        vurdertJournalpost = alleredeMottattJournalpost.vurder(dataWrapper, journalpostInfo);
        if (vurdertJournalpost.erHåndtert())
            return registerMottattJournalpostTilBehandling(vurdertJournalpost.håndtertMelding(), journalpostInfo, "allerede mottatt");

        // Gjøres felles for alle journalposter som ikke er ignorerte/allerde mottatt
        var aktørId = journalpostInfo.getAktørId();
        var mottattTidspunkt = journalpostInfo.getForsendelseTidspunkt();
        dataWrapper.setAktørId(aktørId.map(AktørId::getId).orElseThrow());
        dataWrapper.setForsendelseMottattTidspunkt(mottattTidspunkt);
        dataWrapper.setPayload(journalpostInfo.getStrukturertPayload());
        dataWrapper.setBrevkode(journalpostInfo.getBrevkode());

        vurdertJournalpost = strukturertJournalpost.vurder(dataWrapper, journalpostInfo);
        if (vurdertJournalpost.erHåndtert())
            return registerMottattJournalpostTilBehandling(vurdertJournalpost.håndtertMelding(), journalpostInfo, "strukturert dokument");

        // Siste vurderingen som skal gjøres
        vurdertJournalpost = uhåndtertJournalpost.vurder(dataWrapper, journalpostInfo);
        if (vurdertJournalpost.erHåndtert())
            return registerMottattJournalpostTilBehandling(vurdertJournalpost.håndtertMelding(), journalpostInfo, "uhåndtert");

        // Forsikrer oss om at det nå kun er Journalposter av typen 'EndeligJournalført' som nå ikke er håndtert.
        if (hendelsetype.isEmpty() || hendelsetype.get() != JournalføringHendelsetype.ENDELING_JOURNALFØRT) {
            throw new IllegalStateException("Uhåndtert journalpost[" + journalPostId.getVerdi() + "] med brevkode="
                + journalpostInfo.getBrevkode());
        } else {
            return null;
        }
    }

    private JournalpostInfo hentOgHåndterJournalpost(MottattMelding dataWrapper, JournalpostId journalPostId) {
        JournalpostInfo journalpostInfo = null;
        try {
            journalpostInfo = arkivTjeneste.hentJournalpostInfo(journalPostId);
        } catch (TekniskException tekniskException) {
            Throwable cause = tekniskException.getCause();
            if (cause instanceof TekniskException) {
                // Dette er ikke en "bulletproof" håndtering av feilen, men skulle feilemeldingen endres, vil vi få en taskfeil som kan håndteres.
                String feilmelding = cause.getMessage().trim();
                if (feilmelding.contains("Avvist av SAF tilgangskontroll: Tilgang til ressurs (journalpost/dokument) ble avvist. System har ikke tilgang til tema ressursen tilhører.")) {
                    log.warn("Journalpost[{}] er Avvist av SAF tilgangskontroll: Tilgang til ressurs journalpost ble avvist. System har ikke tilgang til tema ressursen tilhører (tema endret).", journalPostId.getVerdi());

                    var journalpostInfoUtledetFraFeil = new JournalpostInfo();
                    journalpostInfoUtledetFraFeil.setJournalpostId(journalPostId.getVerdi());
                    dataWrapper.getJournalføringHendelsetype()
                        .ifPresent(journalføringHendelsetype -> journalpostInfoUtledetFraFeil.setType(journalføringHendelsetype.name()));

                    HentDataFraJoarkTask.utledTemaFraSafFeilmelding((TekniskException) cause).ifPresent(journalpostInfoUtledetFraFeil::setTema);

                    journalpostInfo = journalpostInfoUtledetFraFeil;
                } else throw tekniskException;
            } else {
                throw tekniskException;
            }
        }
        return journalpostInfo;
    }

    private MottattMelding registerMottattJournalpostTilBehandling(
        MottattMelding håndtertMelding,
        JournalpostInfo journalpostInfo,
        String kortBeskrivelseHåndtering) {
        if (håndtertMelding != null) {
            var brevkode = journalpostInfo.getBrevkode();
            var tittel = journalpostInfo.getTittel();
            var payload = journalpostInfo.getStrukturertPayload();
            var mottatt = new JournalpostMottattEntitet(håndtertMelding.getJournalPostId(),
                håndtertMelding.getBehandlingTema(),
                journalpostInfo.getAktørId().orElse(null),
                brevkode,
                journalpostInfo.getForsendelseTidspunkt(),
                tittel, payload,
                JournalpostMottattEntitet.Status.UBEHANDLET);
            journalpostRepository.lagreMottatt(mottatt);
            log.info("Håndterer Journalpost[{}], Brevkode={} videre i: {}. Håndteres som [{}]",
                håndtertMelding.getJournalPostId().getVerdi(), journalpostInfo.getBrevkode(), håndtertMelding.getProsessTaskData().getTaskType(), kortBeskrivelseHåndtering);
        }

        return håndtertMelding;
    }

    public static Optional<Tema> utledTemaFraSafFeilmelding(TekniskException tekniskException) {
        Pattern pattern = Pattern.compile("tema_(\\w+)");
        Matcher matcher = pattern.matcher(tekniskException.getMessage());
        if (matcher.find()) {
            return Optional.of(Tema.valueOf(matcher.group(1).toUpperCase()));
        } else {
            return Optional.empty();
        }
    }
}
