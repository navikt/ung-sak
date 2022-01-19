package no.nav.k9.sak.kontrakt.sykdom.dokument;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SykdomInnleggelseOppdateringResultatDto {

    
    @JsonProperty(value = "førerTilRevurdering")
    @Valid
    private boolean førerTilRevurdering = false;


    public SykdomInnleggelseOppdateringResultatDto() {

    }

    public SykdomInnleggelseOppdateringResultatDto(boolean førerTilRevurdering) {
        this.førerTilRevurdering = førerTilRevurdering;
    }

    public boolean isFørerTilRevurdering() {
        return førerTilRevurdering;
    }
}
