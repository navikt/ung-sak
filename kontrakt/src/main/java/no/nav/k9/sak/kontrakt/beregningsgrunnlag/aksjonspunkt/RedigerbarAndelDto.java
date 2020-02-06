package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import java.time.LocalDate;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class RedigerbarAndelDto {

    @JsonProperty(value = "andelsnr", required = true)
    @NotNull
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long andelsnr;

    @JsonProperty(value = "arbeidsgiverId")
    @Pattern(regexp = "[\\d]{9}|[\\d]{13}")
    private String arbeidsgiverId;

    @JsonProperty(value = "arbeidsforholdId")
    @Pattern(regexp = "^[\\p{XDigit}_]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsforholdId;

    @JsonProperty(value = "nyAndel")
    private Boolean nyAndel;

    @JsonProperty(value = "aktivitetStatus", required = true)
    @NotNull
    @Valid
    private AktivitetStatus aktivitetStatus;

    @JsonProperty(value = "arbeidsforholdType", required = true)
    @NotNull
    @Valid
    private OpptjeningAktivitetType arbeidsforholdType;

    @JsonProperty(value = "lagtTilAvSaksbehandler")
    private Boolean lagtTilAvSaksbehandler;

    @JsonProperty(value = "beregningsperiodeFom")
    private LocalDate beregningsperiodeFom;

    @JsonProperty(value = "beregningsperiodeTom")
    private LocalDate beregningsperiodeTom;

    protected RedigerbarAndelDto() {
        //
    }

    public RedigerbarAndelDto(Boolean nyAndel,
                              String arbeidsgiverId, String internArbeidsforholdId,
                              Long andelsnr,
                              Boolean lagtTilAvSaksbehandler,
                              AktivitetStatus aktivitetStatus, OpptjeningAktivitetType arbeidsforholdType) {
        Objects.requireNonNull(aktivitetStatus, "aktivitetStatus");
        Objects.requireNonNull(arbeidsforholdType, "arbeidsforholdType");
        this.nyAndel = nyAndel;
        this.arbeidsgiverId = arbeidsgiverId;
        this.arbeidsforholdId = internArbeidsforholdId;
        this.andelsnr = andelsnr;
        this.lagtTilAvSaksbehandler = lagtTilAvSaksbehandler;
        this.aktivitetStatus = aktivitetStatus;
        this.arbeidsforholdType = arbeidsforholdType;
    }

    public RedigerbarAndelDto(Boolean nyAndel,
                              String arbeidsgiverId, InternArbeidsforholdRef arbeidsforholdId,
                              Long andelsnr,
                              Boolean lagtTilAvSaksbehandler,
                              AktivitetStatus aktivitetStatus, OpptjeningAktivitetType arbeidsforholdType) {
        Objects.requireNonNull(aktivitetStatus, "aktivitetStatus");
        Objects.requireNonNull(arbeidsforholdType, "arbeidsforholdType");
        this.nyAndel = nyAndel;
        this.arbeidsgiverId = arbeidsgiverId;
        this.arbeidsforholdId = arbeidsforholdId == null ? null : arbeidsforholdId.getReferanse();
        this.andelsnr = andelsnr;
        this.lagtTilAvSaksbehandler = lagtTilAvSaksbehandler;
        this.aktivitetStatus = aktivitetStatus;
        this.arbeidsforholdType = arbeidsforholdType;
    }

    public RedigerbarAndelDto(Boolean nyAndel,
                              Long andelsnr,
                              Boolean lagtTilAvSaksbehandler,
                              AktivitetStatus aktivitetStatus, OpptjeningAktivitetType arbeidsforholdType) {
        this(nyAndel, null, (InternArbeidsforholdRef) null, andelsnr, lagtTilAvSaksbehandler, aktivitetStatus, arbeidsforholdType);
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public Long getAndelsnr() {
        return andelsnr;
    }

    public InternArbeidsforholdRef getArbeidsforholdId() {
        return InternArbeidsforholdRef.ref(arbeidsforholdId);
    }

    public String getArbeidsgiverId() {
        return arbeidsgiverId;
    }

    public OpptjeningAktivitetType getArbeidsforholdType() {
        return arbeidsforholdType;
    }

    public Boolean getNyAndel() {
        return nyAndel;
    }

    public Boolean getLagtTilAvSaksbehandler() {
        return lagtTilAvSaksbehandler;
    }

    public LocalDate getBeregningsperiodeFom() {
        return beregningsperiodeFom;
    }

    public LocalDate getBeregningsperiodeTom() {
        return beregningsperiodeTom;
    }
}
