package no.nav.ung.sak.behandlingslager.bosatt;

import no.nav.ung.kodeverk.bosatt.Kilde;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;

public class BostedsfaktaOgAvklaring {

    private final BostedsinformasjonFraSøknad søknadsinformasjon;
    private final BostedsPeriodeAvklaring foreslåttAvklaring;
    private final BostedsPeriodeAvklaring ferdigstiltAvklaring;

    private BostedsfaktaOgAvklaring(BostedsinformasjonFraSøknad søknadsinformasjon, BostedsPeriodeAvklaring foreslåttAvklaring, BostedsPeriodeAvklaring ferdigstiltAvklaring) {
        this.søknadsinformasjon = søknadsinformasjon;
        this.foreslåttAvklaring = foreslåttAvklaring;
        this.ferdigstiltAvklaring = ferdigstiltAvklaring;
    }

    static BostedsfaktaOgAvklaring fraSøknad(BostedsinformasjonFraSøknad søknadsinformasjon) {
        return new BostedsfaktaOgAvklaring(søknadsinformasjon, null, null);
    }

    BostedsfaktaOgAvklaring medForeslåttAvklaring(BostedsPeriodeAvklaring foreslåttAvklaring) {
        return foreslåttAvklaring == null ? this : new BostedsfaktaOgAvklaring(søknadsinformasjon, foreslåttAvklaring, ferdigstiltAvklaring);
    }

    BostedsfaktaOgAvklaring medFerdigstiltAvklaring(BostedsPeriodeAvklaring ferdigstiltAvklaring) {
        return ferdigstiltAvklaring == null ? this : new BostedsfaktaOgAvklaring(søknadsinformasjon, foreslåttAvklaring, ferdigstiltAvklaring);
    }

    public BostedsinformasjonFraSøknad getSøknadsinformasjon() {
        return søknadsinformasjon;
    }

    public BostedsPeriodeAvklaring getForeslåttAvklaring() {
        return foreslåttAvklaring;
    }

    public BostedsPeriodeAvklaring getFerdigstiltAvklaring() {
        return ferdigstiltAvklaring;
    }

    /**
     * Den avklaringen som er gjeldende for perioden — foreslått avklaring har forrang over ferdigstilt.
     */
    public BostedsPeriodeAvklaring getGjeldendeAvklaring() {
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
