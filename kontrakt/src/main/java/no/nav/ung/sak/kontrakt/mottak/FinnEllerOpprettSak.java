package no.nav.ung.sak.kontrakt.mottak;

import java.util.Objects;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.ung.abac.AbacAttributt;
import no.nav.ung.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class FinnEllerOpprettSak {

    @JsonProperty(value = "ytelseType")
    @Size(max = 20)
    @Pattern(regexp = "^[\\p{Alnum}æøåÆØÅ_\\-\\.]*$")
    private String ytelseType;

    @JsonProperty(value = "aktørId", required = true)
    @NotNull
    @Digits(integer = 19, fraction = 0)
    private String aktørId;

    @Deprecated(forRemoval = true)
    @JsonProperty(value = "pleietrengendeAktørId")
    @Digits(integer = 19, fraction = 0)
    private String pleietrengendeAktørId;

    @Deprecated(forRemoval = true)
    @JsonProperty(value = "relatertPersonAktørId")
    @Digits(integer = 19, fraction = 0)
    private String relatertPersonAktørId;

    @JsonProperty(value = "periode", required = true)
    @NotNull
    @Valid
    private Periode periode;

    @JsonProperty(value = "saksnummer")
    @Size(max = 19)
    @Pattern(regexp = "^[a-zA-Z0-9]*$")
    private String saksnummer;

    @JsonCreator
    public FinnEllerOpprettSak(@JsonProperty(value = "ytelseType") @Size(max = 20) @Pattern(regexp = "^[\\p{Alnum}æøåÆØÅ_\\-\\.]*$") String ytelseType,
                               @JsonProperty(value = "aktørId", required = true) @NotNull @Digits(integer = 19, fraction = 0) String aktørId,
                               @JsonProperty(value = "pleietrengendeAktørId") @Digits(integer = 19, fraction = 0) String pleietrengendeAktørId,
                               @JsonProperty(value = "relatertPersonAktørId") @Digits(integer = 19, fraction = 0) String relatertPersonAktørId,
                               @JsonProperty(value = "periode", required = true) @NotNull @Valid Periode periode,
                               @JsonProperty(value = "saksnummer") @Size(max = 19) @Pattern(regexp = "^[a-zA-Z0-9]*$") String saksnummer) {
        this.relatertPersonAktørId = relatertPersonAktørId;
        this.ytelseType = Objects.requireNonNull(ytelseType, "ytelseType");
        this.aktørId = aktørId;
        this.pleietrengendeAktørId = pleietrengendeAktørId;
        this.periode = periode;
        this.saksnummer = saksnummer;
    }

    public String getYtelseType() {
        return ytelseType;
    }

    public Periode getPeriode() {
        return periode;
    }

    @AbacAttributt(value = "aktorId", masker = true)
    public String getAktørId() {
        return aktørId;
    }

    public String getPleietrengendeAktørId() {
        return pleietrengendeAktørId;
    }

    public String getRelatertPersonAktørId() {
        return relatertPersonAktørId;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
            + "<ytelseType=" + ytelseType
            + ", periode=" + getPeriode()
            + ">";
    }

}
