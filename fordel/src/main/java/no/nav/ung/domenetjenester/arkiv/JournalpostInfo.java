package no.nav.ung.domenetjenester.arkiv;

import java.time.LocalDateTime;
import java.util.Optional;

import no.nav.k9.felles.integrasjon.saf.Journalstatus;
import no.nav.k9.felles.integrasjon.saf.Tema;
import no.nav.ung.sak.typer.AktørId;

public class JournalpostInfo {

    private String strukturertPayload;
    private String dokumentInfoId;
    private String brevkode;
    private String tittel;
    private String fagsakSystem;
    private String fagsakId;
    private String type;
    private AktørId aktørId;
    private LocalDateTime forsendelseTidspunkt;
    private Journalstatus journalstatus;
    private String journalpostId;
    private Tema tema;

    public JournalpostInfo() {
    }

    public String getStrukturertPayload() {
        return strukturertPayload;
    }

    public void setStrukturertPayload(String strukturertPayload) {
        this.strukturertPayload = strukturertPayload;
    }

    public boolean getInnholderStrukturertInformasjon() {
        return strukturertPayload != null && !strukturertPayload.isEmpty();
    }

    public boolean harBrevkode() {
        return brevkode != null && !brevkode.isBlank();
    }

    public String getDokumentInfoId() {
        return dokumentInfoId;
    }

    public void setDokumentInfoId(String dokumentInfoId) {
        this.dokumentInfoId = dokumentInfoId;
    }

    public String getBrevkode() {
        return brevkode;
    }

    public void setBrevkode(String brevkode) {
        this.brevkode = brevkode;
    }

    public String getTittel() {
        return tittel;
    }

    public void setTittel(String tittel) {
        this.tittel = tittel;
    }

    public void setIdent(AktørId aktørId) {
        this.aktørId = aktørId;
    }

    public Optional<AktørId> getAktørId() {
        return Optional.ofNullable(aktørId);
    }

    public LocalDateTime getForsendelseTidspunkt() {
        return forsendelseTidspunkt;
    }

    public void setForsendelseTidspunkt(LocalDateTime datoOpprettet) {
        this.forsendelseTidspunkt = datoOpprettet;
    }

    public void setFagsakSystem(String fagsakSystem) {
        this.fagsakSystem = fagsakSystem;
    }

    public Optional<String> getFagsakSystem() {
        return Optional.ofNullable(fagsakSystem);
    }

    public void setFagsakId(String fagsakId) {
        this.fagsakId = fagsakId;
    }

    public Optional<String> getFagsakId() {
        return Optional.ofNullable(fagsakId);
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public Journalstatus getJournalstatus() {
        return journalstatus;
    }

    public void setJournalstatus(Journalstatus journalstatus) {
        this.journalstatus = journalstatus;
    }

    @Override
    public String toString() {
        return "JournalpostInfo{" +
                "dokumentInfoId='" + dokumentInfoId + '\'' +
                ", brevkode='" + brevkode + '\'' +
                ", tittel='" + tittel + '\'' +
                '}';
    }

    public void setJournalpostId(String journalPostId) {
        this.journalpostId = journalPostId;
    }

    public String getJournalpostId() {
        return journalpostId;
    }

    public void setTema(Tema tema) {
        this.tema = tema;
    }

    public Tema getTema() {
        return tema;
    }
}
