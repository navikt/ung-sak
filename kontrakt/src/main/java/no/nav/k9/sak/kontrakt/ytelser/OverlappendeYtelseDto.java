package no.nav.k9.sak.kontrakt.ytelser;

import java.util.Collections;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.typer.Saksnummer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class OverlappendeYtelseDto {

    @NotNull
    @JsonProperty("ytelseType")
    private FagsakYtelseType ytelseType;

    @NotNull
    @JsonProperty("kilde")
    private Fagsystem kilde;

    @JsonProperty(value = "saksnummer")
    @Valid
    private Saksnummer saksnummer;

    @JsonProperty(value = "overlappendePerioder")
    @Size(max = 100)
    @Valid
    private List<OverlappendeYtelsePeriodeDto> overlappendePerioder = Collections.emptyList();

    public OverlappendeYtelseDto(FagsakYtelseType ytelseType, Fagsystem kilde, Saksnummer saksnummer, List<OverlappendeYtelsePeriodeDto> overlappendePerioder) {
        this.ytelseType = ytelseType;
        this.kilde = kilde;
        this.saksnummer = saksnummer;
        this.overlappendePerioder = overlappendePerioder;
    }
}
