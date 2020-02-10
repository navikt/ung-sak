package no.nav.foreldrepenger.dokumentbestiller.dto;

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

import no.nav.k9.kodeverk.dokument.DokumentMalType;
import no.nav.k9.sak.kontrakt.abac.AbacAttributt;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BestillBrevDto {

    @JsonProperty(value = "behandlingId", required = true)
    @NotNull
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long behandlingId;

    @JsonProperty(value = "mottaker", required = true)
    @NotNull
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{P}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    @Size(max = 256)
    private String mottaker;

    @JsonProperty(value = "brevmalkode", required = true)
    @NotNull
    @Size(min = 1, max = 100)
    @Pattern(regexp = "^[\\p{L}\\p{N}_\\.\\-/]+$")
    private String brevmalkode;

    @JsonProperty(value = "fritekst")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    public String fritekst;

    @JsonProperty(value = "arsakskode")
    @Size(min = 1, max = 100)
    @Pattern(regexp = "^[\\p{L}\\p{N}_\\.\\-/]+$")
    public String arsakskode;

    public BestillBrevDto() { // NOSONAR
    }

    public BestillBrevDto(long behandlingId, DokumentMalType dokumentMalType, String fritekst, String arsakskode) { // NOSONAR
        this.behandlingId = behandlingId;
        this.brevmalkode = dokumentMalType == null ? null : dokumentMalType.getKode();
        this.fritekst = fritekst;
        this.mottaker = "Søker";
        this.arsakskode = arsakskode;
    }

    public BestillBrevDto(long behandlingId, DokumentMalType dokumentMalType) {
        this(behandlingId, dokumentMalType, null, null);
    }

    public BestillBrevDto(long behandlingId, DokumentMalType dokumentMalType, String fritekst) {
        this(behandlingId, dokumentMalType, fritekst, null);
    }

    @AbacAttributt("behandlingId")
    public Long getBehandlingId() {
        return behandlingId;
    }

    public String getÅrsakskode() {
        return arsakskode;
    }

    public void setÅrsakskode(String årsakskode) {
        this.arsakskode = årsakskode;
    }

    public void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    public String getMottaker() {
        return mottaker;
    }

    public void setMottaker(String mottaker) {
        this.mottaker = mottaker;
    }

    public String getBrevmalkode() {
        return brevmalkode;
    }

    public void setBrevmalkode(String brevmalkode) {
        this.brevmalkode = brevmalkode;
    }

    public String getFritekst() {
        return fritekst;
    }

    public void setFritekst(String fritekst) {
        this.fritekst = fritekst;
    }

}
