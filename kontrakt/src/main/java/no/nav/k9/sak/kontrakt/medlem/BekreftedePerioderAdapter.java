package no.nav.k9.sak.kontrakt.medlem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
public class BekreftedePerioderAdapter {

    @JsonProperty(value = "aksjonspunkter")
    @Size(max = 100)
    private List<String> aksjonspunkter = new ArrayList<>();

    @JsonProperty(value = "begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String begrunnelse;

    @JsonProperty(value = "bosattVurdering")
    private Boolean bosattVurdering;

    @JsonProperty(value = "erEosBorger")
    private Boolean erEosBorger;

    @JsonProperty(value = "fodselsdato")
    private LocalDate fodselsdato;

    @JsonProperty(value = "lovligOppholdVurdering")
    private Boolean lovligOppholdVurdering;

    @JsonProperty(value = "medlemskapManuellVurderingType")
    private MedlemskapManuellVurderingType medlemskapManuellVurderingType;

    @JsonProperty(value = "omsorgsovertakelseDato")
    private LocalDate omsorgsovertakelseDato;

    @JsonProperty(value = "oppholdsrettVurdering")
    private Boolean oppholdsrettVurdering;

    @JsonProperty(value = "vurderingdato")
    private LocalDate vurderingsdato;

    public BekreftedePerioderAdapter() {
    }

    public List<String> getAksjonspunkter() {
        return Collections.unmodifiableList(aksjonspunkter);
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public Boolean getBosattVurdering() {
        return bosattVurdering;
    }

    public Boolean getErEosBorger() {
        return erEosBorger;
    }

    public LocalDate getFodselsdato() {
        return fodselsdato;
    }

    public Boolean getLovligOppholdVurdering() {
        return lovligOppholdVurdering;
    }

    public MedlemskapManuellVurderingType getMedlemskapManuellVurderingType() {
        return medlemskapManuellVurderingType;
    }

    public LocalDate getOmsorgsovertakelseDato() {
        return omsorgsovertakelseDato;
    }

    public Boolean getOppholdsrettVurdering() {
        return oppholdsrettVurdering;
    }

    public LocalDate getVurderingsdato() {
        return vurderingsdato;
    }

    public void setAksjonspunkter(List<String> aksjonspunkter) {
        this.aksjonspunkter = List.copyOf(aksjonspunkter);
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public void setBosattVurdering(Boolean bosattVurdering) {
        this.bosattVurdering = bosattVurdering;
    }

    public void setErEosBorger(Boolean erEosBorger) {
        this.erEosBorger = erEosBorger;
    }

    public void setFodselsdato(LocalDate fodselsdato) {
        this.fodselsdato = fodselsdato;
    }

    public void setLovligOppholdVurdering(Boolean lovligOppholdVurdering) {
        this.lovligOppholdVurdering = lovligOppholdVurdering;
    }

    public void setMedlemskapManuellVurderingType(MedlemskapManuellVurderingType medlemskapManuellVurderingType) {
        this.medlemskapManuellVurderingType = medlemskapManuellVurderingType;
    }

    public void setOmsorgsovertakelseDato(LocalDate omsorgsovertakelseDato) {
        this.omsorgsovertakelseDato = omsorgsovertakelseDato;
    }

    public void setOppholdsrettVurdering(Boolean oppholdsrettVurdering) {
        this.oppholdsrettVurdering = oppholdsrettVurdering;
    }

    public void setVurderingsdato(LocalDate vurderingsdato) {
        this.vurderingsdato = vurderingsdato;
    }
}
