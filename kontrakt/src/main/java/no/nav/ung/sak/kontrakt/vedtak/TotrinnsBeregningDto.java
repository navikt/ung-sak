package no.nav.ung.sak.kontrakt.vedtak;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.ung.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class TotrinnsBeregningDto {

    @JsonProperty(value = "faktaOmBeregningTilfeller")
    @Size(max = 100)
    @Valid
    private List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller = new ArrayList<>();

    @JsonProperty(value = "fastsattVarigEndringNaering")
    private boolean fastsattVarigEndringNaering;

    @JsonProperty(value = "fastsattVarigEndring")
    private Boolean fastsattVarigEndring;

    @JsonProperty(value = "skjæringstidspunkt")
    private LocalDate skjæringstidspunkt;

    public TotrinnsBeregningDto() {
        //
    }

    public List<FaktaOmBeregningTilfelle> getFaktaOmBeregningTilfeller() {
        return Collections.unmodifiableList(faktaOmBeregningTilfeller);
    }

    public void setFaktaOmBeregningTilfeller(List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller) {
        this.faktaOmBeregningTilfeller = List.copyOf(faktaOmBeregningTilfeller);
    }

    public void setFastsattVarigEndringNaering(boolean fastsattVarigEndringNaering) {
        this.fastsattVarigEndringNaering = fastsattVarigEndringNaering;
    }

    public boolean isFastsattVarigEndringNaering() {
        return fastsattVarigEndringNaering;
    }

    public Boolean isFastsattVarigEndring() {
        return fastsattVarigEndring;
    }

    public void setFastsattVarigEndring(Boolean fastsattVarigEndring) {
        this.fastsattVarigEndring = fastsattVarigEndring;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public void setSkjæringstidspunkt(LocalDate skjæringstidspunkt) {
        this.skjæringstidspunkt = skjæringstidspunkt;
    }
}
