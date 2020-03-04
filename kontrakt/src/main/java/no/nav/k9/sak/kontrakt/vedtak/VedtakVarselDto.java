package no.nav.k9.sak.kontrakt.vedtak;

import java.time.LocalDate;
import java.util.Map;

import javax.validation.Valid;
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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.vedtak.Vedtaksbrev;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.kontrakt.behandling.SkjæringstidspunktDto;
import no.nav.k9.sak.kontrakt.vilkår.VilkårResultatDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class VedtakVarselDto {

    @JsonAlias("avslagsårsak")
    @JsonProperty(value = "avslagsarsak")
    @Valid
    private Avslagsårsak avslagsarsak;

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "avslagsarsakFritekst")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{P}\\p{M}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String avslagsarsakFritekst;

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "erRevurderingMedUendretUtfall")
    @Valid
    private Boolean erRevurderingMedUendretUtfall;

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "fritekstbrev")
    @Size(max = 100000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{P}\\p{M}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String fritekstbrev;

    /** behandlingsresultat id */
    @JsonProperty(value = "id", required = true)
    @NotNull
    @Min(0L)
    @Max(Long.MAX_VALUE)
    private Long id;

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "overskrift")
    @Size(max = 1000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{P}\\p{M}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String overskrift;

    @JsonProperty(value = "skjæringstidspunkt", required = true)
    @NotNull
    @Valid
    private SkjæringstidspunktDto skjæringstidspunkt;

    @JsonAlias("behandlingResultatType")
    @JsonProperty(value = "type")
    @Valid
    private BehandlingResultatType type;

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "vedtaksbrev")
    @Valid
    private Vedtaksbrev vedtaksbrev;

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "vilkårResultat")
    @Valid
    private Map<VilkårType, VilkårResultatDto> vilkårResultat;

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "vedtaksdato")
    @Valid
    private LocalDate vedtaksdato;

    public VedtakVarselDto() {
        // trengs for deserialisering av JSON
    }

    public String getAvslagsarsakFritekst() {
        return avslagsarsakFritekst;
    }

    public Boolean getErRevurderingMedUendretUtfall() {
        return Boolean.TRUE.equals(erRevurderingMedUendretUtfall);
    }

    public String getFritekstbrev() {
        return fritekstbrev;
    }

    public Long getId() {
        return id;
    }

    public String getOverskrift() {
        return overskrift;
    }

    public SkjæringstidspunktDto getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public BehandlingResultatType getType() {
        return type;
    }

    public Vedtaksbrev getVedtaksbrev() {
        return vedtaksbrev;
    }

    public LocalDate getVedtaksdato() {
        return vedtaksdato;
    }
    
    public Map<VilkårType, VilkårResultatDto> getVilkårResultat() {
        return vilkårResultat;
    }

    public void setAvslagsarsakFritekst(String avslagsarsakFritekst) {
        this.avslagsarsakFritekst = avslagsarsakFritekst;
    }

    public void setErRevurderingMedUendretUtfall(Boolean erRevurderingMedUendretUtfall) {
        this.erRevurderingMedUendretUtfall = erRevurderingMedUendretUtfall;
    }

    public void setFritekstbrev(String fritekstbrev) {
        this.fritekstbrev = fritekstbrev;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOverskrift(String overskrift) {
        this.overskrift = overskrift;
    }

    public void setSkjæringstidspunkt(SkjæringstidspunktDto skjæringstidspunkt) {
        this.skjæringstidspunkt = skjæringstidspunkt;
    }

    public void setType(BehandlingResultatType type) {
        this.type = type;
    }

    public void setVedtaksbrev(Vedtaksbrev vedtaksbrev) {
        this.vedtaksbrev = vedtaksbrev;
    }

    public void setVilkårResultat(Map<VilkårType, VilkårResultatDto> vilkårResultat) {
        this.vilkårResultat = vilkårResultat;
    }

    public void setVedtaksdato(LocalDate vedtaksdato) {
        this.vedtaksdato = vedtaksdato;
    }
}
