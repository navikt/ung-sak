package no.nav.ung.sak.kontrakt.klage;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class KlageVurderingResultatDto {
    private String klageVurdering;
    private String begrunnelse;
    private String fritekstTilBrev;
    private String hjemmel;
    private String klageMedholdArsak;
    private String klageMedholdArsakNavn;
    private String klageVurderingOmgjoer;
    private String klageVurdertAv;
    private boolean godkjentAvMedunderskriver;

    public KlageVurderingResultatDto() {
    }

    public String getKlageVurdering() {
        return klageVurdering;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public String getFritekstTilBrev() {
        return fritekstTilBrev;
    }

    public String getHjemmel() {
        return hjemmel;
    }

    public String getKlageMedholdArsak() {
        return klageMedholdArsak;
    }

    public String getKlageVurdertAv() {
        return klageVurdertAv;
    }

    public String getKlageMedholdArsakNavn() {
        return klageMedholdArsakNavn;
    }

    public String getKlageVurderingOmgjoer() {
        return klageVurderingOmgjoer;
    }

    public boolean isGodkjentAvMedunderskriver() {
        return godkjentAvMedunderskriver;
    }

    public void setKlageVurderingOmgjoer(String klageVurderingOmgjoer) {
        this.klageVurderingOmgjoer = klageVurderingOmgjoer;
    }

    public void setKlageVurdering(String klageVurdering) {
        this.klageVurdering = klageVurdering;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public void setFritekstTilBrev(String fritekstTilBrev) {
        this.fritekstTilBrev = fritekstTilBrev;
    }

    public void setHjemmel(String hjemmel) {
        this.hjemmel = hjemmel;
    }

    public void setKlageMedholdArsak(String klageMedholdArsak) {
        this.klageMedholdArsak = klageMedholdArsak;
    }

    public void setKlageMedholdArsakNavn(String klageMedholdArsakNavn) {
        this.klageMedholdArsakNavn = klageMedholdArsakNavn;
    }

    public void setKlageVurdertAv(String klageVurdertAv) {
        this.klageVurdertAv = klageVurdertAv;
    }

    public void setGodkjentAvMedunderskriver(boolean godkjentAvMedunderskriver) {
        this.godkjentAvMedunderskriver = godkjentAvMedunderskriver;
    }
}
