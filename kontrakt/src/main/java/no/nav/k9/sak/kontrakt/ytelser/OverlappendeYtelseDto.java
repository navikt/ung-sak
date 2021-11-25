package no.nav.k9.sak.kontrakt.ytelser;

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

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class OverlappendeYtelseDto {

    @NotNull
    @JsonProperty("ytelseType")
    private FagsakYtelseType ytelseType;

    @JsonProperty(value = "overlappendePerioder")
    @Size(max = 100)
    @Valid
    private List<OverlappendeYtelsePeriodeDto> overlappendePerioder = Collections.emptyList();

    public OverlappendeYtelseDto(FagsakYtelseType ytelseType, List<OverlappendeYtelsePeriodeDto> overlappendePerioder) {
        this.ytelseType = ytelseType;
        this.overlappendePerioder = overlappendePerioder;
    }
}
