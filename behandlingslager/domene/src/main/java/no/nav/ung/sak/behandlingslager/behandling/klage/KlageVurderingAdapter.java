package no.nav.ung.sak.behandlingslager.behandling.klage;

import no.nav.ung.kodeverk.hjemmel.Hjemmel;
import no.nav.ung.kodeverk.klage.KlageMedholdÅrsak;
import no.nav.ung.kodeverk.klage.KlageVurderingType;
import no.nav.ung.kodeverk.klage.KlageVurderingOmgjør;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;

public class KlageVurderingAdapter {
    private KlageVurderingType klageVurderingType;
    private String begrunnelse;
    private String fritekstTilBrev;
    private Hjemmel hjemmel;
    private KlageMedholdÅrsak klageMedholdArsakKode;
    private KlageVurderingOmgjør klageVurderingOmgjoer;
    private KlageVurdertAv klageVurdertAv;

    public KlageVurderingAdapter(KlageVurderingType klageVurderingType,
                                 KlageMedholdÅrsak klageMedholdArsakKode,
                                 KlageVurderingOmgjør klageVurderingOmgjør,
                                 String begrunnelse,
                                 String fritekstTilBrev,
                                 Hjemmel hjemmel,
                                 KlageVurdertAv klageVurdertAv) {
        this.klageVurderingType = klageVurderingType;
        this.begrunnelse = begrunnelse;
        this.fritekstTilBrev = fritekstTilBrev;
        this.hjemmel = hjemmel;
        this.klageMedholdArsakKode = klageMedholdArsakKode;
        this.klageVurderingOmgjoer = klageVurderingOmgjør;
        this.klageVurdertAv = klageVurdertAv;
    }

    public KlageVurderingType getKlageVurdering() {
        return klageVurderingType;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public String getFritekstTilBrev() {
        return fritekstTilBrev;
    }

    public Hjemmel getHjemmel() {
        return hjemmel;
    }

    public KlageMedholdÅrsak getKlageMedholdArsakKode() {
        return klageMedholdArsakKode;
    }

    public KlageVurderingOmgjør getKlageVurderingOmgjoer() {
        return klageVurderingOmgjoer;
    }

    public KlageVurdertAv getKlageVurdertAv() {
        return klageVurdertAv;
    }

    public boolean skalOversendesTilNK(Behandling behandling) {
        return getKlageVurdertAv() == KlageVurdertAv.NAY &&
            getKlageVurdering() == KlageVurderingType.STADFESTE_YTELSESVEDTAK;
    }
}
