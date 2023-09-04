package no.nav.k9.sak.kontrakt.notat;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.kodeverk.notat.NotatGjelderType;
import no.nav.k9.sak.kontrakt.dokument.TekstValideringRegex;

/**
 * Dto for notat entiteter. Brukes bare til serialisering.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "gjelderType")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class NotatDto { //TODO gjør om til record

    //TODO fjern
    @JsonProperty(value = "id")
    private Long id;

    @JsonProperty(value = "uuid")
    private UUID uuid;

    @JsonProperty(value = "notatTekst")
    @Size(max = 4000)
    @Pattern(regexp = TekstValideringRegex.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @NotNull
    private String notatTekst;

    @JsonProperty(value = "skjult")
    @NotNull
    private boolean skjult;

    @JsonProperty("gjelderType")
    private NotatGjelderType gjelderType;

    @JsonProperty(value = "versjon")
    @NotNull
    long versjon;

    @JsonProperty(value = "opprettetAv")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Alnum}ÆØÅæøå\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String opprettetAv;

    @JsonProperty(value = "opprettetTidspunkt")
    private LocalDateTime opprettetTidspunkt;

    @JsonProperty(value = "endretAv")
    @Size(max = 20)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String endretAv;

    @JsonProperty(value = "endretTidspunkt")
    @Valid
    private LocalDateTime endretTidspunkt;

    public NotatDto(Long id, UUID uuid, String notatTekst, boolean skjult, NotatGjelderType gjelderType, long versjon, String opprettetAv, LocalDateTime opprettetTidspunkt, String endretAv, LocalDateTime endretTidspunkt) {
        this.id = id;
        this.uuid = uuid;
        this.notatTekst = notatTekst;
        this.skjult = skjult;
        this.gjelderType = gjelderType;
        this.versjon = versjon;
        this.opprettetAv = opprettetAv;
        this.opprettetTidspunkt = opprettetTidspunkt;
        this.endretAv = endretAv;
        this.endretTidspunkt = endretTidspunkt;
    }

    public Long getId() {
        return id;
    }

    public String getNotatTekst() {
        return notatTekst;
    }

    public boolean isSkjult() {
        return skjult;
    }

    public NotatGjelderType getGjelderType() {
        return gjelderType;
    }

    public long getVersjon() {
        return versjon;
    }

    public String getOpprettetAv() {
        return opprettetAv;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    public String getEndretAv() {
        return endretAv;
    }

    public LocalDateTime getEndretTidspunkt() {
        return endretTidspunkt;
    }

    public UUID getUuid() {
        return uuid;
    }
}
