package no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.bosatt;

import no.nav.ung.kodeverk.bosatt.Kilde;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsinformasjonFraSøknad;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;

public class BostedsfaktaOgAvklaring {

    private final BostedsinformasjonFraSøknad søknadsinformasjon;
    private final VilkårPeriodeAvklaring foreslåttAvklaring;
    private final VilkårPeriodeAvklaring ferdigstiltAvklaring;

    private BostedsfaktaOgAvklaring(BostedsinformasjonFraSøknad søknadsinformasjon, VilkårPeriodeAvklaring foreslåttAvklaring, VilkårPeriodeAvklaring ferdigstiltAvklaring) {
        this.søknadsinformasjon = søknadsinformasjon;
        this.foreslåttAvklaring = foreslåttAvklaring;
        this.ferdigstiltAvklaring = ferdigstiltAvklaring;
    }

    static BostedsfaktaOgAvklaring fraSøknad(BostedsinformasjonFraSøknad søknadsinformasjon) {
        return new BostedsfaktaOgAvklaring(søknadsinformasjon, null, null);
    }

    BostedsfaktaOgAvklaring medForeslåttAvklaring(VilkårPeriodeAvklaring foreslåttAvklaring) {
        return foreslåttAvklaring == null ? this : new BostedsfaktaOgAvklaring(søknadsinformasjon, foreslåttAvklaring, ferdigstiltAvklaring);
    }

    BostedsfaktaOgAvklaring medFerdigstiltAvklaring(VilkårPeriodeAvklaring ferdigstiltAvklaring) {
        return ferdigstiltAvklaring == null ? this : new BostedsfaktaOgAvklaring(søknadsinformasjon, foreslåttAvklaring, ferdigstiltAvklaring);
    }

    public BostedsinformasjonFraSøknad getSøknadsinformasjon() {
        return søknadsinformasjon;
    }

    public VilkårPeriodeAvklaring getForeslåttAvklaring() {
        return foreslåttAvklaring;
    }

    public VilkårPeriodeAvklaring getFerdigstiltAvklaring() {
        return ferdigstiltAvklaring;
    }

    /**
     * Den avklaringen som er gjeldende for perioden — foreslått avklaring har forrang over ferdigstilt.
     */
    public VilkårPeriodeAvklaring getGjeldendeAvklaring() {
        return foreslåttAvklaring != null ? foreslåttAvklaring : ferdigstiltAvklaring;
    }

    public boolean harAvklaring() {
        return getGjeldendeAvklaring() != null;
    }

    public boolean harForeslåttAvklaring() {
        return foreslåttAvklaring != null;
    }

    public boolean kanRedigeres() {
        return foreslåttAvklaring != null;
    }

    public Kilde getKilde() {
        return harAvklaring() ? Kilde.SAKSBEHANDLER : Kilde.SØKNAD;
    }

    public boolean isErBosattITrondheim() {
        return !harAvklaring() && søknadsinformasjon.isErBosattITrondheim();
    }


    @Override
    public String toString() {
        return "BostedsfaktaOgAvklaring{"
            + "kilde=" + getKilde()
            + ", erBosattITrondheim=" + isErBosattITrondheim() + '}';
    }
}
