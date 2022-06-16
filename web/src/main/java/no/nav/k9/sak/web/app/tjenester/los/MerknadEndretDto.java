package no.nav.k9.sak.web.app.tjenester.los;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.sak.kontrakt.dokument.TekstValideringRegex;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record MerknadEndretDto(

    @JsonProperty(value = "behandlingUuid", required = true)
    @Valid
    UUID behandlingUuid,

    @JsonProperty(value = "merknadKoder")
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @Size(max = 10)
    @Valid
    List<String> merknadKoder,

    @JsonProperty(value = "fritekst")
    @Size(max = 50)
    @Pattern(regexp = TekstValideringRegex.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    String fritekst,

    @JsonProperty(value = "saksbehandlerIdent")
    @Size(max = 50)
    @Pattern(regexp = "^[\\p{Alnum}-]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    String saksbehandlerIdent
) {

    @AbacAttributt("behandlingUuid")
    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public MerknadEndretDto overstyrSaksbehandlerIdent(String saksbehandlerIdent) {
        return new MerknadEndretDto(
            behandlingUuid,
            merknadKoder,
            fritekst,
            saksbehandlerIdent
        );
    }
}
