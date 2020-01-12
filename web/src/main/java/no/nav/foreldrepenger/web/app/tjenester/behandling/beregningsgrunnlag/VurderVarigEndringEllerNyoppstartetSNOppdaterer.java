package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;


import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.VurderVarigEndretNyoppstartetSNHåndterer;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.VurderVarigEndringEllerNyoppstartetSNDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.historikk.VurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderVarigEndringEllerNyoppstartetSNDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderVarigEndringEllerNyoppstartetSNOppdaterer implements AksjonspunktOppdaterer<VurderVarigEndringEllerNyoppstartetSNDto> {
    private static final AksjonspunktDefinisjon FASTSETTBRUTTOSNKODE = AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE;

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private VurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste vurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste;
    private VurderVarigEndretNyoppstartetSNHåndterer vurderVarigEndretNyoppstartetSNHåndterer;
    
    VurderVarigEndringEllerNyoppstartetSNOppdaterer() {
        // CDI
    }

    @Inject
    public VurderVarigEndringEllerNyoppstartetSNOppdaterer(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                                           VurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste vurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste,
                                                           VurderVarigEndretNyoppstartetSNHåndterer vurderVarigEndretNyoppstartetSNHåndterer) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.vurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste = vurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste;
        this.vurderVarigEndretNyoppstartetSNHåndterer = vurderVarigEndretNyoppstartetSNHåndterer;
    }

    @Override
    public OppdateringResultat oppdater(VurderVarigEndringEllerNyoppstartetSNDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = param.getBehandling();
        OppdateringResultat.Builder resultatBuilder = OppdateringResultat.utenTransisjon();
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);

        // Aksjonspunkt "opprettet" i GUI må legge til, bør endre på hvordan dette er løst
        if (dto.getErVarigEndretNaering()) {
            if (dto.getBruttoBeregningsgrunnlag() != null) {
                vurderVarigEndretNyoppstartetSNHåndterer.håndter(behandling.getId(), dto);
            } else {
                behandlingskontrollTjeneste.lagreAksjonspunkterFunnet(kontekst, BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG, List.of(FASTSETTBRUTTOSNKODE));
            }
        } else {
            behandling.getÅpentAksjonspunktMedDefinisjonOptional(FASTSETTBRUTTOSNKODE)
            .ifPresent(a -> behandlingskontrollTjeneste.lagreAksjonspunkterAvbrutt(kontekst, BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG, List.of(a)));
        }

        vurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste.lagHistorikkInnslag(param, dto);

        return resultatBuilder.build();
    }
}
