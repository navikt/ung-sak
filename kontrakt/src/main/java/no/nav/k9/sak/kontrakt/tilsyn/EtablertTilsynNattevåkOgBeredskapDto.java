package no.nav.k9.sak.kontrakt.tilsyn;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class EtablertTilsynNattevåkOgBeredskapDto {

    @JsonProperty(value = "etablertTilsynPerioder")
    @Size(max = 1000)
    @Valid
    private List<EtablertTilsynPeriodeDto> etablertTilsynPerioder;

    @JsonProperty(value = "nattevåk")
    @Valid
    private NattevåkDto nattevåk;

    @JsonProperty(value = "beredskap")
    @Valid
    private BeredskapDto beredskap;

    public EtablertTilsynNattevåkOgBeredskapDto(List<EtablertTilsynPeriodeDto> etablertTilsynPerioder, NattevåkDto nattevåk, BeredskapDto beredskap) {
        this.etablertTilsynPerioder = etablertTilsynPerioder;
        this.nattevåk = nattevåk;
        this.beredskap = beredskap;
    }

    public List<EtablertTilsynPeriodeDto> getEtablertTilsynPerioder() {
        return etablertTilsynPerioder;
    }

    public NattevåkDto getNattevåk() {
        return nattevåk;
    }

    public BeredskapDto getBeredskap() {
        return beredskap;
    }
}
