package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.regelmodell;

import java.util.List;

public class NødvendighetVilkårResultat {

    private List<NødvendigOpplæringPeriode> nødvendigOpplæringPerioder;
    private List<GodkjentInstitusjonPeriode> godkjentInstitusjonPerioder;
    private List<GodkjentSykdomsvilkårPeriode> godkjentSykdomPerioder;

    public List<NødvendigOpplæringPeriode> getNødvendigOpplæringPerioder() {
        return nødvendigOpplæringPerioder;
    }

    public void setNødvendigOpplæringPerioder(List<NødvendigOpplæringPeriode> nødvendigOpplæringPerioder) {
        this.nødvendigOpplæringPerioder = nødvendigOpplæringPerioder;
    }

    public List<GodkjentInstitusjonPeriode> getGodkjentInstitusjonPerioder() {
        return godkjentInstitusjonPerioder;
    }

    public void setGodkjentInstitusjonPerioder(List<GodkjentInstitusjonPeriode> godkjentInstitusjonPerioder) {
        this.godkjentInstitusjonPerioder = godkjentInstitusjonPerioder;
    }

    public List<GodkjentSykdomsvilkårPeriode> getGodkjentSykdomPerioder() {
        return godkjentSykdomPerioder;
    }

    public void setGodkjentSykdomPerioder(List<GodkjentSykdomsvilkårPeriode> godkjentSykdomPerioder) {
        this.godkjentSykdomPerioder = godkjentSykdomPerioder;
    }
}
