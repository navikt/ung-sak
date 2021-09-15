package no.nav.k9.sak.kontrakt.beregningsresultat;

import java.math.BigDecimal;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.OrgNummer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class TilkjentYtelseAndelDto {

    @JsonProperty(value = "inntektskategori", required = true)
    @NotNull
    @Valid
    private Inntektskategori inntektskategori;
    @JsonProperty(value = "arbeidsgiverAktørId")
    @Valid
    private AktørId aktørId;
    @JsonProperty(value = "arbeidsgiverOrgnr")
    @Valid
    private OrgNummer arbeidsgiverOrgnr;
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
        this.aktørId = builder.arbeidsgiverAktørId;
        this.beløpTilSøker = builder.beløpTilSøker;
        this.refusjonsbeløp = builder.refusjonsbeløp;
        this.utbetalingsgrad = builder.utbetalingsgrad;
    }

    public static Builder build() {
        return new Builder();
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public OrgNummer getArbeidsgiverOrgnr() {
        return arbeidsgiverOrgnr;
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
        private AktørId arbeidsgiverAktørId;
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

        public Builder medAktørId(AktørId arbeidsgiverAktørId) {
            this.arbeidsgiverAktørId = arbeidsgiverAktørId;
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
