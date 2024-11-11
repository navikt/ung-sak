package no.nav.ung.sak.behandling.revurdering.ytelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.ung.sak.behandling.revurdering.felles.HarEtablertYtelse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;

@ApplicationScoped
@FagsakYtelseTypeRef
public class HarEtablertYtelseImpl implements HarEtablertYtelse {

    @Inject
    public HarEtablertYtelseImpl() { }

    @Override
    public boolean vurder(boolean finnesInnvilgetIkkeOpphørtVedtak) {
        return harEtablertYtelse(finnesInnvilgetIkkeOpphørtVedtak);
    }

    private boolean harEtablertYtelse(boolean erMinstEnInnvilgetBehandlingUtenPåfølgendeOpphør) {
        return erMinstEnInnvilgetBehandlingUtenPåfølgendeOpphør;
    }
}
