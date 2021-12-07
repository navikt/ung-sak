package no.nav.k9.sak.kontrakt.beregninginput;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.sak.kontrakt.uttak.Periode;
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

    public OverstyrBeregningAktivitet() {
    }

    public OverstyrBeregningAktivitet(OrgNummer arbeidsgiverOrgnr,
                                      AktørId arbeidsgiverAktørId,
                                      Integer inntektPrAar, Integer
                                          refusjonPrAar) {
        this.arbeidsgiverOrgnr = arbeidsgiverOrgnr;
        this.arbeidsgiverAktørId = arbeidsgiverAktørId;
        this.inntektPrAar = inntektPrAar;
        this.refusjonPrAar = refusjonPrAar;
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
