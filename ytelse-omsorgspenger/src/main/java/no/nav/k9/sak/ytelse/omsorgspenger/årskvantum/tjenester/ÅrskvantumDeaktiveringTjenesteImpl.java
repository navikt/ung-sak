package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.vedtak.årskvantum.ÅrskvantumDeaktiveringTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest.ÅrskvantumKlient;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest.ÅrskvantumRestKlient;

@FagsakYtelseTypeRef("OMP")
@ApplicationScoped
public class ÅrskvantumDeaktiveringTjenesteImpl implements ÅrskvantumDeaktiveringTjeneste {

    private static final Logger log = LoggerFactory.getLogger(ÅrskvantumDeaktiveringTjenesteImpl.class);

    private ÅrskvantumKlient årskvantumKlient;

    ÅrskvantumDeaktiveringTjenesteImpl() {
        // CDI
    }

    @Inject
    public ÅrskvantumDeaktiveringTjenesteImpl(ÅrskvantumRestKlient årskvantumRestKlient) {
        this.årskvantumKlient = årskvantumRestKlient;
    }

    @Override
    public void deaktiverUttakForBehandling(UUID behandlingUuid) {
        årskvantumKlient.deaktiverUttakForBehandling(behandlingUuid);
    }
}
