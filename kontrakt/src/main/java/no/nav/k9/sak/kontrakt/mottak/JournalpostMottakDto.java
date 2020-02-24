package no.nav.k9.sak.kontrakt.mottak;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class JournalpostMottakDto {

    private static final int PAYLOAD_MAX_CHARS = 196000;

    @JsonProperty(value = "behandlingstemaOffisiellKode", required = true)
    @Valid
    @NotNull
    @Size(max = 8)
    @Pattern(regexp = "^[a-zA-ZæøåÆØÅ_\\-0-9]*")
    private String behandlingstemaOffisiellKode;

    @JsonProperty(value = "dokumentKategoriOffisiellKode")
    @Size(max = 25)
    @Pattern(regexp = "^[a-zA-ZæøåÆØÅ_\\-0-9]*")
    private String dokumentKategoriOffisiellKode;

    @JsonProperty(value = "dokumentTypeIdOffisiellKode")
    @Valid
    @Size(max = 8)
    @Pattern(regexp = "^[a-zA-ZæøåÆØÅ_\\-0-9]*")
    private String dokumentTypeIdOffisiellKode;

    @JsonProperty(value = "forsendelseId")
    @Valid
    private UUID forsendelseId;

    @JsonProperty(value = "forsendelseMottatt")
    private LocalDate forsendelseMottatt;

    @JsonProperty(value = "forsendelseMottattTidspunkt")
    private LocalDateTime forsendelseMottattTidspunkt;

    @JsonProperty(value = "journalForendeEnhet")
    @Size(max = 5)
    @Pattern(regexp = "^[a-zA-ZæøåÆØÅ_\\-0-9]*")
    private String journalForendeEnhet;

    @JsonProperty(value = "journalpostId", required = true)
    @NotNull
    @Valid
    private JournalpostId journalpostId;

    @JsonProperty(value = "saksnummer", required = true)
    @NotNull
    @Valid
    private Saksnummer saksnummer;

    @JsonProperty("payloadXml")
    @Pattern(regexp = "^[a-zA-Z0-9\\-_=]*$")
    @Size(max = PAYLOAD_MAX_CHARS * 2) // Gir plass til 50% flere byte enn characters, det bør holde
    protected String base64EncodedPayloadXml;

    /**
     * Siden XML'en encodes før overføring må lengden på XML'en lagres som en separat property for å kunne valideres.
     * Lengden er basert på at MOTTAT_DOKUMENT.XML_PAYLOAD ern en VARCHAR2(4000)
     */
    @JsonProperty("payloadLength")
    @Max(PAYLOAD_MAX_CHARS)
    @Min(1)
    protected Integer payloadLength;

    public JournalpostMottakDto(Saksnummer saksnummer, JournalpostId journalpostId, String behandlingstemaOffisiellKode, String dokumentTypeIdOffisiellKode,
                                LocalDateTime forsendelseMottattTidspunkt, String payloadXml) {
        this.saksnummer = saksnummer;
        this.journalpostId = journalpostId;
        this.behandlingstemaOffisiellKode = behandlingstemaOffisiellKode;
        this.dokumentTypeIdOffisiellKode = dokumentTypeIdOffisiellKode;
        this.forsendelseMottatt = forsendelseMottattTidspunkt.toLocalDate();
        this.forsendelseMottattTidspunkt = forsendelseMottattTidspunkt;
        String payload = null;
        if (payloadXml != null && !(payload = payloadXml.trim()).isEmpty()) {
            byte[] bytes = payload.getBytes(Charset.forName("UTF-8"));
            this.payloadLength = payload.length();
            this.base64EncodedPayloadXml = Base64.getUrlEncoder().encodeToString(bytes);
        }
    }

    protected JournalpostMottakDto() {
        // For Jackson
    }

    public String getBehandlingstemaOffisiellKode() {
        return behandlingstemaOffisiellKode;
    }

    public String getDokumentKategoriOffisiellKode() {
        return dokumentKategoriOffisiellKode;
    }

    public Optional<String> getDokumentTypeIdOffisiellKode() {
        return Optional.ofNullable(dokumentTypeIdOffisiellKode);
    }

    public Optional<UUID> getForsendelseId() {
        return Optional.ofNullable(this.forsendelseId);
    }

    public Optional<LocalDate> getForsendelseMottatt() {
        return Optional.ofNullable(forsendelseMottatt);
    }

    public LocalDateTime getForsendelseMottattTidspunkt() {
        return forsendelseMottattTidspunkt;
    }

    public String getJournalForendeEnhet() {
        return journalForendeEnhet;
    }

    @AbacAttributt("journalpostId")
    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    @AbacAttributt("saksnummer")
    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public void setDokumentKategoriOffisiellKode(String dokumentKategoriOffisiellKode) {
        this.dokumentKategoriOffisiellKode = dokumentKategoriOffisiellKode;
    }

    public void setForsendelseId(UUID forsendelseId) {
        this.forsendelseId = forsendelseId;
    }

    public void setJournalForendeEnhet(String journalForendeEnhet) {
        this.journalForendeEnhet = journalForendeEnhet;
    }

}
