package no.nav.k9.sak.kontrakt.beregninginput;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.OrgNummer;


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class OverstyrBeregningAktivitet {

    @JsonProperty(value = "arbeidsgiverOrgnr")
    @Valid
    private OrgNummer arbeidsgiverOrgnr;

    @Valid
    @JsonProperty("arbeidsgiverAktørId")
    private AktørId arbeidsgiverAktørId;

    @JsonProperty(value = "inntektPrAar")
    @NotNull
    @Min(0)
    @Max(100000000)
    private Integer inntektPrAar;

    @JsonProperty(value = "refusjonPrAar")
    @Min(0)
    @Max(100000000)
    private Integer refusjonPrAar;

    @JsonProperty(value = "opphørRefusjon")
    private LocalDate opphørRefusjon;

    public OverstyrBeregningAktivitet() {
    }

    public OverstyrBeregningAktivitet(OrgNummer arbeidsgiverOrgnr,
                                      AktørId arbeidsgiverAktørId,
                                      Integer inntektPrAar, Integer
                                          refusjonPrAar, LocalDate opphørRefusjon) {
        this.arbeidsgiverOrgnr = arbeidsgiverOrgnr;
        this.arbeidsgiverAktørId = arbeidsgiverAktørId;
        this.inntektPrAar = inntektPrAar;
        this.refusjonPrAar = refusjonPrAar;
        this.opphørRefusjon = opphørRefusjon;
    }


    public OrgNummer getArbeidsgiverOrgnr() {
        return arbeidsgiverOrgnr;
    }

    public AktørId getArbeidsgiverAktørId() {
        return arbeidsgiverAktørId;
    }

    public Integer getInntektPrAar() {
        return inntektPrAar;
    }

    public Integer getRefusjonPrAar() {
        return refusjonPrAar;
    }

    public LocalDate getOpphørRefusjon() {
        return opphørRefusjon;
    }

    @Override
    public String toString() {
        return "OverstyrBeregningAktivitet{" +
            "arbeidsgiverOrgnr=" + arbeidsgiverOrgnr +
            ", arbeidsgiverAktørId=" + arbeidsgiverAktørId +
            ", inntektPrAar=" + inntektPrAar +
            ", refusjonPrAar=" + refusjonPrAar +
            '}';
    }

    @AssertTrue(message = "Enten orgnr eller aktørId må være satt")
    private boolean ok() {
        var orgnr = getArbeidsgiverOrgnr();
        var aktørId = getArbeidsgiverAktørId();
        return orgnr != null || aktørId != null;
    }


}
