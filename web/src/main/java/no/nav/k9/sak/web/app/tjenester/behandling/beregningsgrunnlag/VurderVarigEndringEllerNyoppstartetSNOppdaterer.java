package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;


import java.time.LocalDate;
import java.util.Map;
import java.util.NavigableSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
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

    private BeregningTjeneste kalkulusTjeneste;
    private BeregningsgrunnlagVilkårTjeneste vilkårTjeneste;

    VurderVarigEndringEllerNyoppstartetSNOppdaterer() {
        // CDI
    }

    @Inject
    public VurderVarigEndringEllerNyoppstartetSNOppdaterer(BeregningTjeneste kalkulusTjeneste,
                                                           BeregningsgrunnlagVilkårTjeneste vilkårTjeneste) {
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.vilkårTjeneste = vilkårTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VurderVarigEndringEllerNyoppstartetSNDtoer dtoer, AksjonspunktOppdaterParameter param) {
        OppdateringResultat.Builder resultatBuilder = OppdateringResultat.utenTransisjon();
        NavigableSet<DatoIntervallEntitet> perioderSomSkalKunneVurderes = vilkårTjeneste.utledPerioderTilVurdering(param.getRef(), false);
        dtoer.getGrunnlag().forEach(dto -> {
            if (!perioderSomSkalKunneVurderes.contains(DatoIntervallEntitet.fra(dto.getPeriode()))) {
                throw new IllegalStateException("Prøver å endre periode " + dto.getPeriode() + " men denne er ikke i" +
                    " listen over vilkårsperioder som er til vurdering " + perioderSomSkalKunneVurderes);
            }
        });
        Map<LocalDate, HåndterBeregningDto> stpTilDtoMap = dtoer.getGrunnlag().stream()
            .collect(Collectors.toMap(dto -> dto.getPeriode().getFom(), MapDtoTilRequest::map));
        kalkulusTjeneste.oppdaterBeregningListe(stpTilDtoMap, param.getRef());
        // TODO FIKS HISTORIKK

        return resultatBuilder.build();
    }
}
