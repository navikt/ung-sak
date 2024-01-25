package no.nav.k9.sak.web.app.tjenester.los;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.k9.sak.typer.AktørId;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
class LosOpplysningerSomManglerIKlageDto {

    @JsonProperty(value = "pleietrengendeAktørId")
    private AktørId pleietrengendeAktørId;

    @JsonProperty(value = "utenlandstilsnitt")
    private boolean utenlandstilsnitt;

    public void setUtenlandstilsnitt(boolean utenlandstilsnitt) {
        this.utenlandstilsnitt = utenlandstilsnitt;
    }

    public void setPleietrengendeAktørId(AktørId pleietrengendeAktørId) {
        this.pleietrengendeAktørId = pleietrengendeAktørId;
    }

}
