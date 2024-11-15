package no.nav.ung.sak.kontrakt.beregninginput;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.OrgNummer;


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

    @AssertTrue(message = "Startdato for refusjon må være før opphørsdato")
    public boolean isStartdatoRefusjonFørOpphør() {
        if (startdatoRefusjon == null || opphørRefusjon == null) {
            return true;
        }
        return startdatoRefusjon.isBefore(opphørRefusjon);
    }

    @AssertTrue(message = "Enten inntekt eller refusjonsinformasjon må vere satt")
    public boolean isHarInformasjon() {
        if (startdatoRefusjon == null && opphørRefusjon == null && inntektPrAar == null) {
            return false;
        }
        return true;
    }


}
