package no.nav.k9.sak.web.app.tjenester.behandling.uttak;

import java.time.LocalDate;
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

    @JsonProperty(value = "uttaksplan", required = false)
    @NotNull
    @Valid
    private Uttaksplan uttaksplan;

    @JsonProperty(value = "simulertUttaksplan", required = false)
    @NotNull
    @Valid
    private Uttaksplan simulertUttaksplan;
    @JsonProperty(value = "utsattePerioder")
    @Valid
    @Size
    private Set<LukketPeriode> utsattePerioder;
    @JsonProperty(value = "perioderTilVurdering")
    @Valid
    @Size
    private Set<LukketPeriode> perioderTilVurdering;
    @JsonProperty(value = "virkningsdatoUttakNyeRegler")
    @Valid
    private LocalDate virkningsdatoUttakNyeRegler;

    public UttaksplanMedUtsattePerioder() {
    }

    public UttaksplanMedUtsattePerioder(Uttaksplan uttaksplan, Uttaksplan simulertUttaksplan, Set<LukketPeriode> utsattePerioder, LocalDate virkningsdatoUttakNyeRegler, Set<LukketPeriode> perioderTilVurdering) {
        this.uttaksplan = uttaksplan;
        this.simulertUttaksplan = simulertUttaksplan;
        this.utsattePerioder = utsattePerioder;
        this.virkningsdatoUttakNyeRegler = virkningsdatoUttakNyeRegler;
        this.perioderTilVurdering = perioderTilVurdering;
    }

    public static UttaksplanMedUtsattePerioder medUttaksplan(Uttaksplan uttaksplan, Set<LukketPeriode> utsattePerioder, LocalDate virkningsdatoUttakNyeRegler, Set<LukketPeriode> perioderTilVurdering) {
        return new UttaksplanMedUtsattePerioder(uttaksplan, null, utsattePerioder, virkningsdatoUttakNyeRegler, perioderTilVurdering);
    }

    public static UttaksplanMedUtsattePerioder medSimulertUttaksplan(Uttaksplan uttaksplan, Set<LukketPeriode> utsattePerioder, LocalDate virkningsdatoUttakNyeRegler, Set<LukketPeriode> perioderTilVurdering) {
        return new UttaksplanMedUtsattePerioder(null, uttaksplan, utsattePerioder, virkningsdatoUttakNyeRegler, perioderTilVurdering);
    }

    public Uttaksplan getUttaksplan() {
        return uttaksplan;
    }

    public Uttaksplan getSimulertUttaksplan() {
        return simulertUttaksplan;
    }

    public Set<LukketPeriode> getUtsattePerioder() {
        return utsattePerioder;
    }

    public LocalDate getVirkningsdatoUttakNyeRegler() {
        return virkningsdatoUttakNyeRegler;
    }

    public Set<LukketPeriode> getPerioderTilVurdering() {
        return perioderTilVurdering;
    }
}
