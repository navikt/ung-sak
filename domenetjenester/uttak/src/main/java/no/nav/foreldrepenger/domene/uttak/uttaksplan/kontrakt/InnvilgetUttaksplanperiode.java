package no.nav.foreldrepenger.domene.uttak.uttaksplan.kontrakt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.uttak.UtfallType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName("Innvilget")
public class InnvilgetUttaksplanperiode extends Uttaksplanperiode {

    @JsonProperty(value = "grad")
    @Min(0)
    @Max(100)
    private int grad;

    /**
     * Utbetalingsgrad per arbeidsforhold.
     */
    @JsonProperty(value = "utbetalingsgrader")
    @Valid
    private List<UttakUtbetalingsgrad> utbetalingsgrader = Collections.emptyList();

    @JsonCreator
    public InnvilgetUttaksplanperiode(@JsonProperty(value = "grad") @Min(0) @Max(100) int grad,
                                      @JsonProperty(value = "utbetalingsgrader", required = true) @Valid List<UttakUtbetalingsgrad> utbetalingsgrader) {
        this.grad = grad;
        setUtbetalingsgrader(utbetalingsgrader);
    }

    @Override
    public UtfallType getUtfall() {
        return UtfallType.INNVILGET;
    }

    public int getGrad() {
        return grad;
    }

    public List<UttakUtbetalingsgrad> getUtbetalingsgrader() {
        return Collections.unmodifiableList(utbetalingsgrader);
    }

    private void setUtbetalingsgrader(List<UttakUtbetalingsgrad> utbetalingsgrader) {
        if (utbetalingsgrader != null) {
            this.utbetalingsgrader = new ArrayList<>(utbetalingsgrader);
            Collections.sort(this.utbetalingsgrader);
        }
    }

    public Optional<UttakUtbetalingsgrad> getUtbetalingsgrad(UttakArbeidsforhold key) {
        return getUtbetalingsgrader().stream().filter(u -> Objects.equals(key, u.getArbeidsforhold())).findFirst();
    }

}
