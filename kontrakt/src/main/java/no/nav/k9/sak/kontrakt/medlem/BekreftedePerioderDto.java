package no.nav.k9.sak.kontrakt.medlem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.medlem.MedlemskapManuellVurderingType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BekreftedePerioderDto {

    @JsonProperty(value = "vurderingdato", required = true)
    @NotNull
    private LocalDate vurderingsdato;

    @JsonProperty(value = "aksjonspunkter")
    @Size(max = 100)
    @Valid
    private List<@NotNull @Pattern(regexp = "^[\\p{Alnum}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'") String> aksjonspunkter = new ArrayList<>();

    @JsonProperty(value = "bosattVurdering")
    private Boolean bosattVurdering;

    @JsonProperty(value = "erEosBorger")
    private Boolean erEosBorger;

    @JsonProperty(value = "oppholdsrettVurdering")
    private Boolean oppholdsrettVurdering;

    @JsonProperty(value = "lovligOppholdVurdering")
    private Boolean lovligOppholdVurdering;

    @JsonProperty(value = "fodselsdato")
    private LocalDate fodselsdato;

    @JsonProperty(value = "medlemskapManuellVurderingType")
    private MedlemskapManuellVurderingType medlemskapManuellVurderingType;

    @JsonProperty(value = "omsorgsovertakelseDato")
    private LocalDate omsorgsovertakelseDato;

    @JsonProperty(value = "begrunnelse")
    @Size(max = 5000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String begrunnelse;

    public BekreftedePerioderDto() {
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public LocalDate getVurderingsdato() {
        return vurderingsdato;
    }

    public void setVurderingsdato(LocalDate vurderingsdato) {
        this.vurderingsdato = vurderingsdato;
    }

    public List<String> getAksjonspunkter() {
        return aksjonspunkter;
    }

    public void setAksjonspunkter(List<String> aksjonspunkter) {
        this.aksjonspunkter = aksjonspunkter;
    }

    public Boolean getBosattVurdering() {
        return bosattVurdering;
    }

    public void setBosattVurdering(Boolean bosattVurdering) {
        this.bosattVurdering = bosattVurdering;
    }

    public Boolean getErEosBorger() {
        return erEosBorger;
    }

    public void setErEosBorger(Boolean erEosBorger) {
        this.erEosBorger = erEosBorger;
    }

    public Boolean getOppholdsrettVurdering() {
        return oppholdsrettVurdering;
    }

    public void setOppholdsrettVurdering(Boolean oppholdsrettVurdering) {
        this.oppholdsrettVurdering = oppholdsrettVurdering;
    }

    public Boolean getLovligOppholdVurdering() {
        return lovligOppholdVurdering;
    }

    public void setLovligOppholdVurdering(Boolean lovligOppholdVurdering) {
        this.lovligOppholdVurdering = lovligOppholdVurdering;
    }

    public LocalDate getFodselsdato() {
        return fodselsdato;
    }

    public void setFodselsdato(LocalDate fodselsdato) {
        this.fodselsdato = fodselsdato;
    }

    public MedlemskapManuellVurderingType getMedlemskapManuellVurderingType() {
        return medlemskapManuellVurderingType;
    }

    public void setMedlemskapManuellVurderingType(MedlemskapManuellVurderingType medlemskapManuellVurderingType) {
        this.medlemskapManuellVurderingType = medlemskapManuellVurderingType;
    }

    public LocalDate getOmsorgsovertakelseDato() {
        return omsorgsovertakelseDato;
    }

    public void setOmsorgsovertakelseDato(LocalDate omsorgsovertakelseDato) {
        this.omsorgsovertakelseDato = omsorgsovertakelseDato;
    }
}
