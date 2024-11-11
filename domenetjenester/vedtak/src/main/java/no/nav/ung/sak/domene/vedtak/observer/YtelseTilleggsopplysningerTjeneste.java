package no.nav.ung.sak.domene.vedtak.observer;

import no.nav.ung.sak.behandlingslager.behandling.Behandling;

public interface YtelseTilleggsopplysningerTjeneste {

    public Tilleggsopplysning generer(Behandling behandling);
}
