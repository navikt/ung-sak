package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.FordelBeregningsgrunnlagHåndterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.historikk.FordelBeregningsgrunnlagHistorikkTjeneste;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FordelBeregningsgrunnlagDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = FordelBeregningsgrunnlagDto.class, adapter = AksjonspunktOppdaterer.class)
public class FordelBeregningsgrunnlagOppdaterer implements AksjonspunktOppdaterer<FordelBeregningsgrunnlagDto>  {

    private FordelBeregningsgrunnlagHistorikkTjeneste fordelBeregningsgrunnlagHistorikkTjeneste;
    private FordelBeregningsgrunnlagHåndterer fordelBeregningsgrunnlagHåndterer;


    FordelBeregningsgrunnlagOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public FordelBeregningsgrunnlagOppdaterer(FordelBeregningsgrunnlagHistorikkTjeneste fordelBeregningsgrunnlagHistorikkTjeneste, FordelBeregningsgrunnlagHåndterer fordelBeregningsgrunnlagHåndterer) {
        this.fordelBeregningsgrunnlagHistorikkTjeneste = fordelBeregningsgrunnlagHistorikkTjeneste;
        this.fordelBeregningsgrunnlagHåndterer = fordelBeregningsgrunnlagHåndterer;
    }

    @Override
    public OppdateringResultat oppdater(FordelBeregningsgrunnlagDto dto, AksjonspunktOppdaterParameter param) {
        fordelBeregningsgrunnlagHåndterer.håndter(dto, param.getBehandlingId());
        fordelBeregningsgrunnlagHistorikkTjeneste.lagHistorikk(dto, param);
        return OppdateringResultat.utenOveropp();
    }

}
