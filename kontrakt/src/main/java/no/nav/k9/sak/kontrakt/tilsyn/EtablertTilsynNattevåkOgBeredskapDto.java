package no.nav.k9.sak.kontrakt.tilsyn;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import java.time.LocalDateTime;
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

    @JsonProperty(value = "opprettetAv")
    @Size(max = 20)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String opprettetAv;

    @JsonProperty(value = "opprettetTidspunkt")
    @Valid
    private LocalDateTime opprettetTidspunkt;

    public EtablertTilsynNattevåkOgBeredskapDto(List<EtablertTilsynPeriodeDto> etablertTilsynPerioder, NattevåkDto nattevåk, BeredskapDto beredskap, String opprettetAv, LocalDateTime opprettetTidspunkt) {
        this.etablertTilsynPerioder = etablertTilsynPerioder;
        this.nattevåk = nattevåk;
        this.beredskap = beredskap;
        this.opprettetAv = opprettetAv;
        this.opprettetTidspunkt = opprettetTidspunkt;
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

    public String getOpprettetAv() {
        return opprettetAv;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }
}
