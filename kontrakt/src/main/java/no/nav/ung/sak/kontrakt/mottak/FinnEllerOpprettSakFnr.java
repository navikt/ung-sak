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

import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.ung.abac.StandardAbacAttributt;
import no.nav.ung.sak.typer.Periode;

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

    @JsonProperty(value = "periode", required = true)
    @NotNull
    @Valid
    private Periode periode;

    @JsonProperty(value = "saksnummer")
    @Size(max = 19)
    @Pattern(regexp = "^[a-zA-Z0-9]*$")
    private String saksnummer;

    @JsonCreator
    public FinnEllerOpprettSakFnr(@JsonProperty(value = "ytelseType") @Size(max = 20) @Pattern(regexp = "^[\\p{Alnum}æøåÆØÅ_\\-\\.]*$") String ytelseType,
                                  @JsonProperty(value = "søker", required = true) @NotNull @Digits(integer = 11, fraction = 0) String søker,
                                  @JsonProperty(value = "periode", required = true) @NotNull @Valid Periode periode,
                                  @JsonProperty(value = "saksnummer") @Size(max = 19) @Pattern(regexp = "^[a-zA-Z0-9]*$") String saksnummer) {
        this.ytelseType = Objects.requireNonNull(ytelseType, "ytelseType");
        this.søker = søker;
        this.periode = periode;
        this.saksnummer = saksnummer;
    }

    public String getYtelseType() {
        return ytelseType;
    }

    public Periode getPeriode() {
        return periode;
    }

    @StandardAbacAttributt(StandardAbacAttributtType.FNR)
    public String getSøker() {
        return søker;
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
