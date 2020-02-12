package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class AvklarAktiviteterDto {

    @JsonProperty(value = "aktiviteterTomDatoMapping")
    @NotNull
    @Valid
    @Size(max = 200)
    private List<AktivitetTomDatoMappingDto> aktiviteterTomDatoMapping = Collections.emptyList();

    public List<AktivitetTomDatoMappingDto> getAktiviteterTomDatoMapping() {
        return Collections.unmodifiableList(aktiviteterTomDatoMapping);
    }

    public void setAktiviteterTomDatoMapping(List<AktivitetTomDatoMappingDto> aktiviteterTomDatoMapping) {
        this.aktiviteterTomDatoMapping = List.copyOf(aktiviteterTomDatoMapping);
    }
}
