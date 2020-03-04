package no.nav.foreldrepenger.behandling.revurdering.ytelse;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.felles.HarEtablertYtelse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakVarsel;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.vedtak.Vedtaksbrev;

@ApplicationScoped
@FagsakYtelseTypeRef
public class HarEtablertYtelseImpl implements HarEtablertYtelse {

    private VedtakVarselRepository vedtakVarselRepository;

    protected HarEtablertYtelseImpl() {
    }

    @Inject
    public HarEtablertYtelseImpl(VedtakVarselRepository vedtakVarselRepository) {
        this.vedtakVarselRepository = vedtakVarselRepository;
    }

    @Override
    public boolean vurder(boolean finnesInnvilgetIkkeOpphørtVedtak) {
        return harEtablertYtelse(finnesInnvilgetIkkeOpphørtVedtak);
    }

    private boolean harEtablertYtelse(boolean erMinstEnInnvilgetBehandlingUtenPåfølgendeOpphør) {
        return erMinstEnInnvilgetBehandlingUtenPåfølgendeOpphør;
    }

    @Override
    public VedtakVarsel fastsettForIkkeEtablertYtelse(Behandling revurdering) {
        revurdering.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        VedtakVarsel vedtakVarsel = vedtakVarselRepository.hentHvisEksisterer(revurdering.getId()).orElse(new VedtakVarsel());
        vedtakVarsel.setVedtaksbrev(Vedtaksbrev.AUTOMATISK);
        return vedtakVarsel;
    }

}
