package no.nav.k9.sak.kontrakt.uttak.overstyring;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
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
public class OverstyrUttakPeriodeDto {

    @JsonProperty(value = "id")
    @Min(value = 999951) //sekvens begynner på 1M og tar 50 om gangen
    @Max(value = Integer.MAX_VALUE)
    private Long id;

    @JsonProperty(value = "periode", required = true)
    @NotNull
    @Valid
    private Periode periode;

    @JsonProperty(value = "søkersUttaksgrad")
    @DecimalMin("0")
    @DecimalMax("100")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal søkersUttaksgrad;

    @JsonProperty(value = "utbetalingsgrader")
    @Valid
    @Size(min = 0, max = 100)
    private List<OverstyrUttakUtbetalingsgradDto> utbetalingsgrader;

    @JsonProperty("begrunnelse")
    @NotNull
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String begrunnelse;

    @JsonProperty("saksbehandler")
    @Pattern(regexp = "^[\\p{Graph}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Size(max = 100)
    private String saksbehandler;

    public OverstyrUttakPeriodeDto() {
        //
    }

    public OverstyrUttakPeriodeDto(Long id, Periode periode, BigDecimal søkersUttaksgrad, List<OverstyrUttakUtbetalingsgradDto> utbetalingsgrader, String begrunnelse, String saksbehandler) {
        this.id = id;
        this.periode = periode;
        this.søkersUttaksgrad = søkersUttaksgrad;
        this.utbetalingsgrader = utbetalingsgrader;
        this.begrunnelse = begrunnelse;
        this.saksbehandler = saksbehandler;
    }

    public Long getId() {
        return id;
    }

    public Periode getPeriode() {
        return periode;
    }

    public BigDecimal getSøkersUttaksgrad() {
        return søkersUttaksgrad;
    }

    public List<OverstyrUttakUtbetalingsgradDto> getUtbetalingsgrader() {
        return utbetalingsgrader;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public String getSaksbehandler() {
        return saksbehandler;
    }

    @AssertTrue(message = "Perioder skal være lukket (ha start og slutt)")
    public boolean getPerioderErLukket() {
        return periode.getFom() != null && periode.getTom() != null;
    }
}