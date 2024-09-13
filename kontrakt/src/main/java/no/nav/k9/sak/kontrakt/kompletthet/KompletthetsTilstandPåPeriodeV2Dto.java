package no.nav.k9.sak.kontrakt.kompletthet;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.kodeverk.beregningsgrunnlag.kompletthet.Vurdering;
import no.nav.k9.sak.kontrakt.Patterns;
import no.nav.k9.sak.kontrakt.uttak.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class KompletthetsTilstandPåPeriodeV2Dto {

    @NotNull
    @Valid
    @JsonProperty("periode")
    private Periode periode;

    @NotNull
    @Size()
    @Valid
    @JsonProperty("status")
    private List<ArbeidsgiverArbeidsforholdStatusV2> status;

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

    @JsonCreator
    public KompletthetsTilstandPåPeriodeV2Dto(@JsonProperty("periode") Periode periode,
                                              @JsonProperty("status") List<ArbeidsgiverArbeidsforholdStatusV2> status,
                                              @Valid @JsonProperty("vurdering") Vurdering vurdering,
                                              @Valid @NotNull @JsonProperty("tilVurdering") Boolean tilVurdering,
                                              @JsonProperty("begrunnelse") @Size(max = 4000)
                                              @Pattern(regexp = Patterns.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]") String begrunnelse) {
        this.periode = periode;
        this.status = status;
        this.vurdering = vurdering;
        this.begrunnelse = begrunnelse;
        this.tilVurdering = tilVurdering;
    }

    public Boolean getTilVurdering() {
        return tilVurdering;
    }

    public Periode getPeriode() {
        return periode;
    }

    public List<ArbeidsgiverArbeidsforholdStatusV2> getStatus() {
        return status;
    }

    public Vurdering getVurdering() {
        return vurdering;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }
}
