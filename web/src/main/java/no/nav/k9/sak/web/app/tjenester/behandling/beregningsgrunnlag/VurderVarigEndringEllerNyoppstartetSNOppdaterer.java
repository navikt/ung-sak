package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;


import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusTjeneste;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.VurderVarigEndringEllerNyoppstartetSNDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderVarigEndringEllerNyoppstartetSNDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderVarigEndringEllerNyoppstartetSNOppdaterer implements AksjonspunktOppdaterer<VurderVarigEndringEllerNyoppstartetSNDto> {
    private static final AksjonspunktDefinisjon FASTSETTBRUTTOSNKODE = AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE;

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private KalkulusTjeneste kalkulusTjeneste;

    VurderVarigEndringEllerNyoppstartetSNOppdaterer() {
        // CDI
    }

    @Inject
    public VurderVarigEndringEllerNyoppstartetSNOppdaterer(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                                           KalkulusTjeneste kalkulusTjeneste) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.kalkulusTjeneste = kalkulusTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VurderVarigEndringEllerNyoppstartetSNDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = param.getBehandling();
        OppdateringResultat.Builder resultatBuilder = OppdateringResultat.utenTransisjon();
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);

        // Aksjonspunkt "opprettet" i GUI må legge til, bør endre på hvordan dette er løst
        if (dto.getErVarigEndretNaering()) {
            if (dto.getBruttoBeregningsgrunnlag() != null) {
                HåndterBeregningDto håndterBeregningDto = MapDtoTilRequest.map(dto);
                kalkulusTjeneste.oppdaterBeregning(håndterBeregningDto, param.getRef());
            } else {
                behandlingskontrollTjeneste.lagreAksjonspunkterFunnet(kontekst, BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG, List.of(FASTSETTBRUTTOSNKODE));
            }
        } else {
            behandling.getÅpentAksjonspunktMedDefinisjonOptional(FASTSETTBRUTTOSNKODE)
            .ifPresent(a -> behandlingskontrollTjeneste.lagreAksjonspunkterAvbrutt(kontekst, BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG, List.of(a)));
        }
        return resultatBuilder.build();
    }
}
