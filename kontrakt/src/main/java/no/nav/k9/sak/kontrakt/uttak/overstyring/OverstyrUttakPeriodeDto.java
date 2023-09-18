package no.nav.k9.sak.kontrakt.uttak.overstyring;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class OverstyrUttakPeriodeDto  {

    @JsonProperty(value = "id")
    @Min(value = 999951) //sekvens begynner på 1M og tar 50 om gangen
    @Max(value = Integer.MAX_VALUE)
    private Long id;

    @JsonProperty(value = "fom", required = true)
    @NotNull
    @Valid
    private Periode periode;

    @JsonProperty(value = "søkersUttakgsgrad")
    @DecimalMin("0")
    @DecimalMax("100")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal søkersUttakgsgrad;

    @JsonProperty(value = "utbetalingsgrader")
    @Valid
    @Size(min = 0, max = 100)
    private List<OverstyrUttakUtbetalingsgradDto> utbetalingsgrader;

    @JsonProperty("begrunnelse")
    @NotNull
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String begrunnelse;


    public OverstyrUttakPeriodeDto() {
        //
    }

    public OverstyrUttakPeriodeDto(Long id, Periode periode, BigDecimal søkersUttakgsgrad, List<OverstyrUttakUtbetalingsgradDto> utbetalingsgrader, String begrunnelse) {
        this.id = id;
        this.periode = periode;
        this.søkersUttakgsgrad = søkersUttakgsgrad;
        this.utbetalingsgrader = utbetalingsgrader;
        this.begrunnelse = begrunnelse;
    }

    public Long getId() {
        return id;
    }

    public Periode getPeriode() {
        return periode;
    }

    public BigDecimal getSøkersUttakgsgrad() {
        return søkersUttakgsgrad;
    }

    public List<OverstyrUttakUtbetalingsgradDto> getUtbetalingsgrader() {
        return utbetalingsgrader;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }
}
