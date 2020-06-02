package no.nav.k9.sak.kontrakt.behandling;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.abac.AbacAttributt;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class ByttBehandlendeEnhetDto {

    @JsonProperty(value = "begrunnelse", required = true)
    @NotNull
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String begrunnelse;

    @JsonProperty(value = "behandlingId", required = true)
    @NotNull
    @Min(0)
    @Max(Long.MAX_VALUE)
    // TODO (BehandlingIdDto): bør kunne støtte behandlingUuid også?
    private Long behandlingId;

    @JsonAlias("versjon")
    @JsonProperty(value = "behandlingVersjon", required = true)
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long behandlingVersjon;

    @JsonProperty(value = "enhetId", required = true)
    @NotNull
    @Size(max = 10)
    @Pattern(regexp = "^[\\p{L}\\p{N}_\\.\\-/]+$")
    private String enhetId;

    @JsonProperty(value = "enhetNavn")
    @Size(min = 1, max = 256)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String enhetNavn;

    public String getBegrunnelse() {
        return begrunnelse;
    }

    @AbacAttributt("behandlingId")
    public Long getBehandlingId() {
        return behandlingId;
    }

    public Long getBehandlingVersjon() {
        return behandlingVersjon;
    }

    public String getEnhetId() {
        return enhetId;
    }

    public String getEnhetNavn() {
        return enhetNavn;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    public void setBehandlingVersjon(Long behandlingVersjon) {
        this.behandlingVersjon = behandlingVersjon;
    }

    public void setEnhetId(String enhetId) {
        this.enhetId = enhetId;
    }

    public void setEnhetNavn(String enhetNavn) {
        this.enhetNavn = enhetNavn;
    }

}
