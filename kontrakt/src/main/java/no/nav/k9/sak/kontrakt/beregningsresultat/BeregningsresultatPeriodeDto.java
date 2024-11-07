package no.nav.k9.sak.kontrakt.beregningsresultat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
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
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BeregningsresultatPeriodeDto {

    public static class Builder {
        private List<BeregningsresultatPeriodeAndelDto> andeler;
        private int dagsats;
        private LocalDate fom;
        private LocalDate tom;
        private BigDecimal totalUtbetalingsgradFraUttak;
        private BigDecimal totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt;
        private BigDecimal reduksjonsfaktorInaktivTypeA;

        private Builder() {
            this.andeler = new ArrayList<>();
        }

        public BeregningsresultatPeriodeDto create() {
            return new BeregningsresultatPeriodeDto(this);
        }

        public Builder medAndeler(List<BeregningsresultatPeriodeAndelDto> andeler) {
            this.andeler.addAll(andeler);
            return this;
        }

        public Builder medDagsats(int dagsats) {
            this.dagsats = dagsats;
            return this;
        }

        public Builder medTotalUtbetalingsgradFraUttak(BigDecimal totalUtbetalingsgradFraUttak) {
            this.totalUtbetalingsgradFraUttak = totalUtbetalingsgradFraUttak;
            return this;
        }

        public Builder medTotalUtbetalingsgradEtterReduksjonVedTilkommetInntekt(BigDecimal totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt) {
            this.totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt = totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt;
            return this;
        }

        public Builder medReduksjonsfaktorInaktivTypeA(BigDecimal reduksjonsfaktorInaktivTypeA) {
            this.reduksjonsfaktorInaktivTypeA = reduksjonsfaktorInaktivTypeA;
            return this;
        }


        public Builder medFom(LocalDate fom) {
            this.fom = fom;
            return this;
        }

        public Builder medTom(LocalDate tom) {
            this.tom = tom;
            return this;
        }
    }

    @JsonProperty(value = "andeler", required = true)
    @Valid
    @Size(max = 200)
    private List<BeregningsresultatPeriodeAndelDto> andeler;

    @JsonProperty(value = "dagsats", required = true)
    @Min(0)
    @Max(100000)
    private int dagsats;

    @JsonProperty(value = "totalUtbetalingsgradFraUttak")
    @DecimalMin("0")
    @DecimalMax("1")
    @Digits(integer = 1, fraction = 4)
    @Valid
    private BigDecimal totalUtbetalingsgradFraUttak;

    @JsonProperty(value = "totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt")
    @DecimalMin("0")
    @DecimalMax("1")
    @Digits(integer = 1, fraction = 4)
    @Valid
    private BigDecimal totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt;

    @JsonProperty(value = "reduksjonsfaktorInaktivTypeA")
    @Valid
    @DecimalMin(value = "0.00", message = "verdien ${validatedValue} må være >= {value}")
    @DecimalMax(value = "1.00", message = "verdien ${validatedValue} må være <= {value}")
    @Digits(integer = 10, fraction = 4)
    private BigDecimal reduksjonsfaktorInaktivTypeA;

    @JsonProperty(value = "fom", required = true)
    @Valid
    private LocalDate fom;

    @JsonProperty(value = "tom", required = true)
    @Valid
    private LocalDate tom;

    private BeregningsresultatPeriodeDto(Builder builder) {
        fom = builder.fom;
        tom = builder.tom;
        dagsats = builder.dagsats;
        totalUtbetalingsgradFraUttak = builder.totalUtbetalingsgradFraUttak;
        totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt = builder.totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt;
        reduksjonsfaktorInaktivTypeA = builder.reduksjonsfaktorInaktivTypeA;
        andeler = List.copyOf(builder.andeler);
    }

    public BeregningsresultatPeriodeDto() {
        // Deserialisering av JSON
    }

    public static Builder build() {
        return new Builder();
    }

    public static Builder build(LocalDate fom, LocalDate tom) {
        return new Builder().medFom(fom).medTom(tom);
    }

    public List<BeregningsresultatPeriodeAndelDto> getAndeler() {
        return Collections.unmodifiableList(andeler);
    }

    public int getDagsats() {
        return dagsats;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }


    public BigDecimal getTotalUtbetalingsgradFraUttak() {
        return totalUtbetalingsgradFraUttak;
    }

    public BigDecimal getTotalUtbetalingsgradEtterReduksjonVedTilkommetInntekt() {
        return totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt;
    }

    public BigDecimal getReduksjonsfaktorInaktivTypeA() {
        return reduksjonsfaktorInaktivTypeA;
    }

    public void setAndeler(List<BeregningsresultatPeriodeAndelDto> andeler) {
        this.andeler = List.copyOf(andeler);
    }

    public void setDagsats(int dagsats) {
        this.dagsats = dagsats;
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    public void setTom(LocalDate tom) {
        this.tom = tom;
    }
}
