package no.nav.k9.sak.kontrakt.vedtak;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

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

    @JsonAlias("avslagsårsaker")
    @JsonProperty(value = "avslagsarsaker")
    @Valid
    private Map<VilkårType, Set<Avslagsårsak>> avslagsarsaker;

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "avslagsarsakFritekst")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{P}\\p{M}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String avslagsarsakFritekst;

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "erRevurderingMedUendretUtfall")
    @Valid
    private Boolean erRevurderingMedUendretUtfall;

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "fritekstbrev")
    @Size(max = 100000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{P}\\p{M}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String fritekstbrev;

    /**
     * behandlingsresultat id
     */
    @JsonProperty(value = "id", required = true)
    @NotNull
    @Min(0L)
    @Max(Long.MAX_VALUE)
    private Long id;

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "overskrift")
    @Size(max = 1000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{P}\\p{M}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
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

    @JsonProperty(value = "redusertUtbetalingÅrsaker")
    @Size(max = 50)
    @Valid
    private Set<String> redusertUtbetalingÅrsaker = Collections.emptySet();

    public VedtakVarselDto() {
        // trengs for deserialisering av JSON
    }

    public String getAvslagsarsakFritekst() {
        return avslagsarsakFritekst;
    }

    public void setAvslagsarsakFritekst(String avslagsarsakFritekst) {
        this.avslagsarsakFritekst = avslagsarsakFritekst;
    }

    public Boolean getErRevurderingMedUendretUtfall() {
        return Boolean.TRUE.equals(erRevurderingMedUendretUtfall);
    }

    public void setErRevurderingMedUendretUtfall(Boolean erRevurderingMedUendretUtfall) {
        this.erRevurderingMedUendretUtfall = erRevurderingMedUendretUtfall;
    }

    public String getFritekstbrev() {
        return fritekstbrev;
    }

    public Set<String> getRedusertUtbetalingÅrsaker() {
        return Set.copyOf(redusertUtbetalingÅrsaker);
    }

    public void setFritekstbrev(String fritekstbrev) {
        this.fritekstbrev = fritekstbrev;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOverskrift() {
        return overskrift;
    }

    public void setOverskrift(String overskrift) {
        this.overskrift = overskrift;
    }

    public SkjæringstidspunktDto getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public void setSkjæringstidspunkt(SkjæringstidspunktDto skjæringstidspunkt) {
        this.skjæringstidspunkt = skjæringstidspunkt;
    }

    public BehandlingResultatType getType() {
        return type;
    }

    public void setType(BehandlingResultatType type) {
        this.type = type;
    }

    public Vedtaksbrev getVedtaksbrev() {
        return vedtaksbrev;
    }

    public void setVedtaksbrev(Vedtaksbrev vedtaksbrev) {
        this.vedtaksbrev = vedtaksbrev;
    }

    public LocalDate getVedtaksdato() {
        return vedtaksdato;
    }

    public void setVedtaksdato(LocalDate vedtaksdato) {
        this.vedtaksdato = vedtaksdato;
    }

    public Map<VilkårType, VilkårResultatDto> getVilkårResultat() {
        return vilkårResultat;
    }

    public void setVilkårResultat(Map<VilkårType, VilkårResultatDto> vilkårResultat) {
        this.vilkårResultat = vilkårResultat;
    }

    public Avslagsårsak getAvslagsarsak() {
        return avslagsarsak;
    }

    public void setAvslagsarsak(Avslagsårsak avslagsarsak) {
        this.avslagsarsak = avslagsarsak;
    }

    public Map<VilkårType, Set<Avslagsårsak>> getAvslagsarsaker() {
        return avslagsarsaker;
    }

    public void setAvslagsarsaker(Map<VilkårType, Set<Avslagsårsak>> avslagsarsaker) {
        this.avslagsarsaker = avslagsarsaker;
    }

    public void setRedusertUtbetalingÅrsaker(Set<String> redusertUtbetalingÅrsaker) {
        this.redusertUtbetalingÅrsaker =
            redusertUtbetalingÅrsaker == null ? Collections.emptySet() : Set.copyOf(redusertUtbetalingÅrsaker);
    }
}
