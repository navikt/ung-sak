package no.nav.k9.sak.kontrakt.mottak;

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

import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class FinnEllerOpprettSakFnr {

    @JsonProperty(value = "ytelseType")
    @Size(max = 20)
    @Pattern(regexp = "^[\\p{Alnum}æøåÆØÅ_\\-\\.]*$")
    private String ytelseType;

    @JsonProperty(value = "søker", required = true)
    @NotNull
    @Digits(integer = 11, fraction = 0)
    private String søker;

    @JsonProperty(value = "pleietrengende")
    @Digits(integer = 11, fraction = 0)
    private String pleietrengende;

    @JsonProperty(value = "relatertPerson")
    @Digits(integer = 11, fraction = 0)
    private String relatertPerson;

    @JsonProperty(value = "periode", required = true)
    @NotNull
    @Valid
    private Periode periode;

    @JsonCreator
    public FinnEllerOpprettSakFnr(@JsonProperty(value = "ytelseType") @Size(max = 20) @Pattern(regexp = "^[\\p{Alnum}æøåÆØÅ_\\-\\.]*$") String ytelseType,
                               @JsonProperty(value = "søker", required = true) @NotNull @Digits(integer = 11, fraction = 0) String søker,
                               @JsonProperty(value = "pleietrengende") @Digits(integer = 11, fraction = 0) String pleietrengende,
                               @JsonProperty(value = "relatertPerson") @Digits(integer = 11, fraction = 0) String relatertPerson,
                               @JsonProperty(value = "periode", required = true) @NotNull @Valid Periode periode) {
        this.ytelseType = Objects.requireNonNull(ytelseType, "ytelseType");
        this.søker = søker;
        this.pleietrengende = pleietrengende;
        this.relatertPerson = relatertPerson;
        this.periode = periode;
    }

    public String getYtelseType() {
        return ytelseType;
    }

    public Periode getPeriode() {
        return periode;
    }

    @AbacAttributt(value = "søker", masker = true)
    public String getSøker() {
        return søker;
    }

    public String getPleietrengende() {
        return pleietrengende;
    }

    public String getRelatertPerson() {
        return relatertPerson;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
            + "<ytelseType=" + ytelseType
            + ", periode=" + getPeriode()
            + ">";
    }

}
