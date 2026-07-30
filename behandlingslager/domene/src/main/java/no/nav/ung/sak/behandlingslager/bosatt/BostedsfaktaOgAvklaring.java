package no.nav.ung.sak.behandlingslager.bosatt;

import no.nav.ung.kodeverk.bosatt.Kilde;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import java.time.LocalDateTime;
import java.util.UUID;

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
    private final BostedsPeriodeAvklaring foreslåttAvklaring;

    public BostedsfaktaOgAvklaring(BostedsinformasjonFraSøknad søknadsinformasjon, BostedsPeriodeAvklaring foreslåttAvklaring) {
        this.søknadsinformasjon = søknadsinformasjon;
        this.foreslåttAvklaring = foreslåttAvklaring;
    }

    public BostedsinformasjonFraSøknad getSøknadsinformasjon() {
        return søknadsinformasjon;
    }

    public BostedsPeriodeAvklaring getForeslåttAvklaring() {
        return foreslåttAvklaring;
    }

    public boolean harForeslåttAvklaring() {
        return foreslåttAvklaring != null;
    }

    public Kilde getKilde() {
        return harForeslåttAvklaring() ? Kilde.SAKSBEHANDLER : Kilde.SØKNAD;
    }

    public boolean isErBosattITrondheim() {
        return harForeslåttAvklaring() ? foreslåttAvklaring.isErBosattITrondheim() : søknadsinformasjon.isErBosattITrondheim();
    }

    public BostedsvilkårIkkeOppfyltÅrsak getIkkeOppfyltÅrsak() {
        return harForeslåttAvklaring() ? foreslåttAvklaring.getIkkeOppfyltÅrsak() : null;
    }

    @Override
    public String toString() {
        return "BostedsfaktaOgAvklaring{"
            + "kilde=" + getKilde()
            + ", erBosattITrondheim=" + isErBosattITrondheim() + '}';
    }
}

