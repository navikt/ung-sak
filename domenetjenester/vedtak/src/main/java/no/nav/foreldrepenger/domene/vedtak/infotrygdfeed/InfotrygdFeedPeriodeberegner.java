package no.nav.foreldrepenger.domene.vedtak.infotrygdfeed;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.typer.Saksnummer;

public interface InfotrygdFeedPeriodeberegner {
    /**
     * FagsakYtelseType som er støttet av denne periodeberegneren
     */
    FagsakYtelseType getFagsakYtelseType();

    /**
     * Infotrygdkode for FagsakYtelseType som er støttet av denne periodeberegneren
     */
    String getInfotrygdYtelseKode();

    /**
     * For alle gjeldende perioder: beregn første fom-dato og siste tom-dato.
     *
     * Resultatet kan også inneholde fom- og tomdatoer av typen Tid.TIDENES_BEGYNNELSE eller Tid.TIDENES_ENDE. Dette
     * håndteres av den som kaller denne metoden.
     *
     * @return siste fom- og tom-dato.
     */
    InfotrygdFeedPeriode finnInnvilgetPeriode(Saksnummer saksnummer);
}
