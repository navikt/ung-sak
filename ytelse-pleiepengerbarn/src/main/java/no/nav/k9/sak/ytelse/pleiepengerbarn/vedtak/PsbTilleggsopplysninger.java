package no.nav.k9.sak.ytelse.pleiepengerbarn.vedtak;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.domene.vedtak.observer.Tilleggsopplysning;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PsbTilleggsopplysninger implements Tilleggsopplysning {

    @Valid
    @JsonProperty(value = "pleietrengende")
    private AktørId pleietrengende;

    @Valid
    @Size
    @JsonProperty(value = "innleggelsesPerioder")
    private List<Periode> innleggelsesPerioder;

    public PsbTilleggsopplysninger() {
    }

    public PsbTilleggsopplysninger(AktørId pleietrengendeAktørId, List<Periode> innleggelsesperioder) {
        this.pleietrengende = pleietrengendeAktørId;
        this.innleggelsesPerioder = innleggelsesperioder;
    }

    public AktørId getPleietrengende() {
        return pleietrengende;
    }

    public void setPleietrengende(AktørId pleietrengende) {
        this.pleietrengende = pleietrengende;
    }

    public List<Periode> getInnleggelsesPerioder() {
        return innleggelsesPerioder;
    }

    public void setInnleggelsesPerioder(List<Periode> innleggelsesPerioder) {
        this.innleggelsesPerioder = innleggelsesPerioder;
    }
}
