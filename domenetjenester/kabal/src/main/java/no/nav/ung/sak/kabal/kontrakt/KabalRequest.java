package no.nav.ung.sak.kabal.kontrakt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class KabalRequest {

    @JsonIgnore
    private UUID behandlingUuid;

    private String avsenderEnhet;
    private String avsenderSaksbehandlerIdent;
    private String dvhReferanse;
    private String frist;
    private List<String> hjemler;
    private String innsendtTilNav;
    private String innsynUrl;
    private String kilde;
    private String kildeReferanse;
    private OversendtKlager klager;
    private String kommentar;
    private String mottattFoersteinstans;
    private String oversendtEnhet;
    private String sakReferanse;
    private String oversendtKaDato;
    private OversendtSakenGjelder sakenGjelder;
    private String tema;
    private List<String> tilknyttedeJournalposter;
    private String type;
    private String ytelse;
    private OversendtSak fagsak;

    public record OversendtKlager(
        OversendtPartId id,
        OversendtProsessfullmektig klagersProsessfullmektig
    ) {
    }

    public record OversendtPartId(
        String type,
        String verdi
    ) {
    }

    public record OversendtProsessfullmektig(
        OversendtPartId id,
        boolean skalKlagerMottaKopi
    ) {
    }

    public record OversendtSakenGjelder(
        OversendtPartId id,
        boolean skalMottaKopi
    ) { }

    public record OversendtSak(
        String fagsakId,
        String fagsystem
    ) { }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public void setBehandlingUuid(UUID behandlingUuid) {
        this.behandlingUuid = behandlingUuid;
    }

    public String getAvsenderEnhet() {
        return avsenderEnhet;
    }

    public void setAvsenderEnhet(String avsenderEnhet) {
        this.avsenderEnhet = avsenderEnhet;
    }

    public String getAvsenderSaksbehandlerIdent() {
        return avsenderSaksbehandlerIdent;
    }

    public void setAvsenderSaksbehandlerIdent(String avsenderSaksbehandlerIdent) {
        this.avsenderSaksbehandlerIdent = avsenderSaksbehandlerIdent;
    }

    public String getDvhReferanse() {
        return dvhReferanse;
    }

    public void setDvhReferanse(String dvhReferanse) {
        this.dvhReferanse = dvhReferanse;
    }

    public String getFrist() {
        return frist;
    }

    public void setFrist(String frist) {
        this.frist = frist;
    }

    public List<String> getHjemler() {
        return hjemler;
    }

    public void setHjemler(List<String> hjemler) {
        this.hjemler = hjemler;
    }

    public String getInnsendtTilNav() {
        return innsendtTilNav;
    }

    public void setInnsendtTilNav(String innsendtTilNav) {
        this.innsendtTilNav = innsendtTilNav;
    }

    public String getInnsynUrl() {
        return innsynUrl;
    }

    public void setInnsynUrl(String innsynUrl) {
        this.innsynUrl = innsynUrl;
    }

    public String getKilde() {
        return kilde;
    }

    public void setKilde(String kilde) {
        this.kilde = kilde;
    }

    public String getKildeReferanse() {
        return kildeReferanse;
    }

    public void setKildeReferanse(String kildeReferanse) {
        this.kildeReferanse = kildeReferanse;
    }

    public OversendtKlager getKlager() {
        return klager;
    }

    public void setKlager(OversendtKlager klager) {
        this.klager = klager;
    }

    public String getKommentar() {
        return kommentar;
    }

    public void setKommentar(String kommentar) {
        this.kommentar = kommentar;
    }

    public String getMottattFoersteinstans() {
        return mottattFoersteinstans;
    }

    public void setMottattFoersteinstans(String mottattFoersteinstans) {
        this.mottattFoersteinstans = mottattFoersteinstans;
    }

    public String getOversendtEnhet() {
        return oversendtEnhet;
    }

    public void setOversendtEnhet(String oversendtEnhet) {
        this.oversendtEnhet = oversendtEnhet;
    }

    public String getSakReferanse() {
        return sakReferanse;
    }

    public void setSakReferanse(String sakReferanse) {
        this.sakReferanse = sakReferanse;
    }

    public OversendtSakenGjelder getSakenGjelder() {
        return sakenGjelder;
    }

    public void setSakenGjelder(OversendtSakenGjelder sakenGjelder) {
        this.sakenGjelder = sakenGjelder;
    }

    public String getTema() {
        return tema;
    }

    public void setTema(String tema) {
        this.tema = tema;
    }

    public List<String> getTilknyttedeJournalposter() {
        return tilknyttedeJournalposter;
    }

    public void setTilknyttedeJournalposter(List<String> tilknyttedeJournalposter) {
        this.tilknyttedeJournalposter = tilknyttedeJournalposter;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getYtelse() {
        return ytelse;
    }

    public void setYtelse(String ytelse) {
        this.ytelse = ytelse;
    }

    public String getOversendtKaDato() {
        return oversendtKaDato;
    }

    public void setOversendtKaDato(String oversendtKaDato) {
        this.oversendtKaDato = oversendtKaDato;
    }

    public void setOversendtSak(OversendtSak oversendtSak) { this.fagsak = oversendtSak; }

    public OversendtSak getOversendtSak() { return fagsak; }

    @Override
    public String toString() {
        return "KabalRequest{" +
            "behandlingUuid=" + behandlingUuid +
            ", avsenderEnhet='" + avsenderEnhet + '\'' +
            ", avsenderSaksbehandlerIdent='" + avsenderSaksbehandlerIdent + '\'' +
            ", dvhReferanse='" + dvhReferanse + '\'' +
            ", frist='" + frist + '\'' +
            ", hjemler=" + hjemler +
            ", innsendtTilNav='" + innsendtTilNav + '\'' +
            ", innsynUrl='" + innsynUrl + '\'' +
            ", kilde='" + kilde + '\'' +
            ", kildeReferanse='" + kildeReferanse + '\'' +
            ", klager=" + klager +
            ", kommentar='" + kommentar + '\'' +
            ", mottattFoersteinstans='" + mottattFoersteinstans + '\'' +
            ", oversendtEnhet='" + oversendtEnhet + '\'' +
            ", sakReferanse='" + sakReferanse + '\'' +
            ", oversendtKaDato='" + oversendtKaDato + '\'' +
            ", sakenGjelder=" + sakenGjelder +
            ", tema='" + tema + '\'' +
            ", tilknyttedeJournalposter=" + tilknyttedeJournalposter +
            ", type='" + type + '\'' +
            ", ytelse='" + ytelse + '\'' +
            ", fagsak=" + fagsak +
            '}';
    }
}
