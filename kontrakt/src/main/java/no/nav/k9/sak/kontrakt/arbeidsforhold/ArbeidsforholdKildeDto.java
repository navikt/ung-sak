package no.nav.k9.sak.kontrakt.arbeidsforhold;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdKilde;

/**
 * @deprecated Unødvendig klasse - bruk heller {@link ArbeidsforholdKilde}
 */
@Deprecated
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
class ArbeidsforholdKildeDto {

    @JsonProperty(value = "navn", required = true)
    @NotNull
    private String navn;

    @JsonCreator
    public ArbeidsforholdKildeDto(@JsonProperty(value = "navn") @NotNull String navn) {
        this.navn = navn;
    }

    public String getNavn() {

        return navn;
    }
}
