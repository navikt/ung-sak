package no.nav.k9.sak.web.app.tjenester.behandling.uttak;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class UttaksplanMedUtsattePerioder {

    @JsonProperty(value = "uttaksplan", required = true)
    @NotNull
    @Valid
    private Uttaksplan uttaksplan;

    @JsonProperty(value = "utsattePerioder")
    @Valid
    @Size
    private Set<LukketPeriode> utsattePerioder;

    public UttaksplanMedUtsattePerioder() {
    }

    public UttaksplanMedUtsattePerioder(Uttaksplan uttaksplan, Set<LukketPeriode> utsattePerioder) {
        this.uttaksplan = uttaksplan;
        this.utsattePerioder = utsattePerioder;
    }

    public Uttaksplan getUttaksplan() {
        return uttaksplan;
    }

    public Set<LukketPeriode> getUtsattePerioder() {
        return utsattePerioder;
    }
}
