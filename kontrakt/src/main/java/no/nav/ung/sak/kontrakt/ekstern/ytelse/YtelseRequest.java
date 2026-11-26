package no.nav.ung.sak.kontrakt.ekstern.ytelse;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.sak.typer.PersonIdent;

import java.time.LocalDate;
import java.util.regex.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YtelseRequest(
    @NotNull PersonIdent ident,
    @NotNull LocalDate fom,
    @NotNull LocalDate tom
) {

    private static final Pattern ELLEVE_SIFRE = Pattern.compile("\\d{11}");

    // Valideres her og ikke via @Valid på PersonIdent, fordi meldingen derfra gjentar ident.
    // getIdent() gir fnr eller d-nummer, ellers null. erNorskIdent() kaster ved tom ident.
    // PersonIdent sjekker ikke at alle tegnene er sifre, så det gjør vi her.
    @JsonIgnore
    @AssertTrue(message = "ident må være et gyldig fødselsnummer eller d-nummer")
    public boolean isGyldigIdent() {
        if (ident == null) {
            return true;
        }
        var fnrEllerDnr = ident.getIdent();
        return fnrEllerDnr != null && ELLEVE_SIFRE.matcher(fnrEllerDnr).matches();
    }

    @JsonIgnore
    @AssertTrue(message = "fom kan ikke være etter tom")
    public boolean isGyldigPeriode() {
        return fom == null || tom == null || !fom.isAfter(tom);
    }

    @Override
    public String toString() {
        return "YtelseRequest[ident=***, fom=" + fom + ", tom=" + tom + "]";
    }
}
