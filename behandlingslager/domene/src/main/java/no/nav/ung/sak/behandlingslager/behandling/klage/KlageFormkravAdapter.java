package no.nav.ung.sak.behandlingslager.behandling.klage;


import no.nav.ung.kodeverk.hjemmel.KlageHjemmel;
import no.nav.ung.kodeverk.klage.KlageAvvistÅrsak;
import no.nav.ung.kodeverk.klage.KlageVurderingType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.ung.kodeverk.hjemmel.KlageHjemmel.*;

public class KlageFormkravAdapter {

    private boolean erKlagerPart;
    private boolean erFristOverholdt;
    private boolean erKonkret;
    private boolean erSignert;
    private boolean gjelderVedtak;
    private String begrunnelse;

    public KlageFormkravAdapter(KlageFormkravEntitet klageFormkrav) {
        this.erKlagerPart = klageFormkrav.erKlagerPart();
        this.erFristOverholdt = klageFormkrav.erFristOverholdt();
        this.erKonkret = klageFormkrav.erKonkret();
        this.erSignert = klageFormkrav.erSignert();
        this.gjelderVedtak = klageFormkrav.hentGjelderVedtak();
        this.begrunnelse = klageFormkrav.hentBegrunnelse();
    }

    public KlageFormkravAdapter(boolean erKlagerPart,
                                boolean erFristOverholdt,
                                boolean erKonkret,
                                boolean erSignert,
                                boolean gjelderVedtak,
                                String begrunnelse) {
        this.erKlagerPart = erKlagerPart;
        this.erFristOverholdt = erFristOverholdt;
        this.erKonkret = erKonkret;
        this.erSignert = erSignert;
        this.gjelderVedtak = gjelderVedtak;
        this.begrunnelse = begrunnelse;
    }

    public boolean isErKlagerPart() {
        return erKlagerPart;
    }

    public boolean isFristOverholdt() {
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
        return !erFristOverholdt || !erKlagerPart || !erKonkret || !erSignert || !gjelderVedtak;
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

    public Optional<KlageVurderingType> hentVurderingTypeHvisAvvist() {
        if (erAvvist()) {
            return Optional.of(KlageVurderingType.AVVIS_KLAGE);
        }
        return Optional.empty();
    }

    public List<KlageHjemmel> hentFolketrygdParagrafer() {
        List<KlageHjemmel> paragrafer = new ArrayList<>();

        if (!isFristOverholdt()) {
            paragrafer.add(FTRL_KLAGE_ANKE_TRYGDESAKER);
        }

        return paragrafer;
    }

    public List<KlageHjemmel> hentForvaltningslovParagrafer() {
        List<KlageHjemmel> paragrafer = new ArrayList<>();

        paragrafer.add(FL_SAKSFORBEREDELSE_I_KLAGESAK);

        if (!gjelderVedtak()) {
            paragrafer.add(FL_VEDTAK_SOM_KAN_PÅKLAGES);
        }

        if (!isErKlagerPart()) {
            paragrafer.add(FL_VEDTAK_SOM_KAN_PÅKLAGES);
        }

        if (!isFristOverholdt()) {
            paragrafer.add(FL_OVERSITTING_AV_KLAGEFRIST);
        }

        if (!isErKonkret()) {
            paragrafer.add(FL_ADRESSAT_FORM_OG_INNHOLD);
        }

        if (!isErSignert()) {
            paragrafer.add(FL_ADRESSAT_FORM_OG_INNHOLD);
        }

        return paragrafer.stream()
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }


    public String getBegrunnelse() {
        return begrunnelse;
    }
}
