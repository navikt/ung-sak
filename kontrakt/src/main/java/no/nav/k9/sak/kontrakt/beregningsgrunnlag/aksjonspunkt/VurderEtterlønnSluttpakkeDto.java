package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class VurderEtterlønnSluttpakkeDto {

    @JsonProperty(value = "erEtterlønnSluttpakke", required = true)
    @NotNull
    private Boolean erEtterlønnSluttpakke;

    public VurderEtterlønnSluttpakkeDto(Boolean erEtterlønnSluttpakke) { // NOSONAR
        this.erEtterlønnSluttpakke = erEtterlønnSluttpakke;
    }

    protected VurderEtterlønnSluttpakkeDto() {
        //
    }

    public Boolean erEtterlønnSluttpakke() {
        return erEtterlønnSluttpakke;
    }

    public void setErEtterlønnSluttpakke(Boolean erEtterlønnSluttpakke) {
        this.erEtterlønnSluttpakke = erEtterlønnSluttpakke;
    }
}
