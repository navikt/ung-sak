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
import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ReserverSaksnummerDto {

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
    public ReserverSaksnummerDto(@JsonProperty(value = "ytelseType") FagsakYtelseType ytelseType,
                                 @JsonProperty(value = "brukerAktørId", required = true) @NotNull String brukerAktørId,
                                 @JsonProperty(value = "pleietrengendeAktørId") String pleietrengendeAktørId,
                                 @JsonProperty(value = "behandlingsår") String behandlingsår) {
        this.ytelseType = ytelseType != null ? ytelseType : FagsakYtelseType.UDEFINERT;
        this.brukerAktørId = Objects.requireNonNull(brukerAktørId, "aktørId");
        this.pleietrengendeAktørId = pleietrengendeAktørId;
        this.behandlingsår = behandlingsår;
    }

    public FagsakYtelseType getYtelseType() {
        return ytelseType;
    }

    @AbacAttributt(value = "aktorId", masker = true)
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
