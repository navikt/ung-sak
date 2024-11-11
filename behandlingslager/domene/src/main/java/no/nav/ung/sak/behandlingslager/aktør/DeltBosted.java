package no.nav.ung.sak.behandlingslager.aktør;

import no.nav.ung.sak.typer.Periode;

public class DeltBosted {

    private Periode periode;
    private Adresseinfo adresseinfo;

    public DeltBosted(Periode periode, Adresseinfo adresseinfo) {
        this.periode = periode;
        this.adresseinfo = adresseinfo;
    }

    public Periode getPeriode() {
        return periode;
    }

    public Adresseinfo getAdresseinfo() {
        return adresseinfo;
    }

}
