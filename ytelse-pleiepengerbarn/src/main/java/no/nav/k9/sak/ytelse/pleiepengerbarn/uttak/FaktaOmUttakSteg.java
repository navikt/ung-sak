package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynGrunnlagRepository;

import java.util.ArrayList;

@ApplicationScoped
@BehandlingStegRef(kode = "KOFAKUT")
@BehandlingTypeRef
@FagsakYtelseTypeRef("PSB")
public class FaktaOmUttakSteg implements BehandlingSteg {

    private UnntakEtablertTilsynGrunnlagRepository unntakEtablertTilsynGrunnlagRepository;

    protected FaktaOmUttakSteg() {
        // for proxy
    }

    @Inject
    public FaktaOmUttakSteg(UnntakEtablertTilsynGrunnlagRepository unntakEtablertTilsynGrunnlagRepository) {
        this.unntakEtablertTilsynGrunnlagRepository = unntakEtablertTilsynGrunnlagRepository;
    }

    @SuppressWarnings("unused")
    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        var unntakEtablerTilsynGrunnlag = unntakEtablertTilsynGrunnlagRepository.hent(behandlingId);

        var aksjonspunkter = new ArrayList<AksjonspunktDefinisjon>();
        if (unntakEtablerTilsynGrunnlag != null) {
            if (søktOmNattevåk(unntakEtablerTilsynGrunnlag)) {
                aksjonspunkter.add(AksjonspunktDefinisjon.VURDER_NATTEVÅK);
            }
            if (søktOmBeredskap(unntakEtablerTilsynGrunnlag)) {
                aksjonspunkter.add(AksjonspunktDefinisjon.VURDER_BEREDSKAP);
            }
            if (aksjonspunkter.isEmpty()) {
                return BehandleStegResultat.utførtUtenAksjonspunkter();
            }
        }
        return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunkter);
    }

    private boolean søktOmNattevåk(UnntakEtablertTilsynGrunnlag unntakEtablerTilsynGrunnlag) {
        var nattevåk = unntakEtablerTilsynGrunnlag.getUnntakEtablertTilsynForPleietrengende().getNattevåk();
        if (nattevåk != null) {
            return nattevåk.getPerioder().stream().anyMatch(periode -> periode.getResultat().equals(Resultat.IKKE_VURDERT));
        }
        return false;
    }

    private boolean søktOmBeredskap(UnntakEtablertTilsynGrunnlag unntakEtablerTilsynGrunnlag) {
        var beredskap = unntakEtablerTilsynGrunnlag.getUnntakEtablertTilsynForPleietrengende().getBeredskap();
        if (beredskap != null) {
            return beredskap.getPerioder().stream().anyMatch(periode -> periode.getResultat().equals(Resultat.IKKE_VURDERT));
        }
        return false;
    }

}
