package no.nav.k9.sak.kontrakt.beregninginput;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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

    @JsonProperty(value = "startdatoRefusjon")
    private LocalDate startdatoRefusjon;

    @JsonProperty(value = "opphørRefusjon")
    private LocalDate opphørRefusjon;

    @JsonProperty(value = "skalKunneEndreRefusjon")
    private Boolean skalKunneEndreRefusjon;

    public OverstyrBeregningAktivitet() {
    }

    public OverstyrBeregningAktivitet(OrgNummer arbeidsgiverOrgnr,
                                      AktørId arbeidsgiverAktørId,
                                      Integer inntektPrAar,
                                      Integer refusjonPrAar,
                                      LocalDate startdatoRefusjon,
                                      LocalDate opphørRefusjon,
                                      Boolean skalKunneEndreRefusjon) {
        this.arbeidsgiverOrgnr = arbeidsgiverOrgnr;
        this.arbeidsgiverAktørId = arbeidsgiverAktørId;
        this.inntektPrAar = inntektPrAar;
        this.refusjonPrAar = refusjonPrAar;
        this.startdatoRefusjon = startdatoRefusjon;
        this.opphørRefusjon = opphørRefusjon;
        this.skalKunneEndreRefusjon = skalKunneEndreRefusjon;
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


    public LocalDate getStartdatoRefusjon() {
        return startdatoRefusjon;
    }

    public LocalDate getOpphørRefusjon() {
        return opphørRefusjon;
    }

    public Boolean getSkalKunneEndreRefusjon() {
        return skalKunneEndreRefusjon;
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
    public boolean isOrgnrEllerAktørid() {
        var orgnr = getArbeidsgiverOrgnr();
        var aktørId = getArbeidsgiverAktørId();
        return orgnr != null || aktørId != null;
    }

    @AssertTrue(message = "Enten orgnr eller aktørId må være satt")
    public boolean isStartdatoRefusjonFørOpphør() {
        if (startdatoRefusjon == null || opphørRefusjon == null) {
            return true;
        }
        return startdatoRefusjon.isBefore(opphørRefusjon);
    }


}
