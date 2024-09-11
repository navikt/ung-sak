package no.nav.k9.sak.kontrakt.mottak;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @JsonProperty(value = "relatertPersonAktørId")
    @Digits(integer = 19, fraction = 0)
    private String relatertPersonAktørId;

    @JsonProperty(value = "behandlingsår")
    @Digits(integer = 4, fraction = 0)
    private String behandlingsår;

    //Barn eller fosterbarn for OMP_MA og OMP_UT
    @JsonProperty(value = "barnAktørIder")
    @Size(max = 100)
    @Valid
    private List<String> barnAktørIder;

    @JsonCreator
    public ReserverSaksnummerDto(@JsonProperty(value = "ytelseType") FagsakYtelseType ytelseType,
                                 @JsonProperty(value = "brukerAktørId", required = true) @NotNull String brukerAktørId,
                                 @JsonProperty(value = "pleietrengendeAktørId") String pleietrengendeAktørId,
                                 @JsonProperty(value = "relatertPersonAktørId") String relatertPersonAktørId,
                                 @JsonProperty(value = "behandlingsår") String behandlingsår,
                                 @JsonProperty(value = "barn") List<String> barnAktørIder) {
        this.ytelseType = ytelseType != null ? ytelseType : FagsakYtelseType.UDEFINERT;
        this.brukerAktørId = Objects.requireNonNull(brukerAktørId, "aktørId");
        this.pleietrengendeAktørId = pleietrengendeAktørId;
        this.relatertPersonAktørId = relatertPersonAktørId;
        this.behandlingsår = behandlingsår;
        this.barnAktørIder = barnAktørIder;
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

    public String getRelatertPersonAktørId() {
        return relatertPersonAktørId;
    }

    public String getBehandlingsår() {
        return behandlingsår;
    }

    public List<String> getBarnAktørIder() {
        return barnAktørIder;
    }
}
