package no.nav.k9.sak.domene.opptjening;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;

@ApplicationScoped
public class OppgittOpptjeningTjenesteProvider {
    private Instance<OppgittOpptjeningTjeneste> søktePerioderProvidere;
    private BehandlingRepository behandlingRepository;

    protected OppgittOpptjeningTjenesteProvider() {
        // for proxy
    }

    @Inject
    public OppgittOpptjeningTjenesteProvider(@Any Instance<OppgittOpptjeningTjeneste> søktePerioderProvidere, BehandlingRepository behandlingRepository) {
        this.søktePerioderProvidere = søktePerioderProvidere;
        this.behandlingRepository = behandlingRepository;
    }

    public OppgittOpptjeningTjeneste finnSøktePerioderProvider(long behandlingId) {
        Objects.requireNonNull(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        FagsakYtelseType ytelseType = behandling.getFagsakYtelseType();
        return FagsakYtelseTypeRef.Lookup.find(søktePerioderProvidere, ytelseType)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke støtte for ytelseType:" + ytelseType));
    }
}
