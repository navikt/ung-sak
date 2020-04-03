package no.nav.k9.sak.skjæringstidspunkt;

import java.time.LocalDate;
import java.time.Period;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.threeten.extra.Interval;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class OpplysningsPeriodeTjeneste {

    private Period periodeFør;
    private Period periodeEtter;

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    OpplysningsPeriodeTjeneste() {
        // CDI
    }

    /**
     * Konfig angir perioden med registerinnhenting før/etter skjæringstidspunktet (for en gitt ytelse)
     */
    @Inject
    public OpplysningsPeriodeTjeneste(SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                      @KonfigVerdi(value = "PSB.registerinnhenting.opplysningsperiode.før", defaultVerdi = "P17M") Period periodeFør,
                                      @KonfigVerdi(value = "PSB.registerinnhenting.opplysningsperiode.etter", defaultVerdi = "P4Y") Period periodeEtter) {

        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.periodeFør = periodeFør;
        this.periodeEtter = periodeEtter;
    }

    /**
     * Beregner opplysningsperioden (Perioden vi ber om informasjon fra registerne) for en gitt behandling.
     *
     * Benytter konfig-verdier for å setter lengden på intervallene på hver side av skjæringstidspunkt for registerinnhenting.
     *
     * @param behandling behandlingen
     * @return intervallet
     */
    public Interval beregn(Long behandlingId, FagsakYtelseType ytelseType) {
        return beregning(behandlingId, ytelseType, false);
    }

    public Interval beregnTilOgMedIdag(Long behandlingId, FagsakYtelseType ytelseType) {
        return beregning(behandlingId, ytelseType, true);
    }

    private Interval beregning(Long behandlingId, @SuppressWarnings("unused") FagsakYtelseType ytelseType, boolean tilOgMedIdag) {
        final LocalDate skjæringstidspunkt = skjæringstidspunktTjeneste.utledSkjæringstidspunktForRegisterInnhenting(behandlingId, ytelseType);
        // FIXME K9 Blir dette riktig for alle våre ytelser?
        return beregnInterval(skjæringstidspunkt, tilOgMedIdag);
    }

    private Interval beregnInterval(LocalDate skjæringstidspunkt, boolean tilOgMedIdag) {
        return beregnInterval(skjæringstidspunkt.minus(periodeFør), skjæringstidspunkt.plus(periodeEtter), tilOgMedIdag);
    }

    private Interval beregnInterval(LocalDate fom, LocalDate tom, boolean tilOgMedIdag) {
        return IntervallUtil.byggIntervall(fom, tilOgMedIdag && tom.isBefore(LocalDate.now()) ? LocalDate.now() : tom);
    }
}
