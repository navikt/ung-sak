package no.nav.ung.sak.kontrakt.klage;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class KlagebehandlingDto {

    private KlageVurderingResultatDto klageVurderingResultatNFP;
    private KlageVurderingResultatDto klageVurderingResultatNK;
    private KlageFormkravResultatDto klageFormkravResultatNFP;


    public KlagebehandlingDto() {
        // trengs for deserialisering av JSON
    }
    public KlageVurderingResultatDto getKlageVurderingResultatNFP() {
        return klageVurderingResultatNFP;
    }

    public KlageVurderingResultatDto getKlageVurderingResultatNK() {
        return klageVurderingResultatNK;
    }

    public KlageFormkravResultatDto getKlageFormkravResultatNFP() { return klageFormkravResultatNFP; }

    public void setKlageVurderingResultatNFP(KlageVurderingResultatDto klageVurderingResultatNFP) {
        this.klageVurderingResultatNFP = klageVurderingResultatNFP;
    }

    public void setKlageVurderingResultatNK(KlageVurderingResultatDto klageVurderingResultatNK) {
        this.klageVurderingResultatNK = klageVurderingResultatNK;
    }

    public void setKlageFormkravResultatNFP(KlageFormkravResultatDto klageFormkravResultatNFP) {
        this.klageFormkravResultatNFP = klageFormkravResultatNFP;
    }
}
