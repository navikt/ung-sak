package no.nav.ung.sak.behandlingslager.bosatt;

import no.nav.ung.kodeverk.bosatt.Kilde;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;

/**
 * Fletter sammen oppgitt fakta fra søknad ({@link BostedsinformasjonFraSøknad}) og eventuell foreslått
 * avklaring fra saksbehandler ({@link BostedsPeriodeAvklaring}) for én periode.
 * <p>
 * Klassen leverer de samme feltene som {@link BostedsPeriodeAvklaring} og supplerer med opplysningene
 * fra søknad samt {@link Kilde}. Dersom det finnes en foreslått avklaring er den kilde til sannhet
 * ({@link Kilde#SAKSBEHANDLER}); ellers benyttes fakta fra søknaden ({@link Kilde#SØKNAD}).
 */
public class BostedsfaktaOgAvklaring {

    private final BostedsinformasjonFraSøknad søknadsinformasjon;
    private final BostedsPeriodeAvklaring foreslåttAvslagsavklaring;

    public BostedsfaktaOgAvklaring(BostedsinformasjonFraSøknad søknadsinformasjon, BostedsPeriodeAvklaring foreslåttAvslagsavklaring) {
        this.søknadsinformasjon = søknadsinformasjon;
        this.foreslåttAvslagsavklaring = foreslåttAvslagsavklaring;
    }

    public BostedsinformasjonFraSøknad getSøknadsinformasjon() {
        return søknadsinformasjon;
    }

    public BostedsPeriodeAvklaring getForeslåttAvslagsavklaring() {
        return foreslåttAvslagsavklaring;
    }

    public boolean harForeslåttAvslagsavklaring() {
        return foreslåttAvslagsavklaring != null;
    }

    public Kilde getKilde() {
        return harForeslåttAvslagsavklaring() ? Kilde.SAKSBEHANDLER : Kilde.SØKNAD;
    }

    public boolean isErBosattITrondheim() {
        return !harForeslåttAvslagsavklaring() && søknadsinformasjon.isErBosattITrondheim();
    }

    public BostedsvilkårIkkeOppfyltÅrsak getIkkeOppfyltÅrsak() {
        return harForeslåttAvslagsavklaring() ? foreslåttAvslagsavklaring.getIkkeOppfyltÅrsak() : null;
    }

    @Override
    public String toString() {
        return "BostedsfaktaOgAvklaring{"
            + "kilde=" + getKilde()
            + ", erBosattITrondheim=" + isErBosattITrondheim() + '}';
    }
}

