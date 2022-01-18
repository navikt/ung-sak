package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MigrerAksjonspunktRequest {

    @JsonProperty("aksjonspunktKode")
    @Pattern(regexp = "[\\d]{4}")
    @NotNull
    @Valid
    private String aksjonspunktKode;

    @JsonProperty("periode")
    @NotNull
    @Valid
    private Periode periode;


    protected MigrerAksjonspunktRequest() {
    }

    public MigrerAksjonspunktRequest(String aksjonspunktKode, Periode periode) {
        this.aksjonspunktKode = aksjonspunktKode;
        this.periode = periode;
    }

    public String getAksjonspunktKode() {
        return aksjonspunktKode;
    }

    public Periode getPeriode() {
        return periode;
    }

    public void setPeriode(Periode periode) {
        this.periode = periode;
    }
}
