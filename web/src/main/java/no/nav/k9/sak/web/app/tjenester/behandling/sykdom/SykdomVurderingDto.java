package no.nav.k9.sak.web.app.tjenester.behandling.sykdom;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SykdomVurderingDto {

    /**
     * IDen til SykdomVurdering (og ikke en gitt SykdomVurderingVersjon).
     */
    @JsonProperty(value = "id")
    @Size(max = 50)
    @NotNull
    @Pattern(regexp = "^[\\p{Alnum}-]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String id;
        
    @JsonProperty(value = "type")
    @Valid
    private SykdomVurderingType type;

    /**
     * Index 0 har gjeldende versjon.
     */
    @JsonProperty(value = "versjoner")
    @Size(max = 100)
    @Valid
    private List<SykdomVurderingVersjonDto> versjoner  = new ArrayList<>();

    @JsonProperty(value = "annenInformasjon")
    @Valid
    private SykdomVurderingAnnenInformasjon annenInformasjon;
    

    public SykdomVurderingDto(String id,
            SykdomVurderingType type, 
            List<SykdomVurderingVersjonDto> versjoner,
            SykdomVurderingAnnenInformasjon annenInformasjon) {
        super();
        this.id = id;
        this.type = type;
        this.versjoner = versjoner;
        this.annenInformasjon = annenInformasjon;
    }

    
    public String getId() {
        return id;
    }
    
    public SykdomVurderingType getType() {
        return type;
    }
    
    public List<SykdomVurderingVersjonDto> getVersjoner() {
        return versjoner;
    }

    public SykdomVurderingAnnenInformasjon getAnnenInformasjon() {
        return annenInformasjon;
    }

}