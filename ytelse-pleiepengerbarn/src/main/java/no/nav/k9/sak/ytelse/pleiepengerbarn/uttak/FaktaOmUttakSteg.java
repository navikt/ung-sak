package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import java.util.ArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynForPleietrengende;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynGrunnlagRepository;

@ApplicationScoped
@BehandlingStegRef(kode = "KOFAKUT")
@BehandlingTypeRef
@FagsakYtelseTypeRef("PSB")
public class FaktaOmUttakSteg implements BehandlingSteg {

    private UnntakEtablertTilsynGrunnlagRepository unntakEtablertTilsynGrunnlagRepository;
    private BehandlingRepository behandlingRepository;

    protected FaktaOmUttakSteg() {
        // for proxy
    }

    @Inject
    public FaktaOmUttakSteg(UnntakEtablertTilsynGrunnlagRepository unntakEtablertTilsynGrunnlagRepository, BehandlingRepository behandlingRepository) {
        this.unntakEtablertTilsynGrunnlagRepository = unntakEtablertTilsynGrunnlagRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @SuppressWarnings("unused")
    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        final Long behandlingId = kontekst.getBehandlingId();
        final var behandling = behandlingRepository.hentBehandling(behandlingId);
        final var unntakEtablertTilsynForPleietrengende = unntakEtablertTilsynGrunnlagRepository.hentHvisEksistererUnntakPleietrengende(behandling.getFagsak().getPleietrengendeAktørId());

        final var aksjonspunkter = new ArrayList<AksjonspunktDefinisjon>();
        if (unntakEtablertTilsynForPleietrengende.isPresent()) {
            unntakEtablertTilsynGrunnlagRepository.lagreGrunnlag(behandlingId, unntakEtablertTilsynForPleietrengende.get());
            
            if (søktOmNattevåk(unntakEtablertTilsynForPleietrengende.get())) {
                aksjonspunkter.add(AksjonspunktDefinisjon.VURDER_NATTEVÅK);
            }
            if (søktOmBeredskap(unntakEtablertTilsynForPleietrengende.get())) {
                aksjonspunkter.add(AksjonspunktDefinisjon.VURDER_BEREDSKAP);
            }
            if (aksjonspunkter.isEmpty()) {
                return BehandleStegResultat.utførtUtenAksjonspunkter();
            }
        }
        return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunkter);
    }

    private boolean søktOmNattevåk(UnntakEtablertTilsynForPleietrengende unntakEtablertTilsynForPleietrengende) {
        final var nattevåk = unntakEtablertTilsynForPleietrengende.getNattevåk();
        if (nattevåk != null) {
            return nattevåk.getPerioder().stream().anyMatch(periode -> periode.getResultat().equals(Resultat.IKKE_VURDERT));
        }
        return false;
    }

    private boolean søktOmBeredskap(UnntakEtablertTilsynForPleietrengende unntakEtablertTilsynForPleietrengende) {
        final var beredskap = unntakEtablertTilsynForPleietrengende.getBeredskap();
        if (beredskap != null) {
            return beredskap.getPerioder().stream().anyMatch(periode -> periode.getResultat().equals(Resultat.IKKE_VURDERT));
        }
        return false;
    }

}
