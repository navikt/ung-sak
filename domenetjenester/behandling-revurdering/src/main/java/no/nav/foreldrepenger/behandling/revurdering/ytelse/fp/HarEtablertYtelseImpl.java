package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandling.revurdering.felles.HarEtablertYtelse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;

@ApplicationScoped
@FagsakYtelseTypeRef("FP")
public class HarEtablertYtelseImpl implements HarEtablertYtelse {

    @Override
    public boolean vurder(boolean finnesInnvilgetIkkeOpphørtVedtak) {
        return harEtablertYtelse(finnesInnvilgetIkkeOpphørtVedtak);
    }

    private boolean harEtablertYtelse(boolean erMinstEnInnvilgetBehandlingUtenPåfølgendeOpphør) {
        return erMinstEnInnvilgetBehandlingUtenPåfølgendeOpphør;
    }

    @Override
    public Behandlingsresultat fastsettForIkkeEtablertYtelse(Behandling revurdering, List<KonsekvensForYtelsen> konsekvenserForYtelsen) {
        Behandlingsresultat behandlingsresultat = revurdering.getBehandlingsresultat();
        Behandlingsresultat.Builder behandlingsresultatBuilder = Behandlingsresultat.builderEndreEksisterende(behandlingsresultat);
        konsekvenserForYtelsen.forEach(behandlingsresultatBuilder::leggTilKonsekvensForYtelsen);
        behandlingsresultatBuilder.medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandlingsresultatBuilder.medVedtaksbrev(Vedtaksbrev.AUTOMATISK);
        return behandlingsresultatBuilder.buildFor(revurdering);
    }

}
