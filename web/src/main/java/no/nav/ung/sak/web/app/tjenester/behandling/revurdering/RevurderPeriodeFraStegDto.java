package no.nav.ung.sak.web.app.tjenester.behandling.revurdering;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.typer.Saksnummer;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_ABSENT, content = JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class RevurderPeriodeFraStegDto {
    @JsonProperty(value = "fom", required = true)
    @NotNull
    @Valid
    private LocalDate fom;

    @JsonProperty(value = "tom", required = true)
    @NotNull
    @Valid
    private LocalDate tom;

    @JsonProperty(value = "saksnummer", required = true)
    @NotNull
    @Valid
    private Saksnummer saksnummer;

    @JsonProperty(value = "steg", required = true)
    @NotNull
    @Valid
    private BehandlingÅrsakType steg;

    public RevurderPeriodeFraStegDto() {
    }

    public RevurderPeriodeFraStegDto(LocalDate fom, LocalDate tom, Saksnummer saksnummer, BehandlingÅrsakType steg) {
        this.fom = fom;
        this.tom = tom;
        this.saksnummer = saksnummer;
        this.steg = steg;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    @AbacAttributt("saksnummer")
    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public BehandlingÅrsakType getSteg() {
        return steg;
    }
}
