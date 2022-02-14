package no.nav.k9.sak.web.app.tjenester.forvaltning;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_ABSENT, content = JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class MigrerFraInfotrygdDto {


    @JsonProperty(value = "skjæringstidspunkt", required = true)
    @NotNull
    @Valid
    private LocalDate skjæringstidspunkt;

    @JsonProperty(value = "saksnummer", required = true)
    @NotNull
    @Valid
    private SaksnummerDto saksnummer;


    public MigrerFraInfotrygdDto() {
    }

    public MigrerFraInfotrygdDto(LocalDate skjæringstidspunkt, SaksnummerDto saksnummer) {
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.saksnummer = saksnummer;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public SaksnummerDto getSaksnummer() {
        return saksnummer;
    }
}
