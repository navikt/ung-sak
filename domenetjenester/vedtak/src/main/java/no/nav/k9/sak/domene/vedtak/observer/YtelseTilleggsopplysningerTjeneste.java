package no.nav.k9.sak.domene.vedtak.observer;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;

public interface YtelseTilleggsopplysningerTjeneste {

    public Tilleggsopplysning generer(Behandling behandling);
}
