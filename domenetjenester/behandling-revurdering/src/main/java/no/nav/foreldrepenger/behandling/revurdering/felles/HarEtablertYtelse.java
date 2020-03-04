package no.nav.foreldrepenger.behandling.revurdering.felles;


import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakVarsel;


public interface HarEtablertYtelse {

    boolean vurder(boolean finnesInnvilgetIkkeOpphørtVedtak);

    VedtakVarsel fastsettForIkkeEtablertYtelse(Behandling revurdering);

}
