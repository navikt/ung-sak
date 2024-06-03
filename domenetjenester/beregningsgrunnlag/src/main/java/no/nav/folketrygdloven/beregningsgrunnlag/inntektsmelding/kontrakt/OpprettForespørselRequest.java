package no.nav.folketrygdloven.beregningsgrunnlag.inntektsmelding.kontrakt;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_ABSENT, content = JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class OpprettForespørselRequest {

    @JsonProperty(value = "aktørId", required = true)
    @Pattern(regexp = "^\\d+$", message = "AktørId [${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    @NotNull
    private String aktørId;

    @JsonProperty(value = "orgnummer", required = true)
    @Pattern(regexp = "^\\d+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    @NotNull
    private ForespørselOrganisasjonsnummerDto orgnummer;

    @JsonProperty(value = "skjæringstidspunkt", required = true)
    @Valid
    @NotNull
    private LocalDate skjæringstidspunkt;

    @JsonProperty(value = "ytelsetype", required = true)
    @Valid
    @NotNull
    private InntektsmeldingYtelseType ytelsetype;

    @JsonProperty(value = "saksnummer", required = true)
    @Valid
    @NotNull
    @Pattern(regexp = "^[a-zA-Z0-9]*$")
    private ForespørselSaksnummerDto saksnummer;


    public OpprettForespørselRequest() {
    }

    public OpprettForespørselRequest(String aktørId,
                                     ForespørselOrganisasjonsnummerDto orgnummer,
                                     LocalDate skjæringstidspunkt,
                                     InntektsmeldingYtelseType ytelsetype,
                                     ForespørselSaksnummerDto saksnummer) {
        this.aktørId = aktørId;
        this.orgnummer = orgnummer;
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.ytelsetype = ytelsetype;
        this.saksnummer = saksnummer;
    }

    public String getAktørId() {
        return aktørId;
    }

    public ForespørselOrganisasjonsnummerDto getOrgnummer() {
        return orgnummer;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public InntektsmeldingYtelseType getYtelsetype() {
        return ytelsetype;
    }

    public ForespørselSaksnummerDto getSaksnummer() {
        return saksnummer;
    }


}
