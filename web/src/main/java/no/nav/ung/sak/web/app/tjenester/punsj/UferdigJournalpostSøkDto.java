package no.nav.ung.sak.web.app.tjenester.punsj;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.ung.sak.kontrakt.person.AktørIdDto;

@JsonInclude(value = Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE)
public class UferdigJournalpostSøkDto {
    @JsonProperty(value = "aktorIdDto", required = true)
    @Valid
    @NotNull
    private AktørIdDto aktorIdDto;

    @JsonProperty(value = "aktorIdDtoBarn")
    @Valid
    private AktørIdDto aktorIdDtoBarn;

    public UferdigJournalpostSøkDto() {
    }

    public UferdigJournalpostSøkDto(AktørIdDto aktorIdDto, AktørIdDto aktorIdDtoBarn) {
        this.aktorIdDto = aktorIdDto;
        this.aktorIdDtoBarn = aktorIdDtoBarn;
    }

    public AktørIdDto getAktorIdDto() {
        return aktorIdDto;
    }

    public void setAktorIdDto(AktørIdDto aktorIdDto) {
        this.aktorIdDto = aktorIdDto;
    }

    public AktørIdDto getAktorIdDtoBarn() {
        return aktorIdDtoBarn;
    }

    public void setAktorIdDtoBarn(AktørIdDto aktorIdDtoBarn) {
        this.aktorIdDtoBarn = aktorIdDtoBarn;
    }
}
