package no.nav.k9.sak.web.app.tjenester.forvaltning;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_ABSENT, content = JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class MigrertSkjæringstidspunktDto {


    @JsonProperty(value = "skjæringstidspunkt", required = true)
    @NotNull
    @Valid
    private LocalDate skjæringstidspunkt;

    @JsonProperty(value = "erAktiv", required = true)
    @NotNull
    @Valid
    private boolean erAktiv;


    public MigrertSkjæringstidspunktDto() {
    }

    public MigrertSkjæringstidspunktDto(LocalDate skjæringstidspunkt, boolean erAktiv) {
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.erAktiv = erAktiv;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public boolean isErAktiv() {
        return erAktiv;
    }
}
