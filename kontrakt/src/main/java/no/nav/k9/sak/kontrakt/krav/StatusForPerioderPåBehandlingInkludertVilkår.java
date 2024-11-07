package no.nav.k9.sak.kontrakt.krav;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class StatusForPerioderPåBehandlingInkludertVilkår {

    @Valid
    @NotNull
    @JsonProperty(value = "perioderMedÅrsak", required = true)
    private StatusForPerioderPåBehandling perioderMedÅrsak;

    @Valid
    @Size
    @JsonProperty("periodeMedUtfall")
    private List<PeriodeMedUtfall> periodeMedUtfall;

    @Valid
    @Size
    @JsonProperty("forrigeVedtak")
    private List<PeriodeMedUtfall> forrigeVedtak;

    @JsonCreator
    public StatusForPerioderPåBehandlingInkludertVilkår(@Valid @Size @JsonProperty("perioderMedÅrsak") StatusForPerioderPåBehandling perioderMedÅrsak,
                                                        @Valid @Size @JsonProperty("periodeMedUtfall") List<PeriodeMedUtfall> periodeMedUtfall,
                                                        @Valid @Size @JsonProperty("forrigeVedtak") List<PeriodeMedUtfall> forrigeVedtak) {
        this.perioderMedÅrsak = perioderMedÅrsak;
        this.periodeMedUtfall = periodeMedUtfall;
        this.forrigeVedtak = forrigeVedtak;
    }

    public StatusForPerioderPåBehandling getPerioderMedÅrsak() {
        return perioderMedÅrsak;
    }

    public List<PeriodeMedUtfall> getPeriodeMedUtfall() {
        return periodeMedUtfall;
    }

    public List<PeriodeMedUtfall> getForrigeVedtak() {
        return forrigeVedtak;
    }
}
