package no.nav.k9.sak.kontrakt;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class KortTekst {
    @Size(max = 2000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String tekst;

    public KortTekst(@Size(max = 2000) @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]") String tekst) {
        this.tekst = tekst;
    }

    public String getTekst() {
        return tekst;
    }
}