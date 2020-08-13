package no.nav.k9.sak.kontrakt.søknad.innsending;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Innsending {

    @JsonProperty(value = "saksnummer", required = true)
    @NotNull
    @Valid
    private Saksnummer saksnummer;

    @JsonProperty(value = "journalpostId")
    @Valid
    private JournalpostId journalpostId;

    @JsonProperty(value = "innhold", required = true)
    @NotNull
    @Valid
    private InnsendingInnhold innhold;

    @JsonProperty(value = "type", required = true)
    @Valid
    @NotNull
    private Brevkode type = Brevkode.INNTEKTKOMP_FRILANS; // FIXME K9: kan defaulte så lenge det er kun FRISINN som mottas i k9-sak

    /** @deprecated bruker {@link #forsendelseMottattTidspunkt}*/
    @Deprecated
    @JsonProperty(value = "mottattDato")
    @Valid
    private LocalDate forsendelseMottattDato;

    @JsonProperty(value = "mottattTidspunkt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    @Valid
    @NotNull
    private ZonedDateTime forsendelseMottattTidspunkt;

    @JsonProperty(value = "kanalReferanse")
    @Pattern(regexp = "^[a-zA-Z0-9\\\\/\\.\\:\\-_=]*$")
    @Size(max = 100)
    private String kanalReferanse;

    @JsonCreator
    public Innsending(@JsonProperty(value = "saksnummer", required = true) @NotNull @Valid Saksnummer saksnummer,
                      @JsonProperty(value = "innhold", required = true) @NotNull @Valid InnsendingInnhold innhold,
                      @JsonProperty(value = "journalpostId") @Valid JournalpostId journalpostId,
                      @JsonProperty(value = "type") @Valid Brevkode type,
                      @JsonProperty(value = "mottattDato") LocalDate mottattDato,
                      @JsonProperty(value = "mottattTidspunkt") ZonedDateTime mottattTidspunkt,
                      @JsonProperty(value = "kanalReferanse") String kanalReferanse) {
        this.journalpostId = journalpostId;
        this.forsendelseMottattDato = mottattDato;
        this.forsendelseMottattTidspunkt = mottattTidspunkt;
        this.kanalReferanse = kanalReferanse;
        this.type = type == null ? Brevkode.INNTEKTKOMP_FRILANS : type; // FIXME K9: Fjern default
        this.saksnummer = Objects.requireNonNull(saksnummer, "saksnummer");
        this.innhold = Objects.requireNonNull(innhold, "innhold");
    }

    @AbacAttributt("saksnummer")
    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    @AbacAttributt("journalpostId")
    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public InnsendingInnhold getInnhold() {
        return innhold;
    }

    public FagsakYtelseType getYtelseType() {
        return innhold.getYtelseType();
    }

    public Brevkode getType() {
        return type;
    }

    public LocalDate getForsendelseMottattDato() {
        return forsendelseMottattDato;
    }

    public ZonedDateTime getForsendelseMottattTidspunkt() {
        return forsendelseMottattTidspunkt;
    }

    public String getKanalReferanse() {
        return kanalReferanse;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<"
            + "saksnummer=" + saksnummer
            + ", journalpostId=" + journalpostId
            + ", type=" + type
            + ", forsendelseMottattTidspunkt=" + forsendelseMottattTidspunkt
            + ", kanalReferanse=" + kanalReferanse
            + ">";
    }
}
