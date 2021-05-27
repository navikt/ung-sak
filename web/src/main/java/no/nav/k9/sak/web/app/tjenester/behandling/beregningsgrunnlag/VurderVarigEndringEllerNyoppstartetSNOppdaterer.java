package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;


import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.VurderVarigEndringEllerNyoppstartetSNDtoer;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderVarigEndringEllerNyoppstartetSNDtoer.class, adapter = AksjonspunktOppdaterer.class)
public class VurderVarigEndringEllerNyoppstartetSNOppdaterer implements AksjonspunktOppdaterer<VurderVarigEndringEllerNyoppstartetSNDtoer> {

    private BeregningsgrunnlagOppdateringTjeneste oppdateringTjeneste;

    VurderVarigEndringEllerNyoppstartetSNOppdaterer() {
        // CDI
    }

    @Inject
    public VurderVarigEndringEllerNyoppstartetSNOppdaterer(BeregningsgrunnlagOppdateringTjeneste oppdateringTjeneste) {
        this.oppdateringTjeneste = oppdateringTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VurderVarigEndringEllerNyoppstartetSNDtoer dtoer, AksjonspunktOppdaterParameter param) {
        OppdateringResultat.Builder resultatBuilder = OppdateringResultat.utenTransisjon();
        Map<LocalDate, HåndterBeregningDto> stpTilDtoMap = dtoer.getGrunnlag().stream()
            .collect(Collectors.toMap(dto -> dto.getPeriode().getFom(), MapDtoTilRequest::map));
        List<OppdaterBeregningsgrunnlagResultat> utførteOppdateringer = oppdateringTjeneste.oppdaterBeregning(stpTilDtoMap, param.getRef());
        // TODO FIKS HISTORIKK

        return resultatBuilder.build();
    }
}
