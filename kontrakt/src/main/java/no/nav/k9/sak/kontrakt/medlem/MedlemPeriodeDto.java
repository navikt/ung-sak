package no.nav.k9.sak.kontrakt.medlem;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

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
import no.nav.k9.kodeverk.medlem.VurderingsÅrsak;
import no.nav.k9.sak.kontrakt.person.PersonopplysningDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class MedlemPeriodeDto {

    @JsonProperty(value = "aksjonspunkter")
    @Size(max = 10)
    @Valid
    private Set<String> aksjonspunkter = Collections.emptySet();

    @JsonProperty(value = "årsaker")
    @Size(max = 10)
    @Valid
    private Set<VurderingsÅrsak> årsaker = Collections.emptySet();

    @JsonProperty(value = "begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}\\p{M}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String begrunnelse;

    @JsonProperty(value = "bosattVurdering")
    private Boolean bosattVurdering;

    @JsonProperty(value = "erEosBorger")
    private Boolean erEosBorger;

    @JsonProperty(value = "lovligOppholdVurdering")
    private Boolean lovligOppholdVurdering;

    @JsonProperty(value = "medlemskapManuellVurderingType")
    private MedlemskapManuellVurderingType medlemskapManuellVurderingType;

    @JsonProperty(value = "oppholdsrettVurdering")
    private Boolean oppholdsrettVurdering;

    @JsonProperty(value = "personopplysninger", required = true)
    @Valid
    @NotNull
    private PersonopplysningDto personopplysninger;

    @JsonProperty(value = "vurderingsdato")
    private LocalDate vurderingsdato;

    public MedlemPeriodeDto() {
        // trengs for deserialisering av JSON
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MedlemPeriodeDto that = (MedlemPeriodeDto) o;
        return Objects.equals(vurderingsdato, that.vurderingsdato);
    }

    public Set<String> getAksjonspunkter() {
        return Collections.unmodifiableSet(aksjonspunkter);
    }

    public Set<VurderingsÅrsak> getÅrsaker() {
        return Collections.unmodifiableSet(årsaker);
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

    public Boolean getLovligOppholdVurdering() {
        return lovligOppholdVurdering;
    }

    public MedlemskapManuellVurderingType getMedlemskapManuellVurderingType() {
        return medlemskapManuellVurderingType;
    }

    public Boolean getOppholdsrettVurdering() {
        return oppholdsrettVurdering;
    }

    public PersonopplysningDto getPersonopplysninger() {
        return personopplysninger;
    }

    public LocalDate getVurderingsdato() {
        return vurderingsdato;
    }

    @Override
    public int hashCode() {
        return Objects.hash(vurderingsdato);
    }

    public void setAksjonspunkter(Set<String> aksjonspunkter) {
        this.aksjonspunkter = Set.copyOf(aksjonspunkter);
    }

    public void setÅrsaker(Set<VurderingsÅrsak> årsaker) {
        this.årsaker = Set.copyOf(årsaker);
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

    public void setLovligOppholdVurdering(Boolean lovligOppholdVurdering) {
        this.lovligOppholdVurdering = lovligOppholdVurdering;
    }

    public void setMedlemskapManuellVurderingType(MedlemskapManuellVurderingType medlemskapManuellVurderingType) {
        this.medlemskapManuellVurderingType = medlemskapManuellVurderingType;
    }

    public void setOppholdsrettVurdering(Boolean oppholdsrettVurdering) {
        this.oppholdsrettVurdering = oppholdsrettVurdering;
    }

    public void setPersonopplysninger(PersonopplysningDto personopplysninger) {
        this.personopplysninger = personopplysninger;
    }

    public void setVurderingsdato(LocalDate vurderingsdato) {
        this.vurderingsdato = vurderingsdato;
    }
}
