package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

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

    @NotNull
    @JsonProperty(required = true, value = "erForlengelse")
    private boolean erForlengelse;

    @JsonCreator
    public BeregningsgrunnlagKoblingDto(@JsonProperty(required = true, value = "skjæringstidspunkt") LocalDate skjæringstidspunkt,
                                        @JsonProperty(required = true, value = "referanse") UUID referanse,
                                        @JsonProperty(required = true, value = "erForlengelse") boolean erForlengelse) {
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.referanse = referanse;
        this.erForlengelse = erForlengelse;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public UUID getReferanse() {
        return referanse;
    }

    public boolean getErForlengelse() {
        return erForlengelse;
    }
}
