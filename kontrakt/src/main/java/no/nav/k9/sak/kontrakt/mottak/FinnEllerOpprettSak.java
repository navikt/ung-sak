package no.nav.k9.sak.kontrakt.mottak;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

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
public class FinnEllerOpprettSak {

    @JsonProperty(value = "ytelseType")
    @Size(max = 20)
    @Pattern(regexp = "^[\\p{Alnum}æøåÆØÅ_\\-\\.]*$")
    private String ytelseType;

    @JsonProperty(value = "aktørId", required = true)
    @NotNull
    @Digits(integer = 19, fraction = 0)
    private String aktørId;

    @JsonProperty(value = "pleietrengendeAktørId")
    @Digits(integer = 19, fraction = 0)
    private String pleietrengendeAktørId;

    @JsonProperty(value = "periode", required = true)
    @NotNull
    @Valid
    private Periode periode;

    @JsonCreator
    public FinnEllerOpprettSak(@JsonProperty(value = "ytelseType") @Size(max = 20) @Pattern(regexp = "^[\\p{Alnum}æøåÆØÅ_\\-\\.]*$") String ytelseType,
                               @JsonProperty(value = "aktørId", required = true) @NotNull @Digits(integer = 19, fraction = 0) String aktørId,
                               @JsonProperty(value = "pleietrengendeAktørId") @Digits(integer = 19, fraction = 0) String pleietrengendeAktørId,
                               @JsonProperty(value = "periode", required = true) @NotNull @Valid Periode periode) {
        this.ytelseType = Objects.requireNonNull(ytelseType, "ytelseType");
        this.aktørId = aktørId;
        this.pleietrengendeAktørId = pleietrengendeAktørId;
        this.periode = periode;
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

    @Override
    public String toString() {
        return getClass().getSimpleName()
            + "<ytelseType=" + ytelseType
            + ", periode=" + getPeriode()
            + ">";
    }

}
