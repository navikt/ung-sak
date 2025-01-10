package no.nav.ung.fordel.handler;

import java.sql.Clob;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.TaskType;
import no.nav.ung.domenetjenester.arkiv.JournalføringHendelsetype;
import no.nav.ung.fordel.kodeverdi.BehandlingType;
import no.nav.ung.fordel.kodeverdi.GosysKonstanter;
import no.nav.ung.fordel.kodeverdi.Tema;
import no.nav.ung.kodeverk.behandling.BehandlingTema;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.typer.JournalpostId;

public class MottattMelding {

    private static final Logger log = LoggerFactory.getLogger(MottattMelding.class);

    public static final String ARKIV_ID_KEY = "arkivId";
    public static final String AKTØR_ID_KEY = "aktoerId";
    public static final String SAKSNUMMER_KEY = "saksnummer";
    public static final String TEMA_KEY = "tema";
    public static final String RETRY_KEY = "retry";
    public static final String SØKNAD_ID_KEY = "soeknadId";
    public static final String JOURNALPOSTIDER_KEY = "journalpostIder";
    public static final String ORIGINAL_JOURNALPOSTID_KEY = "originalJournalpostId";
    public static final String ENDELIG_JOURNALFØRTE_JOURNALPOSTIDER_KEY = "endeligJournalfoerte.journalpostIder";
    public static final String BEHANDLINGSTEMA_KEY = "behandlingstema";
    public static final String BEHANDLINGSTYPE_KEY = "behandlingstype";
    public static final String STRUKTURERT_DOKUMENT = "strukturert.dokument";
    public static final String FORSENDELSE_MOTTATT_TIDSPUNKT_KEY = "forsendelse.mottatt.tidspunkt";
    public static final String JOURNAL_ENHET = "journalforende.enhet";
    public static final String FORSENDELSE_ID_KEY = "forsendelse.id";
    public static final String AVSENDER_ID_KEY = "avsender.id";
    public static final String FØRSTE_UTTAKSDAG_KEY = "forste.uttaksdag";
    public static final String SISTE_UTTAKSDAG_KEY = "siste.uttaksdag";
    public static final String YTELSE_TYPE = "ytelseType";

    // Mottatt dokument
    private static final String DOKUMENT_HENDELSE_TYPE = "dokumentHendelse.type";

    public static final String DOKUMENT_TITTEL_KEY = "dokument.tittel";
    public static final String BREVKODE_KEY = "dokument.brevkode";

    // Gosysoppgave
    public static final String OPPGAVE_IGNORER_SJEKK = "oppgave.ignorer.sjekk";
    private static final String OPPGAVE_BESKRIVELSE = "oppgave.beskrivelse";
    private static final String OPPGAVE_FAGSAKSYSTEM = "oppgave.fagsaksystem";
    private static final String OPPGAVE_TYPE = "oppgave.type";

    // Gosysoppgave fremfor innsending.
    private static final String JOURNALFORING_TILOPPGAVE = "journalforing.tilOppgave";
    private static final String JOURNALFORING_OPPDATERT_BRUKER = "journalforing.oppdatertBruker";


    // Punsj

    // Formidling
    private static final String DOKUMENT_MAL_TYPE = "formidling.dokumentmal.type";
    private static final String NESTE_TASK_ETTER_DOKUMENTBESTILLER = "formidling.task.etter.dokumentbestiller";

    private final ProsessTaskData prosessTaskData;

    public MottattMelding(ProsessTaskData eksisterendeData) {
        this.prosessTaskData = eksisterendeData;
    }

    private static FagsakYtelseType getFagsakYtelseFraBehandlingTema(BehandlingTema behandlingTema) {
        if (behandlingTema == null) {
            return null;
        } else {
            return behandlingTema.getFagsakYtelseType();
        }
    }

    public ProsessTaskData getProsessTaskData() {
        return prosessTaskData;
    }

    public MottattMelding nesteSteg(String stegnavn) {
        return nesteSteg(stegnavn, true, LocalDateTime.now());
    }

    public MottattMelding nesteSteg(String stegnavn, boolean økSekvens, LocalDateTime nesteKjøringEtter) {
        ProsessTaskData nesteStegProsessTaskData = ProsessTaskData.forTaskType(new TaskType(stegnavn));
        nesteStegProsessTaskData.setNesteKjøringEtter(nesteKjøringEtter);

        String sekvensnummer = getProsessTaskData().getSekvens();
        if (økSekvens) {
            long sekvens = Long.parseLong(sekvensnummer);
            sekvensnummer = Long.toString(sekvens + 1);
        }
        nesteStegProsessTaskData.setSekvens(sekvensnummer);

        MottattMelding neste = new MottattMelding(nesteStegProsessTaskData);
        neste.copyData(this);
        return neste;
    }

    public MottattMelding nesteSteg(String stegnavn, LocalDateTime nesteKjøring) {
        return nesteSteg(stegnavn, true, nesteKjøring);
    }

    private void copyData(MottattMelding fra) {
        this.addProperties(fra.getProsessTaskData().getProperties());
        this.setPayload(fra.getProsessTaskData().getPayload());
        this.getProsessTaskData().setGruppe(fra.getProsessTaskData().getGruppe());
    }

    private void addProperties(Properties newProps) {
        prosessTaskData.getProperties().putAll(newProps);
    }

    public Properties hentAlleProsessTaskVerdier() {
        return prosessTaskData.getProperties();
    }

    public Long getId() {
        return prosessTaskData.getId();
    }

    public String getArkivId() {
        return prosessTaskData.getPropertyValue(ARKIV_ID_KEY);
    }

    public void setArkivId(String arkivId) {
        prosessTaskData.setProperty(ARKIV_ID_KEY, arkivId);
    }

    public Optional<String> getSaksnummer() {
        return Optional.ofNullable(prosessTaskData.getPropertyValue(SAKSNUMMER_KEY));
    }

    public void setSaksnummer(String saksnummer) {
        prosessTaskData.setProperty(SAKSNUMMER_KEY, saksnummer);
    }

    public final LocalDate getForsendelseMottatt() {
        Optional<LocalDateTime> localDateTime = getForsendelseMottattTidspunkt();
        return localDateTime.map(LocalDateTime::toLocalDate).orElse(null);
    }

    public Optional<LocalDateTime> getForsendelseMottattTidspunkt() {
        final String property = prosessTaskData.getProperties().getProperty(FORSENDELSE_MOTTATT_TIDSPUNKT_KEY);
        final LocalDateTime localDateTime = property != null ? LocalDateTime.parse(property, DateTimeFormatter.ISO_DATE_TIME) : null;
        return Optional.ofNullable(localDateTime);
    }

    public void setForsendelseMottattTidspunkt(LocalDateTime forsendelseMottattTidspunkt) {
        prosessTaskData.setProperty(FORSENDELSE_MOTTATT_TIDSPUNKT_KEY, forsendelseMottattTidspunkt.format(DateTimeFormatter.ISO_DATE_TIME));
    }

    public int getRetryingTask() {
        return Integer.parseInt(Optional.ofNullable(prosessTaskData.getProperties().getProperty(RETRY_KEY)).orElse("0"));
    }

    public void setRetryingTask(int retries) {
        prosessTaskData.setProperty(RETRY_KEY, Integer.toString(retries));
    }

    public Optional<String> getJournalførendeEnhet() {
        return Optional.ofNullable(prosessTaskData.getPropertyValue(JOURNAL_ENHET));
    }

    public void setJournalførendeEnhet(String enhet) {
        prosessTaskData.setProperty(JOURNAL_ENHET, enhet);
    }

    public void setPayload(String payload) {
        prosessTaskData.setPayload(payload);
    }

    public void setPayload(Clob payload) {
        prosessTaskData.setPayload(payload);
    }

    public Optional<String> getPayloadAsString() {
        return Optional.ofNullable(prosessTaskData.getPayloadAsString());
    }

    public Optional<String> getAktørId() {
        return Optional.ofNullable(prosessTaskData.getAktørId());
    }

    public void setAktørId(String aktørId) {
        prosessTaskData.setAktørId(aktørId);
    }

    public String getSøknadId() {
        return prosessTaskData.getPropertyValue(SØKNAD_ID_KEY);
    }

    public void setSøknadId(String søknadId) {
        prosessTaskData.setProperty(SØKNAD_ID_KEY, søknadId);
    }

    public Optional<Boolean> erStrukturertDokument() {
        final String property = prosessTaskData.getPropertyValue(STRUKTURERT_DOKUMENT);
        Boolean bool = property != null ? Boolean.parseBoolean(property) : null;
        return Optional.ofNullable(bool);
    }

    public void setStrukturertDokument(Boolean erStrukturertDokument) {
        prosessTaskData.setProperty(STRUKTURERT_DOKUMENT, String.valueOf(erStrukturertDokument));
    }

    public Optional<UUID> getForsendelseId() {
        String forsendelseId = prosessTaskData.getPropertyValue(FORSENDELSE_ID_KEY);
        return forsendelseId == null ? Optional.empty() : Optional.of(UUID.fromString(forsendelseId));
    }

    public void setForsendelseId(UUID forsendelseId) {
        prosessTaskData.setProperty(FORSENDELSE_ID_KEY, forsendelseId.toString());
    }

    public Optional<String> getAvsenderId() {
        return Optional.ofNullable(prosessTaskData.getProperties().getProperty(AVSENDER_ID_KEY));
    }

    public void setAvsenderId(String avsenderId) {
        prosessTaskData.setProperty(AVSENDER_ID_KEY, avsenderId);
    }

    public Optional<LocalDate> getFørsteUttaksdag() {
        final String property = prosessTaskData.getProperties().getProperty(FØRSTE_UTTAKSDAG_KEY);
        final LocalDate localDate = property != null ? LocalDate.parse(property) : null;
        return Optional.ofNullable(localDate);
    }

    public Optional<LocalDate> getSisteUttaksdag() {
        final String property = prosessTaskData.getProperties().getProperty(SISTE_UTTAKSDAG_KEY);
        final LocalDate localDate = property != null ? LocalDate.parse(property) : null;
        return Optional.ofNullable(localDate);
    }

    public void setFørsteUttaksdag(LocalDate dato) {
        prosessTaskData.setProperty(FØRSTE_UTTAKSDAG_KEY, dato != null ? dato.toString() : null);
    }

    public void setSisteUttaksdag(LocalDate dato) {
        prosessTaskData.setProperty(SISTE_UTTAKSDAG_KEY, dato != null ? dato.toString() : null);
    }

    public Set<JournalpostId> getJournalPostIder() {
        final var propertyValue = prosessTaskData.getPropertyValue(JOURNALPOSTIDER_KEY);
        if (propertyValue == null) {
            return Set.of();
        } else if (!propertyValue.contains(",")) {
            return Set.of(new JournalpostId(propertyValue));
        }
        return new LinkedHashSet<>(Arrays.stream(propertyValue.split(","))
            .map(JournalpostId::new)
            .collect(Collectors.toList()));
    }

    public void setJournalPostIder(Set<JournalpostId> journalPostIder) {
        if (journalPostIder != null) {
            prosessTaskData.setProperty(JOURNALPOSTIDER_KEY, journalPostIder.stream().map(it -> it.getVerdi()).collect(Collectors.joining(",")));
        }
    }

    public Set<JournalpostId> getEndeligJournalførteJournalPostIder() {
        final var propertyValue = prosessTaskData.getPropertyValue(ENDELIG_JOURNALFØRTE_JOURNALPOSTIDER_KEY);
        if (propertyValue == null) {
            return Set.of();
        }
        return new LinkedHashSet<>(Arrays.stream(propertyValue.split(","))
            .map(JournalpostId::new)
            .collect(Collectors.toList()));
    }

    public void setEndeligJournalførteJournalPostIder(Set<JournalpostId> journalPostIder) {
        if (journalPostIder != null) {
            prosessTaskData.setProperty(ENDELIG_JOURNALFØRTE_JOURNALPOSTIDER_KEY,
                journalPostIder.stream().map(JournalpostId::getVerdi).collect(Collectors.joining(",")));
        }
    }

    /**
     * Returnerer første journalpostid hvis finnes.
     */
    public JournalpostId getJournalPostId() {
        var ider = getJournalPostIder();
        if (ider == null || ider.isEmpty()) {
            return null;
        } else {
            return ider.iterator().next(); // tar første
        }
    }

    public void setJournalPostId(JournalpostId journalPostId) {
        if (journalPostId != null) {
            prosessTaskData.setProperty(JOURNALPOSTIDER_KEY, journalPostId.getVerdi());
        }
    }

    public void setOriginalJournalPostId(JournalpostId originalJournalPostId) {
        if (originalJournalPostId != null) {
            prosessTaskData.setProperty(ORIGINAL_JOURNALPOSTID_KEY, originalJournalPostId.getVerdi());
        }
    }

    public Optional<JournalpostId> getOriginalJournalPostId() {
        return Optional.ofNullable(prosessTaskData.getPropertyValue(ORIGINAL_JOURNALPOSTID_KEY)).map(JournalpostId::new);
    }

    public Tema getTema() {
        var propertyValue = prosessTaskData.getPropertyValue(TEMA_KEY);
        if (propertyValue != null) {
            return Tema.fraKode(propertyValue);
        }
        return null;
    }

    public void setTema(Tema tema) {
        Objects.requireNonNull(tema);
        prosessTaskData.setProperty(TEMA_KEY, tema.getKode());
    }

    public BehandlingTema getBehandlingTema() {
        var propertyValue = prosessTaskData.getPropertyValue(BEHANDLINGSTEMA_KEY);
        if (propertyValue != null) {
            return BehandlingTema.fraKode(propertyValue);
        }
        return null;
    }

    public void setOppgaveIgnorerSjekk(boolean ignorer) {
        prosessTaskData.setProperty(OPPGAVE_IGNORER_SJEKK, String.valueOf(ignorer));
    }

    public boolean isOppgaveIgnorerSjekk() {
        var ignorer = prosessTaskData.getPropertyValue(OPPGAVE_IGNORER_SJEKK);
        return ignorer != null && Boolean.parseBoolean(ignorer);
    }

    public void setBehandlingTema(BehandlingTema behandlingTema) {
        Objects.requireNonNull(behandlingTema);
        prosessTaskData.setProperty(BEHANDLINGSTEMA_KEY, behandlingTema.getKode());

        if (prosessTaskData.getPropertyValue(YTELSE_TYPE) == null && !BehandlingTema.UDEFINERT.equals(behandlingTema)) {
            // avled
            FagsakYtelseType ytelseType = getFagsakYtelseFraBehandlingTema(behandlingTema);
            if (ytelseType != null) {
                setYtelseType(ytelseType);
            } else {
                log.info("Har ikke entydig avledning av ytelseType for BehandlingTema: {}. Får ikke angitt ytelseType for melding {}.",
                    behandlingTema, getJournalPostId());
            }
        }
    }

    public Optional<BehandlingType> getBehandlingType() {
        var propertyValue = prosessTaskData.getPropertyValue(BEHANDLINGSTYPE_KEY);
        if (propertyValue != null) {
            return Optional.ofNullable(BehandlingType.fraKode(propertyValue));
        }
        return Optional.empty();
    }

    public void setBehandlingType(BehandlingType behandlingType) {
        Objects.requireNonNull(behandlingType);
        prosessTaskData.setProperty(BEHANDLINGSTYPE_KEY, behandlingType.getKode());
    }

    public boolean erEttersendelse() {
        Optional<BehandlingType> behandlingType = getBehandlingType();
        return behandlingType.isPresent() && behandlingType.get() == BehandlingType.DIGITAL_ETTERSENDELSE;
    }

    public Optional<String> getDokumentTittel() {
        return Optional.ofNullable(prosessTaskData.getPropertyValue(DOKUMENT_TITTEL_KEY));
    }

    public void setDokumentTittel(String tittel) {
        prosessTaskData.setProperty(DOKUMENT_TITTEL_KEY, tittel);
    }

    public Optional<FagsakYtelseType> getYtelseType() {
        var propertyValue = prosessTaskData.getPropertyValue(YTELSE_TYPE);
        if (propertyValue != null && !FagsakYtelseType.UDEFINERT.getKode().equals(propertyValue)) {
            return Optional.ofNullable(FagsakYtelseType.fraKode(propertyValue));
        } else {
            // fallback
            FagsakYtelseType ytelseType = getFagsakYtelseFraBehandlingTema(getBehandlingTema());
            if (FagsakYtelseType.UDEFINERT.equals(ytelseType)) {
                return Optional.empty();
            } else {
                return Optional.ofNullable(ytelseType);
            }
        }
    }

    public void setYtelseType(FagsakYtelseType ytelseType) {
        prosessTaskData.setProperty(YTELSE_TYPE, ytelseType.getKode());
    }

    public Optional<String> getBeskrivelse() {
        return Optional.ofNullable(prosessTaskData.getPropertyValue(OPPGAVE_BESKRIVELSE));
    }

    public void setBeskrivelse(String beskrivelse) {
        prosessTaskData.setProperty(OPPGAVE_BESKRIVELSE, beskrivelse);
    }

    public void setBrevkode(String brevkode) {
        prosessTaskData.setProperty(BREVKODE_KEY, brevkode);
    }

    public String getBrevkode() {
        return prosessTaskData.getPropertyValue(BREVKODE_KEY);
    }


    public void setOppgaveFagsaksystem(GosysKonstanter.Fagsaksystem fagsystem) {
        prosessTaskData.setProperty(OPPGAVE_FAGSAKSYSTEM, fagsystem.getKode());
    }

    public Optional<GosysKonstanter.Fagsaksystem> getOppgaveFagsaksystem() {
        var propertyValue = prosessTaskData.getPropertyValue(OPPGAVE_FAGSAKSYSTEM);
        return Optional.ofNullable(GosysKonstanter.Fagsaksystem.from(propertyValue));
    }

    public void setOppgaveType(GosysKonstanter.OppgaveType oppgaveType) {
        prosessTaskData.setProperty(OPPGAVE_TYPE, oppgaveType.getKode());
    }

    public Optional<GosysKonstanter.OppgaveType> getOppgaveType() {
        var propertyValue = prosessTaskData.getPropertyValue(OPPGAVE_TYPE);
        return Optional.ofNullable(GosysKonstanter.OppgaveType.from(propertyValue));
    }

    public void setJournalforingTilOppgave(boolean journalforingTilOppgave) {
        prosessTaskData.setProperty(JOURNALFORING_TILOPPGAVE, Boolean.toString(journalforingTilOppgave));
    }

    public boolean isJournalforingTilOppgave() {
        var propertyValue = prosessTaskData.getPropertyValue(JOURNALFORING_TILOPPGAVE);
        return Boolean.parseBoolean(propertyValue);
    }

    public void setJournalforingOppdatertBruker(boolean journalforingTilOppgave) {
        prosessTaskData.setProperty(JOURNALFORING_OPPDATERT_BRUKER, Boolean.toString(journalforingTilOppgave));
    }

    public boolean isJournalforingOppdatertBruker() {
        var propertyValue = prosessTaskData.getPropertyValue(JOURNALFORING_OPPDATERT_BRUKER);
        if (propertyValue == null) {
            return false;
        }
        return Boolean.parseBoolean(propertyValue);
    }

    public void setJournalføringHendelsetype(JournalføringHendelsetype journalføringHendelsetype) {
        if (journalføringHendelsetype != null) {
            prosessTaskData.setProperty(DOKUMENT_HENDELSE_TYPE, journalføringHendelsetype.kode);
        }
    }

    public Optional<JournalføringHendelsetype> getJournalføringHendelsetype() {
        return JournalføringHendelsetype.fraKode(prosessTaskData.getPropertyValue(DOKUMENT_HENDELSE_TYPE));
    }

    public String getDokumentMalType() {
        return prosessTaskData.getPropertyValue(DOKUMENT_MAL_TYPE);
    }

    public void setDokumentMalType(String dokumentMalType) {
        prosessTaskData.setProperty(DOKUMENT_MAL_TYPE, dokumentMalType);
    }

    public Optional<String> getNesteTaskEtterDokumentbestiller() {
        final String task = prosessTaskData.getProperties().getProperty(NESTE_TASK_ETTER_DOKUMENTBESTILLER);
        return Optional.ofNullable(task);
    }

    public void setNesteTaskEtterDokumentbestiller(String task) {
        prosessTaskData.setProperty(NESTE_TASK_ETTER_DOKUMENTBESTILLER, task);
    }

}
