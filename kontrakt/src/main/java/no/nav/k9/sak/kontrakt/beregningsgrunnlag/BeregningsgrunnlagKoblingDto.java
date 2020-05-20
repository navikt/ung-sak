package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BeregningsgrunnlagKoblingDto {

    @NotNull
    @JsonProperty(required = true, value = "skjæringstidspunkt")
    private LocalDate skjæringstidspunkt;

    @NotNull
    @JsonProperty(required = true, value = "referanse")
    private UUID referanse;

    @JsonCreator
    public BeregningsgrunnlagKoblingDto(@JsonProperty(required = true, value = "skjæringstidspunkt") @NotNull LocalDate skjæringstidspunkt,
                                        @JsonProperty(required = true, value = "referanse") @NotNull UUID referanse) {
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.referanse = referanse;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public UUID getReferanse() {
        return referanse;
    }
}
