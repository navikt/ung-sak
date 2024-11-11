package no.nav.ung.sak.ytelse.beregning;

import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.ung.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.ung.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;

@ApplicationScoped
public class SjekkOmPerioderInneholderSammeAndeler {

    public SjekkOmPerioderInneholderSammeAndeler() {
        // for CDI proxy
    }

    /**
     * Sjekker om det har skjedd en endring i andelene mellom to perioder.
     * Hvis periodene har forskjellig antall perioder har det alltid skjedd en endring, og dermed returner man true.
     * Hvis periodene har samme antall andeler, sjekk hver enkel andel for en korresponderende andel med samme verdi.
     * Return true hvis alle andelene har en korresponderende verdi, false ellers.
     * Hvis en andel har mer enn en korresponderende andel har det skjedd en feil og en exception blir kastet.
     *
     * @param nyPeriode     Periode for revurdering
     * @param gammelPeriode Periode for førstegangsbehandling
     * @return True hvis det har skjedd en endring
     * False hvis det ikke har skjedd en endring
     */
    public boolean sjekk(BeregningsresultatPeriode nyPeriode, BeregningsresultatPeriode gammelPeriode) {
        List<BeregningsresultatAndel> nyeAndeler = nyPeriode.getBeregningsresultatAndelList();
        List<BeregningsresultatAndel> gamleAndeler = gammelPeriode.getBeregningsresultatAndelList();
        if (nyeAndeler.size() != gamleAndeler.size()) {
            return false;
        }
        return nyeAndeler.stream().allMatch(nyAndel -> finnKorresponderendeAndel(nyAndel, gamleAndeler));
    }

    private boolean finnKorresponderendeAndel(BeregningsresultatAndel nyAndel, List<BeregningsresultatAndel> gamleAndeler) {
        var nyAndelNøkkel = nyAndel.getAktivitetsnøkkel();
        long antallAndelerSomKorresponderer = gamleAndeler.stream().filter(gammelAndel ->
            Objects.equals(nyAndel.erBrukerMottaker(), gammelAndel.erBrukerMottaker()) &&
                Objects.equals(nyAndelNøkkel, gammelAndel.getAktivitetsnøkkel()) &&
                Objects.equals(nyAndel.getDagsats(), gammelAndel.getDagsats())).count();
        if (antallAndelerSomKorresponderer > 1) {
            throw new IllegalArgumentException("Fant flere korresponderende andeler for andel med id " + nyAndel.getId());
        }
        return antallAndelerSomKorresponderer == 1;
    }

}
