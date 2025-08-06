package no.nav.ung.sak.behandlingslager.behandling.klage;

import no.nav.ung.kodeverk.hjemmel.Hjemmel;
import no.nav.ung.kodeverk.klage.KlageMedholdÅrsak;
import no.nav.ung.kodeverk.klage.KlageVurdering;
import no.nav.ung.kodeverk.klage.KlageVurderingOmgjør;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;

public class KlageVurderingAdapter {
    private KlageVurdering klageVurdering;
    private String begrunnelse;
    private String fritekstTilBrev;
    private Hjemmel hjemmel;
    private KlageMedholdÅrsak klageMedholdArsakKode;
    private KlageVurderingOmgjør klageVurderingOmgjoer;
    private KlageVurdertAv klageVurdertAv;

    public KlageVurderingAdapter(KlageVurdering klageVurdering,
                                 KlageMedholdÅrsak klageMedholdArsakKode,
                                 KlageVurderingOmgjør klageVurderingOmgjør,
                                 String begrunnelse,
                                 String fritekstTilBrev,
                                 Hjemmel hjemmel,
                                 KlageVurdertAv klageVurdertAv) {
        this.klageVurdering = klageVurdering;
        this.begrunnelse = begrunnelse;
        this.fritekstTilBrev = fritekstTilBrev;
        this.hjemmel = hjemmel;
        this.klageMedholdArsakKode = klageMedholdArsakKode;
        this.klageVurderingOmgjoer = klageVurderingOmgjør;
        this.klageVurdertAv = klageVurdertAv;
    }

    public KlageVurdering getKlageVurdering() {
        return klageVurdering;
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
            getKlageVurdering() == KlageVurdering.STADFESTE_YTELSESVEDTAK;
    }
}
