package no.nav.ung.sak.behandlingslager.bosatt;

import no.nav.ung.kodeverk.bosatt.Kilde;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;

/**
 * Fletter sammen oppgitt fakta fra søknad ({@link BostedsinformasjonFraSøknad}) med en eventuell foreslått
 * avklaring fra saksbehandler i gjeldende behandling og/eller en tidligere ferdigstilt (vedtatt) avklaring,
 * for én periode.
 * <p>
 * Foreslått og ferdigstilt avklaring holdes som to atskilte felter — istedenfor å flettes til én felles
 * avklaring med et supplerende flagg — slik at det alltid er eksplisitt hvilken av de to en gitt avklaring er.
 * Bygges opp trinnvis via {@link #medForeslåttAvklaring(BostedsPeriodeAvklaring)} og {@link #medFerdigstiltAvklaring(BostedsPeriodeAvklaring)}
 * ettersom {@link BostedsGrunnlag} fletter søknadsfakta med de to avklaringstidslinjene i separate steg.
 * Foreslått avklaring er kilde til sannhet der begge finnes ({@link Kilde#SAKSBEHANDLER}); ellers benyttes
 * ferdigstilt avklaring om den finnes, eller fakta fra søknaden ({@link Kilde#SØKNAD}).
 */
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

    // getKilde kan kun brukes for gjeldende behandling (Avklaringer foreslått i denne behandlingen)
    public Kilde getKilde() {
        return harForeslåttAvklaring() ? Kilde.SAKSBEHANDLER : Kilde.SØKNAD;
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
