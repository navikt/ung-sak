package no.nav.k9.sak.kontrakt.beregningsresultat;

import java.math.BigDecimal;
import java.util.Objects;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.OrgNummer;
import no.nav.k9.sak.typer.PersonIdent;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class TilkjentYtelseAndelDto {

    @JsonProperty(value = "inntektskategori", required = true)
    @NotNull
    @Valid
    private Inntektskategori inntektskategori;
    @JsonProperty(value = "arbeidsgiverOrgnr")
    @Valid
    private OrgNummer arbeidsgiverOrgnr;
    @JsonProperty(value = "arbeidsgiverAktorId")
    @Valid
    private PersonIdent arbeidsgiverPersonIdent;
    @JsonProperty(value = "refusjon")
    @Min(0)
    @Max(1000000)
    private Integer refusjonsbeløp;
    @JsonProperty(value = "tilSoker")
    @Min(0)
    @Max(1000000)
    private Integer beløpTilSøker;
    @JsonProperty(value = "utbetalingsgrad")
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal utbetalingsgrad;

    public TilkjentYtelseAndelDto() {
        // Deserialisering av JSON
    }

    private TilkjentYtelseAndelDto(Builder builder) {
        this.inntektskategori = Objects.requireNonNull(builder.inntektskategori, "inntektskategori");
        this.arbeidsgiverOrgnr = builder.arbeidsgiverOrgnr;
        this.beløpTilSøker = builder.beløpTilSøker;
        this.refusjonsbeløp = builder.refusjonsbeløp;
        this.utbetalingsgrad = builder.utbetalingsgrad;
    }

    public static Builder build() {
        return new Builder();
    }

    public OrgNummer getArbeidsgiverOrgNr() {
        return arbeidsgiverOrgnr;
    }

    public PersonIdent getArbeidsgiverPersonIdent() {
        return arbeidsgiverPersonIdent;
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

    public Integer getRefusjonsbeløp() {
        return refusjonsbeløp;
    }

    public Integer getBeløpTilSøker() {
        return beløpTilSøker;
    }

    public BigDecimal getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    public static class Builder {
        private OrgNummer arbeidsgiverOrgnr;
        private Integer refusjonsbeløp;
        private Integer beløpTilSøker;
        private BigDecimal utbetalingsgrad;
        private Inntektskategori inntektskategori;

        private Builder() {
        }

        public TilkjentYtelseAndelDto create() {
            return new TilkjentYtelseAndelDto(this);
        }

        public Builder medInntektskategori(Inntektskategori inntektskategori) {
            this.inntektskategori = inntektskategori;
            return this;
        }

        public Builder medArbeidsgiverOrgNr(OrgNummer arbeidsgiverOrgnr) {
            this.arbeidsgiverOrgnr = arbeidsgiverOrgnr;
            return this;
        }

        public Builder medBeløpTilSøker(Integer beløpTilSøker) {
            this.beløpTilSøker = beløpTilSøker;
            return this;
        }

        public Builder medRefusjonsbeløp(Integer refusjonsbeløp) {
            this.refusjonsbeløp = refusjonsbeløp;
            return this;
        }

        public Builder medUtbetalingsgrad(BigDecimal utbetalingsgrad) {
            this.utbetalingsgrad = utbetalingsgrad;
            return this;
        }
    }
}
