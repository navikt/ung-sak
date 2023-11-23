package no.nav.k9.sak.web.app.tjenester.ereg;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class EregOrganisasjonBrevmottakerInfoResponseDto {

    public EregOrganisasjonBrevmottakerInfoResponseDto(@Nullable String navn) {
        this.navn = navn;
    }

    @JsonProperty("navn")
    @Nullable
    private String navn;

    @Nullable
    public String getNavn() {
        return navn;
    }
}
