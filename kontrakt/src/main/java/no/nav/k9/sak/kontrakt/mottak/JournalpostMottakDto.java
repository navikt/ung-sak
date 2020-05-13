package no.nav.k9.sak.kontrakt.mottak;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class JournalpostMottakDto {
    private static final int PAYLOAD_MAX_CHARS = 196000;

    @JsonProperty(value = "ytelseType", required = true)
    @Valid
    @NotNull
    private FagsakYtelseType ytelseType = FagsakYtelseType.OMSORGSPENGER;

    @JsonProperty(value = "forsendelseMottatt")
    private LocalDate forsendelseMottatt;

    @JsonProperty(value = "forsendelseMottattTidspunkt")
    private LocalDateTime forsendelseMottattTidspunkt;

    @JsonProperty(value = "journalpostId", required = true)
    @NotNull
    @Valid
    private JournalpostId journalpostId;

    @JsonProperty(value = "saksnummer", required = true)
    @NotNull
    @Valid
    private Saksnummer saksnummer;

    @JsonAlias("payloadXml")
    @JsonProperty("payload")
    @Pattern(regexp = "^[a-zA-Z0-9\\-_=]*$")
    @Size(max = PAYLOAD_MAX_CHARS * 2) // Gir plass til 50% flere byte enn characters, det bør holde
    protected String base64EncodedPayload;

    @JsonProperty(value = "type", required = true)
    @Valid
    @NotNull
    private Brevkode type = Brevkode.INNTEKTSMELDING; // FIXME K9: kan defaulte så lenge det er kun inntektsmeldinger som mottas i k9-sak

    public JournalpostMottakDto(Saksnummer saksnummer,
                                JournalpostId journalpostId,
                                FagsakYtelseType ytelseType,
                                Brevkode type,
                                LocalDateTime forsendelseMottattTidspunkt,
                                String payloadRawString) {
        this.saksnummer = saksnummer;
        this.journalpostId = journalpostId;
        this.type = type;
        this.forsendelseMottatt = forsendelseMottattTidspunkt.toLocalDate();
        this.forsendelseMottattTidspunkt = forsendelseMottattTidspunkt;
        this.ytelseType = ytelseType;
        String payload = null;
        if (payloadRawString != null && !(payload = payloadRawString.trim()).isEmpty()) {
            byte[] bytes = payload.getBytes(Charset.forName("UTF-8"));
            this.base64EncodedPayload = Base64.getUrlEncoder().encodeToString(bytes);
        }
    }

    protected JournalpostMottakDto() {
        // For Jackson
    }

    public Brevkode getType() {
        return type;
    }

    public Optional<LocalDate> getForsendelseMottatt() {
        return Optional.ofNullable(forsendelseMottatt);
    }

    public LocalDateTime getForsendelseMottattTidspunkt() {
        return forsendelseMottattTidspunkt;
    }

    @AbacAttributt("journalpostId")
    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    @AbacAttributt("saksnummer")
    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public FagsakYtelseType getYtelseType() {
        return ytelseType;
    }

}
