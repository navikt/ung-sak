package no.nav.k9.sak.kontrakt.medlem;

import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
public abstract class BekreftedePerioderMalDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "bekreftedePerioder", required = true)
    @Valid
    @Size(max = 100)
    private List<BekreftedePerioderDto> bekreftedePerioder;

    public BekreftedePerioderMalDto() { // NOSONAR
        //
    }

    public BekreftedePerioderMalDto(String begrunnelse, List<BekreftedePerioderDto> bekreftedePerioder) {
        super(begrunnelse);
        this.bekreftedePerioder = List.copyOf(bekreftedePerioder);
    }

    public List<BekreftedePerioderDto> getBekreftedePerioder() {
        return Collections.unmodifiableList(bekreftedePerioder);
    }

    public void setBekreftedePerioder(List<BekreftedePerioderDto> bekreftedePerioder) {
        this.bekreftedePerioder = bekreftedePerioder;
    }
}
