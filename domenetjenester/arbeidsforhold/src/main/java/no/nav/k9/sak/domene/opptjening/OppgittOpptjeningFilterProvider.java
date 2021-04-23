package no.nav.k9.sak.domene.opptjening;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;

@ApplicationScoped
public class OppgittOpptjeningFilterProvider {
    private Instance<OppgittOpptjeningFilter> oppgittOpptjeningFiltere;
    private BehandlingRepository behandlingRepository;

    protected OppgittOpptjeningFilterProvider() {
        // for proxy
    }

    @Inject
    public OppgittOpptjeningFilterProvider(@Any Instance<OppgittOpptjeningFilter> oppgittOpptjeningFiltere, BehandlingRepository behandlingRepository) {
        this.oppgittOpptjeningFiltere = oppgittOpptjeningFiltere;
        this.behandlingRepository = behandlingRepository;
    }

    public OppgittOpptjeningFilter finnOpptjeningFilter(long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        FagsakYtelseType ytelseType = behandling.getFagsakYtelseType();
        return FagsakYtelseTypeRef.Lookup.find(oppgittOpptjeningFiltere, ytelseType)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke støtte for ytelseType:" + ytelseType));
    }
}
