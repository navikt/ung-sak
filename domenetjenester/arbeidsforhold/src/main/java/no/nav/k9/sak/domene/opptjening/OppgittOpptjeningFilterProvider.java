package no.nav.k9.sak.domene.opptjening;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;

@ApplicationScoped
public class OppgittOpptjeningFilterProvider {
    private static final Logger logger = LoggerFactory.getLogger(OppgittOpptjeningFilterProvider.class);

    private Instance<OppgittOpptjeningFilter> oppgittOpptjeningFiltere;
    private BehandlingRepository behandlingRepository;

    private OppgittOpptjeningFilter defaultOpptjeningFilter = new OppgittOpptjeningFilter() {
    };

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
            .orElseGet(() -> {
                logger.info("Har ikke spesifikt opptjening-filter for {}, bruker default impelmentasjon", ytelseType);
                return defaultOpptjeningFilter;
            });
    }

}
