package no.nav.ung.sak.behandlingslager.behandling.klage;



import no.nav.ung.kodeverk.klage.KlageAvvistÅrsak;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;

import java.util.ArrayList;
import java.util.List;

public class KlageFormkravAdapter {

    private boolean erKlagerPart;
    private boolean erFristOverholdt;
    private boolean erKonkret;
    private boolean erSignert;
    private boolean gjelderVedtak;
    private String begrunnelse;
    private Long klageBehandlingId;
    private KlageVurdertAv klageVurdertAv;

    public KlageFormkravAdapter(KlageFormkravEntitet klageFormkrav) {
        this.erKlagerPart = klageFormkrav.erKlagerPart();
        this.erFristOverholdt = klageFormkrav.erFristOverholdt();
        this.erKonkret = klageFormkrav.erKonkret();
        this.erSignert = klageFormkrav.erSignert();
        this.gjelderVedtak = klageFormkrav.hentGjelderVedtak();
        this.begrunnelse = klageFormkrav.hentBegrunnelse();
//        this.klageVurdertAv = klageFormkrav.getKlageVurdertAv();
//        this.klageBehandlingId = klageFormkrav.hentKlageResultat().getKlageBehandling().getId();
    }

    public KlageFormkravAdapter(boolean erKlagerPart,
                                boolean erFristOverholdt,
                                boolean erKonkret,
                                boolean erSignert,
                                boolean gjelderVedtak,
                                String begrunnelse,
                                Long klageBehandlingId,
                                KlageVurdertAv klageVurdertAv) {
        this.erKlagerPart = erKlagerPart;
        this.erFristOverholdt = erFristOverholdt;
        this.erKonkret = erKonkret;
        this.erSignert = erSignert;
        this.gjelderVedtak = gjelderVedtak;
        this.begrunnelse = begrunnelse;
        this.klageBehandlingId = klageBehandlingId;
        this.klageVurdertAv = klageVurdertAv;
    }

    public boolean isErKlagerPart() {
        return erKlagerPart;
    }

    public boolean isErFristOverholdt() {
        return erFristOverholdt;
    }

    public boolean isErKonkret() {
        return erKonkret;
    }

    public boolean isErSignert() {
        return erSignert;
    }

    public boolean gjelderVedtak() {
        return gjelderVedtak;
    }

    public boolean erAvvist() {
        return !erKlagerPart || !erKonkret || !erSignert || !gjelderVedtak;
    }

    public List<KlageAvvistÅrsak> hentAvvistÅrsaker() {
        List<KlageAvvistÅrsak> avvistÅrsaker = new ArrayList<>();
        if (!gjelderVedtak) {
            avvistÅrsaker.add(KlageAvvistÅrsak.IKKE_PAKLAGD_VEDTAK);
        }
        if (!erFristOverholdt) {
            avvistÅrsaker.add(KlageAvvistÅrsak.KLAGET_FOR_SENT);
        }
        if (!erKlagerPart) {
            avvistÅrsaker.add(KlageAvvistÅrsak.KLAGER_IKKE_PART);
        }
        if (!erKonkret) {
            avvistÅrsaker.add(KlageAvvistÅrsak.IKKE_KONKRET);
        }
        if (!erSignert) {
            avvistÅrsaker.add(KlageAvvistÅrsak.IKKE_SIGNERT);
        }
        return avvistÅrsaker;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public Long getKlageBehandlingId() {
        return klageBehandlingId;
    }

    public KlageVurdertAv getKlageVurdertAvKode() {
        return klageVurdertAv;
    }
}
