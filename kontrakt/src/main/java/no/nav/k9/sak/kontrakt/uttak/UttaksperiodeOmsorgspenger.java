package no.nav.k9.sak.kontrakt.uttak;

import com.fasterxml.jackson.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class UttaksperiodeOmsorgspenger {

    @JsonProperty(value = "periode", required = true)
    @Valid
    @NotNull
    private Periode periode;

    @JsonProperty(value="utbetalingsgrad", required = false)
    @Valid
    private UttakUtbetalingsgradOmsorgspenger utbetalingsgrad;

    @JsonProperty(value="innsendingstidspunkt", required = false)
    @Valid
    private LocalDateTime innsendingstidspunkt;

    @JsonProperty(value="lengde", required=false)
    @Valid
    private Duration lengde;

    @JsonProperty(value="arbeidsforhold", required = false)
    @Valid
    @NotNull
    private UttakArbeidsforhold uttakArbeidsforhold;

    @JsonProperty(value="utfall", required=false)
    @Valid
    private OmsorgspengerUtfall utfall;

    @JsonCreator
    public UttaksperiodeOmsorgspenger(@JsonProperty(value = "periode", required = true) @Valid @NotNull Periode periode,
                                      @JsonProperty(value = "utbetalingsgrad", required = false) @Valid UttakUtbetalingsgradOmsorgspenger utbetalingsgrad,
                                      @JsonProperty(value = "innsendingstidspunkt", required = false) @Valid LocalDateTime innsendingstidspunkt,
                                      @JsonProperty(value = "utfall", required = false) @Valid OmsorgspengerUtfall utfall,
                                      @JsonProperty(value = "lengde", required = false) @Valid Duration lengde,
                                      @JsonProperty(value = "arbeidsforhold", required = false) @Valid UttakArbeidsforhold uttakArbeidsforhold) {
        this.periode = periode;
        this.utbetalingsgrad = utbetalingsgrad;
        this.innsendingstidspunkt = innsendingstidspunkt;
        this.utfall = utfall;
        this.lengde = lengde;
        this.uttakArbeidsforhold = uttakArbeidsforhold;
    }

    public Periode getPeriode() {
        return periode;
    }

    public void setPeriode(Periode periode) {
        this.periode = periode;
    }

    public UttakUtbetalingsgradOmsorgspenger getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    public void setUtbetalingsgrad(UttakUtbetalingsgradOmsorgspenger utbetalingsgrad) {
        this.utbetalingsgrad = utbetalingsgrad;
    }

    public LocalDate getFom() {
        return getPeriode().getFom();
    }

    public LocalDate getTom() {
        return getPeriode().getTom();
    }

    public OmsorgspengerUtfall getUtfall() {
        return utfall;
    }

    public void setUtfall(OmsorgspengerUtfall utfall) {
        this.utfall = utfall;
    }

    public Duration getLengde() {
        return lengde;
    }

    public void setLengde(Duration lengde) {
        this.lengde = lengde;
    }

    public UttakArbeidsforhold getUttakArbeidsforhold() {
        return uttakArbeidsforhold;
    }

    public LocalDateTime getInnsendingstidspunkt() {
        return innsendingstidspunkt;
    }

    public void setInnsendingstidspunkt(LocalDateTime innsendingstidspunkt) {
        this.innsendingstidspunkt = innsendingstidspunkt;
    }

    public void setUttakArbeidsforhold(UttakArbeidsforhold uttakArbeidsforhold) {
        this.uttakArbeidsforhold = uttakArbeidsforhold;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;

        var other = (UttaksperiodeOmsorgspenger) obj;
        return Objects.equals(periode, other.periode)
            && Objects.equals(utfall, other.utfall)
            && Objects.equals(utbetalingsgrad, other.utbetalingsgrad);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, utfall, utbetalingsgrad);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<periode=" + periode + ", utbetalingsgrad=" + utbetalingsgrad + ", utfall=" + utfall + ">";
    }

}
