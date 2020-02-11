package no.nav.k9.sak.kontrakt.medlem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
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
public class MedlemDto {

    @JsonProperty(value = "inntekt")
    @Size(max = 100)
    @Valid
    private List<InntektDto> inntekt = Collections.emptyList();

    @JsonProperty(value = "medlemskapPerioder")
    @Size(max = 100)
    @Valid
    private List<MedlemskapPerioderDto> medlemskapPerioder = Collections.emptyList();

    @JsonProperty(value = "oppholdsrettVurdering")
    private Boolean oppholdsrettVurdering;

    @JsonProperty(value = "erEosBorger")
    private Boolean erEosBorger;

    @JsonProperty(value = "lovligOppholdVurdering")
    private Boolean lovligOppholdVurdering;

    @JsonProperty(value = "bosattVurdering")
    private Boolean bosattVurdering;

    @JsonProperty(value = "medlemskapManuellVurderingType")
    @Valid
    private MedlemskapManuellVurderingType medlemskapManuellVurderingType;

    @JsonProperty(value = "fom", required = true)
    @NotNull
    private LocalDate fom; // gjeldendeFra

    @JsonProperty(value = "endringer")
    @Size(max = 100)
    @Valid
    private List<EndringIPersonopplysningDto> endringer = Collections.emptyList();

    public MedlemDto() {
        // trengs for deserialisering av JSON
    }

    public List<InntektDto> getInntekt() {
        return Collections.unmodifiableList(inntekt);
    }

    public List<MedlemskapPerioderDto> getMedlemskapPerioder() {
        return Collections.unmodifiableList(medlemskapPerioder);
    }

    public Boolean getOppholdsrettVurdering() {
        return oppholdsrettVurdering;
    }

    public Boolean getLovligOppholdVurdering() {
        return lovligOppholdVurdering;
    }

    public Boolean getBosattVurdering() {
        return bosattVurdering;
    }

    public MedlemskapManuellVurderingType getMedlemskapManuellVurderingType() {
        return medlemskapManuellVurderingType;
    }

    public LocalDate getFom() {
        return fom;
    }

    public Boolean getErEosBorger() {
        return erEosBorger;
    }

    public List<EndringIPersonopplysningDto> getEndringer() {
        return Collections.unmodifiableList(endringer);
    }

    public void setInntekt(List<InntektDto> inntekt) {
        this.inntekt = List.copyOf(inntekt);
    }

    public void setMedlemskapPerioder(List<MedlemskapPerioderDto> medlemskapPerioder) {
        this.medlemskapPerioder = List.copyOf(medlemskapPerioder);
    }

    public void setOppholdsrettVurdering(Boolean oppholdsrettVurdering) {
        this.oppholdsrettVurdering = oppholdsrettVurdering;
    }

    public void setErEosBorger(Boolean erEosBorger) {
        this.erEosBorger = erEosBorger;
    }

    public void setLovligOppholdVurdering(Boolean lovligOppholdVurdering) {
        this.lovligOppholdVurdering = lovligOppholdVurdering;
    }

    public void setBosattVurdering(Boolean bosattVurdering) {
        this.bosattVurdering = bosattVurdering;
    }

    public void setMedlemskapManuellVurderingType(MedlemskapManuellVurderingType medlemskapManuellVurderingType) {
        this.medlemskapManuellVurderingType = medlemskapManuellVurderingType;
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    public void setEndringer(List<EndringIPersonopplysningDto> endringer) {
        this.endringer = List.copyOf(endringer);
    }
}
