package no.nav.k9.sak.kontrakt.kompletthet;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.beregningsgrunnlag.kompletthet.Vurdering;
import no.nav.k9.sak.kontrakt.Patterns;
import no.nav.k9.sak.kontrakt.uttak.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class KompletthetsTilstandPåPeriodeDto {

    @NotNull
    @Valid
    @JsonProperty("periode")
    private Periode periode;

    @NotNull
    @Size()
    @Valid
    @JsonProperty("status")
    private List<ArbeidsgiverArbeidsforholdStatus> status;

    @Valid
    @JsonProperty("vurdering")
    private Vurdering vurdering;

    @Valid
    @NotNull
    @JsonProperty("tilVurdering")
    private Boolean tilVurdering;

    @JsonProperty("begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = Patterns.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String begrunnelse;

    @JsonProperty("vurdertAv")
    private String vurdertAv;

    @JsonProperty("vurdertTidspunkt")
    private String vurdertTidspunkt;

    @JsonCreator
    public KompletthetsTilstandPåPeriodeDto(@JsonProperty("periode") Periode periode,
                                            @JsonProperty("status") List<ArbeidsgiverArbeidsforholdStatus> status,
                                            @Valid @JsonProperty("vurdering") Vurdering vurdering,
                                            @Valid @NotNull @JsonProperty("tilVurdering") Boolean tilVurdering,
                                            @JsonProperty("begrunnelse") @Size(max = 4000)
                                            @Pattern(regexp = Patterns.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]") String begrunnelse,
                                            @JsonProperty("vurdertAv") String vurdertAv,
                                            @JsonProperty("vurdertTidspunkt") String vurdertTidspunkt) {
        this.periode = periode;
        this.status = status;
        this.vurdering = vurdering;
        this.tilVurdering = tilVurdering;
        this.begrunnelse = begrunnelse;
        this.vurdertAv = vurdertAv;
        this.vurdertTidspunkt = vurdertTidspunkt;
    }

    public Boolean getTilVurdering() {
        return tilVurdering;
    }

    public Periode getPeriode() {
        return periode;
    }

    public List<ArbeidsgiverArbeidsforholdStatus> getStatus() {
        return status;
    }

    public Vurdering getVurdering() {
        return vurdering;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }
}
