package no.nav.k9.sak.behandling.revurdering.felles;


import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarsel;


public interface HarEtablertYtelse {

    boolean vurder(boolean finnesInnvilgetIkkeOpphørtVedtak);

    VedtakVarsel fastsettForIkkeEtablertYtelse(Behandling revurdering);

}
