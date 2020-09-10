package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.Periode;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class BekreftetBeregningsgrunnlagDto {

    @JsonProperty(value = "periode")
    @Valid
    @NotNull
    private Periode periode;

    protected BekreftetBeregningsgrunnlagDto() {
        // For Jackson
    }

    public BekreftetBeregningsgrunnlagDto(@Valid @NotNull Periode periode) {
        this.periode = periode;
    }

    public Periode getPeriode() {
        return periode;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BekreftetBeregningsgrunnlagDto that = (BekreftetBeregningsgrunnlagDto) o;
        return Objects.equals(periode, that.periode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode);
    }

    @Override
    public String toString() {
        return "BekreftetBeregningsgrunnlagDto{" +
            "periode=" + periode +
            '}';
    }
}
