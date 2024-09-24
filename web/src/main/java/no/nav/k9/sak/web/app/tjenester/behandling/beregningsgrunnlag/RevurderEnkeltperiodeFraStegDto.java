package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.abac.AbacAttributt;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_ABSENT, content = JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class RevurderEnkeltperiodeFraStegDto {
    @JsonProperty(value = "fom", required = true)
    @NotNull
    @Valid
    private LocalDate fom;

    @JsonProperty(value = "tom", required = true)
    @NotNull
    @Valid
    private LocalDate tom;

    @JsonProperty(value = "fagsakId", required = true)
    @NotNull
    private Long fagsakId;

    @JsonProperty(value = "steg", required = true)
    @NotNull
    private ManuellRevurderingSteg steg;

    public RevurderEnkeltperiodeFraStegDto() {
    }

    public RevurderEnkeltperiodeFraStegDto(LocalDate fom, LocalDate tom, Long fagsakId, ManuellRevurderingSteg steg) {
        this.fom = fom;
        this.tom = tom;
        this.fagsakId = fagsakId;
        this.steg = steg;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    @AbacAttributt("fagsakId")
    public Long getFagsakId() {
        return fagsakId;
    }

    public ManuellRevurderingSteg getSteg() {
        return steg;
    }
}
