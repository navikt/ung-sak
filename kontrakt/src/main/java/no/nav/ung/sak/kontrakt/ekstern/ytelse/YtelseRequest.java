package no.nav.ung.sak.kontrakt.ekstern.ytelse;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.ung.kodeverk.abac.StandardAbacAttributt;
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

    // EKSTERN_SYSTEM vurderes bare på token og bruker ikke attributtet. Det gir likevel personsjekk
    // dersom requesten gjenbrukes i et endepunkt for innloggede brukere.
    @JsonIgnore
    @StandardAbacAttributt(StandardAbacAttributtType.FNR)
    public String getFnr() {
        return ident == null ? null : ident.getIdent();
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
