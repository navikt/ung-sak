package no.nav.k9.sak.web.app.tjenester.brev;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BrevMottakerinfoEregResponseDto {

    public BrevMottakerinfoEregResponseDto(@NotNull String navn) {
        this.navn = navn;
    }

    @JsonProperty("navn")
    @NotNull
    private final String navn;

    @NotNull
    public String getNavn() {
        return navn;
    }
}
