package no.nav.ung.sak.kontrakt.ekstern.tilleggsstonader;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

/**
 * Forespørsel fra tilleggsstønader om innvilgede perioder med aktivitetspenger.
 *
 * @param ident fødselsnummer eller d-nummer. Sendes i body for å unngå fnr i url og tilgangslogger.
 * @param fom   fra og med dato for perioden det spørres om.
 * @param tom   til og med dato for perioden det spørres om.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AktivitetspengerPerioderRequest(
    @JsonProperty(value = "ident", required = true)
    @NotNull
    @Pattern(regexp = "^\\d{11}$", message = "ident må bestå av 11 siffer")
    String ident,

    @JsonProperty(value = "fom", required = true)
    @NotNull
    LocalDate fom,

    @JsonProperty(value = "tom", required = true)
    @NotNull
    LocalDate tom
) {

    @JsonIgnore
    @AssertTrue(message = "fom kan ikke være etter tom")
    public boolean isGyldigPeriode() {
        return fom == null || tom == null || !fom.isAfter(tom);
    }

    @Override
    public String toString() {
        return "AktivitetspengerPerioderRequest[ident=***, fom=" + fom + ", tom=" + tom + "]";
    }
}
