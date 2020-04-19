package no.nav.k9.sak.kontrakt.opptjening;

import java.time.LocalDate;
import java.util.Objects;

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

import no.nav.k9.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.k9.kodeverk.arbeidsforhold.YtelseType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class InntektDto {

    @JsonProperty(value = "fom", required = true)
    @NotNull
    private LocalDate fom;

    @JsonProperty(value = "tom", required = true)
    @NotNull
    private LocalDate tom;

    @JsonProperty(value = "utbetaler")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}\\p{M}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String utbetaler;

    /** Inntektspost - om det er vanlig LØNN, en ytelse, etc.*/
    @JsonProperty(value = "inntektspostType")
    private InntektspostType inntektspostType;

    /** Hvis {@link #inntektspostType} er en YTELSE, så angir dette ytelse type. */
    @JsonProperty(value = "ytelseType")
    private YtelseType ytelseType;

    @JsonProperty(value = "belop")
    @Min(0)
    @Max(10 * 1000 * 1000)
    private Integer belop;

    /** @deprecated bruk heller #inntektspostType. */
    @Deprecated(forRemoval = true)
    @JsonProperty(value = "ytelse")
    private Boolean ytelse;

    /** @deprecated skal ikke trenge navn her - er samme som søkers navn. */
    @Deprecated(forRemoval = true)
    @JsonProperty(value = "navn")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{N}\\p{M}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String navn;

    public InntektDto() {
        // trengs for deserialisering av JSON
    }

    public Integer getBelop() {
        return belop;
    }

    public LocalDate getFom() {
        return fom;
    }

    public String getNavn() {
        return navn;
    }

    public LocalDate getTom() {
        return tom;
    }

    public String getUtbetaler() {
        return utbetaler;
    }

    public Boolean getYtelse() {
        return ytelse;
    }

    public void setBelop(Integer belop) {
        this.belop = belop;
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    public void setTom(LocalDate tom) {
        this.tom = tom;
    }

    public void setUtbetaler(String utbetaler) {
        this.utbetaler = utbetaler;
    }

    public void setYtelse(Boolean ytelse) {
        this.ytelse = ytelse;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        var other = this.getClass().cast(obj);

        return Objects.equals(fom, other.fom)
            && Objects.equals(tom, other.tom)
            && Objects.equals(utbetaler, other.utbetaler)
            && Objects.equals(ytelse, other.ytelse)
            && Objects.equals(navn, other.navn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fom, tom, utbetaler, ytelse, navn);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<utbetaler=" + utbetaler + ", fom=" + fom + ", tom=" + tom + ", ytelse=" + ytelse + ", navn=" + navn + ", beløp=" + belop + ">";
    }

    public void setInntektspostType(InntektspostType inntektspostType) {
        this.inntektspostType = inntektspostType;

    }

    public void setYtelseType(YtelseType ytelseType) {
        this.ytelseType = ytelseType;
    }

}
