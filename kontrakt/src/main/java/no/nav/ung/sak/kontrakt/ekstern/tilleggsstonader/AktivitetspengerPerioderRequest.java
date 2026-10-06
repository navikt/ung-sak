package no.nav.ung.sak.kontrakt.ekstern.tilleggsstonader;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.sak.typer.PersonIdent;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AktivitetspengerPerioderRequest(
    @NotNull PersonIdent ident,
    @NotNull LocalDate fom,
    @NotNull LocalDate tom
) {

    // Valideres her og ikke via @Valid på PersonIdent, fordi meldingen derfra gjentar ident
    @JsonIgnore
    @AssertTrue(message = "ident må være et gyldig fødselsnummer eller d-nummer")
    public boolean isGyldigIdent() {
        return ident == null || ident.erNorskIdent();
    }

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
