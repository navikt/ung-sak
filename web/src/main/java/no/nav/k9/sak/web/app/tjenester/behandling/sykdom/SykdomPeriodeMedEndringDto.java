package no.nav.k9.sak.web.app.tjenester.behandling.sykdom;

import java.util.Objects;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomPeriodeMedEndring;



@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SykdomPeriodeMedEndringDto {

    @JsonProperty(value = "periode")
    @Valid
    private Periode periode;

    @JsonProperty(value = "endrerVurderingSammeBehandling")
    @Valid
    private boolean endrerVurderingSammeBehandling;

    @JsonProperty(value = "endrerAnnenVurdering")
    @Valid
    private boolean endrerAnnenVurdering;

    
    SykdomPeriodeMedEndringDto() {
        
    }

    public SykdomPeriodeMedEndringDto(Periode periode, boolean endrerVurderingSammeBehandling, boolean endrerAnnenVurdering) {
        this.periode = Objects.requireNonNull(periode, "periode");
        this.endrerVurderingSammeBehandling = endrerVurderingSammeBehandling;
        this.endrerAnnenVurdering = endrerAnnenVurdering;
    }
    
    public SykdomPeriodeMedEndringDto(SykdomPeriodeMedEndring p) {
        this(p.getPeriode(), p.isEndrerVurderingSammeBehandling(), p.isEndrerAnnenVurdering());
    }

    
    public Periode getPeriode() {
        return periode;
    }
    
    public boolean isEndrerAnnenVurdering() {
        return endrerAnnenVurdering;
    }
    
    public boolean isEndrerVurderingSammeBehandling() {
        return endrerVurderingSammeBehandling;
    }
}
