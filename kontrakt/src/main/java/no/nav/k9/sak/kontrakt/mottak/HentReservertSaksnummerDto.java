package no.nav.k9.sak.kontrakt.mottak;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class HentReservertSaksnummerDto {

    @JsonProperty(value = "saksnummer", required = true)
    @NotNull
    @Size(max = 19)
    @Pattern(regexp = "^[a-zA-Z0-9]*$")
    private final String saksnummer;

    @JsonProperty(value = "ytelseType", required = true)
    @Valid
    private FagsakYtelseType ytelseType;

    @JsonProperty(value = "brukerAktørId", required = true)
    @NotNull
    @Digits(integer = 19, fraction = 0)
    private String brukerAktørId;

    @JsonProperty(value = "pleietrengendeAktørId")
    @Digits(integer = 19, fraction = 0)
    private String pleietrengendeAktørId;

    @JsonProperty(value = "behandlingsår")
    @Digits(integer = 4, fraction = 0)
    private String behandlingsår;

    @JsonCreator
    public HentReservertSaksnummerDto(@JsonProperty(value = "saksnummer", required = true) @NotNull @Size(max = 19) @Pattern(regexp = "^[a-zA-Z0-9]*$") String saksnummer,
                                      @JsonProperty(value = "ytelseType", required = true) @NotNull FagsakYtelseType ytelseType,
                                      @JsonProperty(value = "brukerAktørId", required = true) @NotNull String brukerAktørId,
                                      @JsonProperty(value = "pleietrengendeAktørId") String pleietrengendeAktørId,
                                      @JsonProperty(value = "behandlingsår") String behandlingsår) {
        this.saksnummer = Objects.requireNonNull(saksnummer, "saksnummer");
        this.ytelseType = ytelseType;
        this.brukerAktørId = Objects.requireNonNull(brukerAktørId, "brukerAktørId");
        this.pleietrengendeAktørId = pleietrengendeAktørId;
        this.behandlingsår = behandlingsår;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public FagsakYtelseType getYtelseType() {
        return ytelseType;
    }

    public String getBrukerAktørId() {
        return brukerAktørId;
    }

    public String getPleietrengendeAktørId() {
        return pleietrengendeAktørId;
    }

    public String getBehandlingsår() {
        return behandlingsår;
    }
}
