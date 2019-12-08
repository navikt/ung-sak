package no.nav.foreldrepenger.skjæringstidspunkt;

import java.time.LocalDate;

import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;

/** Brukes som factory for å gi spesifikk tjeneste avh. av ytelse. */
public class SkjæringstidspunktRegisterinnhentingTjenesteImpl implements SkjæringstidspunktRegisterinnhentingTjeneste {

    private SkjæringstidspunktRegisterinnhentingTjeneste skjæringstidspunktTjeneste;

    public SkjæringstidspunktRegisterinnhentingTjenesteImpl() {
        // for CDI
    }

    @Inject
    public SkjæringstidspunktRegisterinnhentingTjenesteImpl(@FagsakYtelseTypeRef("FP") SkjæringstidspunktRegisterinnhentingTjeneste skjæringstidspunktTjeneste) {
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    public LocalDate utledSkjæringstidspunktForRegisterInnhenting(Long behandlingId) {
        return skjæringstidspunktTjeneste.utledSkjæringstidspunktForRegisterInnhenting(behandlingId);
    }

}
