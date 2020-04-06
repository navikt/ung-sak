package no.nav.k9.sak.kontrakt.mottak;

import java.time.LocalDate;

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

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class FinnEllerOpprettSak {

    /** @deprecated erstattes av ytelseType. */
    @Deprecated(forRemoval = true)
    @JsonProperty(value = "behandlingstemaOffisiellKode")
    @Size(max = 8)
    @Pattern(regexp = "^[\\p{Alnum}æøåÆØÅ_\\-\\.]*$")
    private String behandlingstemaOffisiellKode;

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

    @JsonProperty(value = "periodeStart")
    private LocalDate periodeStart;

    @JsonCreator
    public FinnEllerOpprettSak(@JsonProperty(value = "behandlingstemaOffisiellKode") @Size(max = 8) @Pattern(regexp = "^[\\p{Alnum}æøåÆØÅ_\\-\\.]*$") String behandlingstemaOffisiellKode,
                               @JsonProperty(value = "ytelseType") @Size(max = 20) @Pattern(regexp = "^[\\p{Alnum}æøåÆØÅ_\\-\\.]*$") String ytelseType,
                               @JsonProperty(value = "aktørId", required = true) @NotNull @Digits(integer = 19, fraction = 0) String aktørId,
                               @JsonProperty(value = "pleietrengendeAktørId") @Digits(integer = 19, fraction = 0) String pleietrengendeAktørId,
                               @JsonProperty(value = "periodeStart") LocalDate periodeStart) {
        if(behandlingstemaOffisiellKode==null && ytelseType==null) {
            throw new IllegalArgumentException("Må oppgi enten behandlinstema eller ytelseType");
        }
        this.behandlingstemaOffisiellKode = behandlingstemaOffisiellKode;
        this.ytelseType = ytelseType;
        this.aktørId = aktørId;
        this.pleietrengendeAktørId = pleietrengendeAktørId;
        this.periodeStart = periodeStart;
    }

    public String getBehandlingstemaOffisiellKode() {
        return behandlingstemaOffisiellKode;
    }
    
    public String getYtelseType() {
        return ytelseType;
    }

    public LocalDate getPeriodeStart() {
        return periodeStart;
    }

    @AbacAttributt(value = "aktorId", masker = true)
    public String getAktørId() {
        return aktørId;
    }

    public String getPleietrengendeAktørId() {
        return pleietrengendeAktørId;
    }

}
