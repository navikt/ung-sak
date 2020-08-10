package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;

/**
 * Samle sammen fakta for fravær.
 */
@ApplicationScoped
@BehandlingStegRef(kode = "INIT_PERIODER")
@BehandlingTypeRef
@FagsakYtelseTypeRef("OMP")
public class InitierPerioderSteg implements BehandlingSteg {

    private OmsorgspengerGrunnlagRepository grunnlagRepository;
    private TrekkUtFraværTjeneste trekkUtFraværTjeneste;

    protected InitierPerioderSteg() {
        // for proxy
    }

    @Inject
    public InitierPerioderSteg(OmsorgspengerGrunnlagRepository grunnlagRepository,
                               TrekkUtFraværTjeneste trekkUtFraværTjeneste) {
        this.trekkUtFraværTjeneste = trekkUtFraværTjeneste;
        this.grunnlagRepository = grunnlagRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();

        var samletFravær = trekkUtFraværTjeneste.samleSammenOppgittFravær(behandlingId);
        grunnlagRepository.lagreOgFlushOppgittFravær(behandlingId, samletFravær);

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
