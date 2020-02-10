package no.nav.k9.sak.kontrakt.behandling;

import java.util.List;

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
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.KonsekvensForYtelsen;
import no.nav.k9.kodeverk.vedtak.Vedtaksbrev;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BehandlingsresultatDto {

    /** behandlingsresultat id */
    @JsonProperty(value = "id", required = true)
    @NotNull
    @Min(0L)
    @Max(Long.MAX_VALUE)
    private Long id;

    /** behandlingsresultat type. */
    @JsonAlias("behandlingResultatType")
    @JsonProperty(value = "type")
    @Valid
    private BehandlingResultatType type;

    @JsonAlias("avslagsårsak")
    @JsonProperty(value = "avslagsarsak")
    @Valid
    private Avslagsårsak avslagsarsak;

    @JsonProperty(value = "konsekvenserForYtelsen")
    @Size(max = 20)
    @Valid
    private List<KonsekvensForYtelsen> konsekvenserForYtelsen;

    @JsonProperty(value = "vedtaksbrev")
    @Valid
    private Vedtaksbrev vedtaksbrev;

    @JsonProperty(value = "erRevurderingMedUendretUtfall")
    @Valid
    private Boolean erRevurderingMedUendretUtfall;

    @JsonProperty(value = "skjæringstidspunkt", required = true)
    @NotNull
    @Valid
    private SkjæringstidspunktDto skjæringstidspunkt;

    @JsonProperty(value = "overskrift")
    @Size(max = 1000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{P}\\p{M}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String overskrift;

    @JsonProperty(value = "fritekstbrev")
    @Size(max = 100000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{P}\\p{M}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String fritekstbrev;

    @JsonProperty(value = "avslagsarsakFritekst")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{P}\\p{M}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String avslagsarsakFritekst;

    public BehandlingsresultatDto() {
        // trengs for deserialisering av JSON
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setType(BehandlingResultatType type) {
        this.type = type;
    }

    public void setAvslagsarsak(Avslagsårsak avslagsarsak) {
        this.avslagsarsak = avslagsarsak;
    }

    public void setAvslagsarsakFritekst(String avslagsarsakFritekst) {
        this.avslagsarsakFritekst = avslagsarsakFritekst;
    }

    public void setKonsekvenserForYtelsen(List<KonsekvensForYtelsen> konsekvenserForYtelsen) {
        this.konsekvenserForYtelsen = konsekvenserForYtelsen;
    }

    public void setVedtaksbrev(Vedtaksbrev vedtaksbrev) {
        this.vedtaksbrev = vedtaksbrev;
    }

    public Long getId() {
        return id;
    }

    public BehandlingResultatType getType() {
        return type;
    }

    public Avslagsårsak getAvslagsarsak() {
        return avslagsarsak;
    }

    public String getAvslagsarsakFritekst() {
        return avslagsarsakFritekst;
    }

    public List<KonsekvensForYtelsen> getKonsekvenserForYtelsen() {
        return konsekvenserForYtelsen;
    }

    public Vedtaksbrev getVedtaksbrev() {
        return vedtaksbrev;
    }

    public Boolean getErRevurderingMedUendretUtfall() {
        return Boolean.TRUE.equals(erRevurderingMedUendretUtfall);
    }

    public void setErRevurderingMedUendretUtfall(Boolean erRevurderingMedUendretUtfall) {
        this.erRevurderingMedUendretUtfall = erRevurderingMedUendretUtfall;
    }

    public String getOverskrift() {
        return overskrift;
    }

    public void setOverskrift(String overskrift) {
        this.overskrift = overskrift;
    }

    public String getFritekstbrev() {
        return fritekstbrev;
    }

    public void setFritekstbrev(String fritekstbrev) {
        this.fritekstbrev = fritekstbrev;
    }

    public SkjæringstidspunktDto getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public void setSkjæringstidspunkt(SkjæringstidspunktDto skjæringstidspunkt) {
        this.skjæringstidspunkt = skjæringstidspunkt;
    }
}
