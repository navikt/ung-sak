package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG;
import static no.nav.k9.sak.behandlingskontroll.transisjoner.FellesTransisjoner.FREMHOPP_TIL_FORESLÅ_BEHANDLINGSRESULTAT;

import java.util.Collections;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.output.KalkulusResultat;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@FagsakYtelseTypeRef("*")
@BehandlingStegRef(kode = "FAST_BERGRUNN")
@BehandlingTypeRef
@ApplicationScoped
public class FastsettBeregningsgrunnlagSteg implements BeregningsgrunnlagSteg {

    private BehandlingRepository behandlingRepository;
    private Instance<KalkulusTjeneste> kalkulusTjeneste;
    private BeregningsgrunnlagVilkårTjeneste vilkårTjeneste;
    private BehandletPeriodeTjeneste behandletPeriodeTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    protected FastsettBeregningsgrunnlagSteg() {
        // for CDI proxy
    }

    @Inject
    public FastsettBeregningsgrunnlagSteg(BehandlingRepository behandlingRepository,
                                          @Any Instance<KalkulusTjeneste> kalkulusTjeneste,
                                          BeregningsgrunnlagVilkårTjeneste vilkårTjeneste,
                                          BehandletPeriodeTjeneste behandletPeriodeTjeneste,
                                          SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {

        this.kalkulusTjeneste = kalkulusTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.vilkårTjeneste = vilkårTjeneste;
        this.behandletPeriodeTjeneste = behandletPeriodeTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        KalkulusResultat kalkulusResultat = FagsakYtelseTypeRef.Lookup.find(kalkulusTjeneste, behandling.getFagsakYtelseType())
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke kalkulustjeneste"))
            .fortsettBeregning(BehandlingReferanse.fra(behandling), FASTSETT_BEREGNINGSGRUNNLAG);
        if (kalkulusResultat.getVilkårOppfylt() != null && !kalkulusResultat.getVilkårOppfylt()) {
            var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
            var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);
            var vilkårsPeriode = behandletPeriodeTjeneste.utledPeriode(ref);
            var orginalVilkårsPeriode = behandletPeriodeTjeneste.utledOrginalVilkårsPeriode(ref);
            vilkårTjeneste.lagreAvslåttVilkårresultat(kontekst, vilkårsPeriode, orginalVilkårsPeriode, kalkulusResultat.getAvslagsårsak());
            return BehandleStegResultat.fremoverført(FREMHOPP_TIL_FORESLÅ_BEHANDLINGSRESULTAT);
        }
        return BehandleStegResultat.utførtMedAksjonspunktResultater(Collections.emptyList());
    }

    @Override
    public void vedHoppOverFramover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType fraSteg, BehandlingStegType tilSteg) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        if (tilSteg.equals(BehandlingStegType.SØKNADSFRIST)) {
            if (behandling.erRevurdering()) {
                throw new IllegalStateException("Støtter ikke denne ennå, lag støtte i kalkulus");
            }
        }
    }
}
