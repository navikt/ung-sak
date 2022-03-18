package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;


import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.resultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.VurderVarigEndringEllerNyoppstartetSNDtoer;
import no.nav.k9.sak.web.app.tjenester.behandling.historikk.beregning.VurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderVarigEndringEllerNyoppstartetSNDtoer.class, adapter = AksjonspunktOppdaterer.class)
public class VurderVarigEndringEllerNyoppstartetSNOppdaterer implements AksjonspunktOppdaterer<VurderVarigEndringEllerNyoppstartetSNDtoer> {

    private BeregningsgrunnlagOppdateringTjeneste oppdateringTjeneste;
    private VurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste historikkTjeneste;

    VurderVarigEndringEllerNyoppstartetSNOppdaterer() {
        // CDI
    }

    @Inject
    public VurderVarigEndringEllerNyoppstartetSNOppdaterer(BeregningsgrunnlagOppdateringTjeneste oppdateringTjeneste,
                                                           VurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste historikkTjeneste) {
        this.oppdateringTjeneste = oppdateringTjeneste;
        this.historikkTjeneste = historikkTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VurderVarigEndringEllerNyoppstartetSNDtoer dtoer, AksjonspunktOppdaterParameter param) {
        OppdateringResultat.Builder resultatBuilder = OppdateringResultat.utenTransisjon();
        Map<LocalDate, HåndterBeregningDto> stpTilDtoMap = dtoer.getGrunnlag().stream()
            .collect(Collectors.toMap(dto -> dto.getPeriode().getFom(), dto1 -> MapDtoTilRequest.map(dto1, dtoer.getBegrunnelse())));
        List<OppdaterBeregningsgrunnlagResultat> utførteOppdateringer = oppdateringTjeneste.oppdaterBeregning(stpTilDtoMap, param.getRef(), false);
        historikkTjeneste.lagHistorikkInnslag(param, dtoer, utførteOppdateringer);
        return resultatBuilder.build();
    }
}
