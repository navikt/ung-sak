package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Bostedavklaring for ett skjæringstidspunkt, med foreslått og eventuell fastsatt verdi.
 * {@code fom} er skjæringstidspunktet (fom-datoen i den tilhørende vilkårsperioden).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BostedGrunnlagPeriodeDto {

    /** Skjæringstidspunkt (fom-dato i vilkårsperioden). */
    @JsonProperty("fom")
    @NotNull
    private LocalDate fom;

    /** Saksbehandlers foreslåtte vurdering fra VURDER_BOSTED-steget. */
    @JsonProperty("foreslåttErBosattITrondheim")
    @NotNull
    private Boolean foreslåttErBosattITrondheim;

    /** Fastsatt vurdering etter FASTSETT_BOSTED. Null dersom ikke fastset ennå. */
    @JsonProperty("fastsattErBosattITrondheim")
    private Boolean fastsattErBosattITrondheim;

    /** Hva bruker oppga i søknaden. Null dersom ikke oppgitt. */
    @JsonProperty("søknadOppgittErBosattITrondheim")
    private Boolean søknadOppgittErBosattITrondheim;

    public BostedGrunnlagPeriodeDto() {
        // for jackson
    }

    public BostedGrunnlagPeriodeDto(LocalDate fom, Boolean foreslåttErBosattITrondheim, Boolean fastsattErBosattITrondheim) {
        this.fom = fom;
        this.foreslåttErBosattITrondheim = foreslåttErBosattITrondheim;
        this.fastsattErBosattITrondheim = fastsattErBosattITrondheim;
    }

    public BostedGrunnlagPeriodeDto(LocalDate fom, Boolean foreslåttErBosattITrondheim, Boolean fastsattErBosattITrondheim, Boolean søknadOppgittErBosattITrondheim) {
        this(fom, foreslåttErBosattITrondheim, fastsattErBosattITrondheim);
        this.søknadOppgittErBosattITrondheim = søknadOppgittErBosattITrondheim;
    }

    public LocalDate getFom() {
        return fom;
    }

    public Boolean getForeslåttErBosattITrondheim() {
        return foreslåttErBosattITrondheim;
    }

    public Boolean getFastsattErBosattITrondheim() {
        return fastsattErBosattITrondheim;
    }

    public Boolean getSøknadOppgittErBosattITrondheim() {
        return søknadOppgittErBosattITrondheim;
    }
}
