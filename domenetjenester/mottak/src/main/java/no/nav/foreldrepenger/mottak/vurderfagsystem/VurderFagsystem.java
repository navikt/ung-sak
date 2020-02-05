package no.nav.foreldrepenger.mottak.vurderfagsystem;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import no.nav.k9.kodeverk.behandling.BehandlingTema;
import no.nav.k9.kodeverk.dokument.DokumentKategori;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;

public class VurderFagsystem {
    // FIXME(Humle): dette skal inn i kodeverk.  Ikke bruk title-case i koder vær så snill.
    public static final String ÅRSAK_ENDRING = "Endring";
    public static final String ÅRSAK_NY = "Ny";

    private JournalpostId journalpostId;
    private boolean strukturertSøknad;
    private AktørId aktørId;
    private BehandlingTema behandlingTema;

    private String årsakInnsendingInntektsmelding;

    private LocalDateTime forsendelseMottattTidspunkt;
    private LocalDate startdatoInntektsmelding;

    private Saksnummer saksnummer;

    private DokumentTypeId dokumentTypeId;
    private DokumentKategori dokumentKategori;

    private String virksomhetsnummer;
    private AktørId arbeidsgiverAktørId;
    private String arbeidsforholdsid;

    public Optional<JournalpostId> getJournalpostId() {
        return Optional.ofNullable(journalpostId);
    }

    public void setJournalpostId(JournalpostId journalpostId) {
        this.journalpostId = journalpostId;
    }

    public boolean erStrukturertSøknad() {
        return strukturertSøknad;
    }

    public void setStrukturertSøknad(boolean strukturertSøknad) {
        this.strukturertSøknad = strukturertSøknad;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public void setAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
    }

    public BehandlingTema getBehandlingTema() {
        return behandlingTema;
    }

    public void setBehandlingTema(BehandlingTema behandlingTema) {
        this.behandlingTema = behandlingTema;
    }

    public Optional<String> getÅrsakInnsendingInntektsmelding() {
        return Optional.ofNullable(årsakInnsendingInntektsmelding);
    }

    public void setÅrsakInnsendingInntektsmelding(String årsakInnsendingInntektsmelding) {
        this.årsakInnsendingInntektsmelding = årsakInnsendingInntektsmelding;
    }

    public Optional<Saksnummer> getSaksnummer() {
        return Optional.ofNullable(saksnummer);
    }

    public void setSaksnummer(Saksnummer saksnummer) {
        this.saksnummer = saksnummer;
    }

    public boolean erInntektsmelding() {
        return årsakInnsendingInntektsmelding != null;
    }

    public DokumentTypeId getDokumentTypeId() {
        return dokumentTypeId;
    }

    public void setDokumentTypeId(DokumentTypeId dokumentTypeId) {
        this.dokumentTypeId = dokumentTypeId;
    }

    public DokumentKategori getDokumentKategori() {
        return dokumentKategori;
    }

    public void setDokumentKategori(DokumentKategori dokumentKategori) {
        this.dokumentKategori = dokumentKategori;
    }

    public Optional<String> getVirksomhetsnummer() {
        return Optional.ofNullable(virksomhetsnummer);
    }

    public void setVirksomhetsnummer(String virksomhetsnummer) {
        this.virksomhetsnummer = virksomhetsnummer;
    }

    public Optional<AktørId> getArbeidsgiverAktørId() {
        return Optional.ofNullable(arbeidsgiverAktørId);
    }

    public void setArbeidsgiverAktørId(AktørId arbeidsgiverAktørId) {
        this.arbeidsgiverAktørId = arbeidsgiverAktørId;
    }

    public Optional<String> getArbeidsforholdsid() {
        return Optional.ofNullable(arbeidsforholdsid);
    }

    public void setArbeidsforholdsid(String arbeidsforholdsid) {
        this.arbeidsforholdsid = arbeidsforholdsid;
    }

    public Optional<LocalDateTime> getForsendelseMottattTidspunkt() {
        return Optional.ofNullable(forsendelseMottattTidspunkt);
    }

    public void setForsendelseMottattTidspunkt(LocalDateTime forsendelseMottattTidspunkt) {
        this.forsendelseMottattTidspunkt = forsendelseMottattTidspunkt;
    }

    public Optional<LocalDate> getStartDatoInntektsmelding() {
        return Optional.ofNullable(startdatoInntektsmelding);
    }

    public void setStartDatoInntektsmelding(LocalDate startdatoInntektsmelding) {
        this.startdatoInntektsmelding = startdatoInntektsmelding;
    }
}

