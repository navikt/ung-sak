package no.nav.k9.sak.web.app.tjenester.ereg;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.abac.AbacAttributt;

import java.util.Objects;

/**
 * Organisasjonsnr for søk i enhetsregisteret (Brønnøysund)
 */
public class OrganisasjonsnrDto {
    public static final String NAME = "organisasjonsnr";

    @JsonProperty(value = NAME, required = true)
    @Size(max = 9)
    @NotNull
    @Pattern(regexp = "^\\d{9}$", message = "[${validatedValue}] er ugyldig. Må vere 9 siffer")
    private String orgnr;

    public OrganisasjonsnrDto(String orgnr) {
        this.orgnr = Objects.requireNonNull(orgnr, "orgnr");
    }

    protected OrganisasjonsnrDto() {
    }

    @NotNull
    @AbacAttributt(NAME)
    public String getOrgnr() {
        return orgnr;
    }

    @JsonSetter(NAME)
    public void setOrgnr(@NotNull String orgnr) {
        this.orgnr = orgnr;
    }

    @Override
    public String toString() {
        return orgnr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrganisasjonsnrDto that)) return false;
        return Objects.equals(orgnr, that.orgnr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orgnr);
    }
}
