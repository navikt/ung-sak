package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Instance;

import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;

public class FaktaOmBeregningTilfelleDtoTjenesteProviderMock {

    public static Instance<FaktaOmBeregningTilfelleDtoTjeneste> getTjenesteInstances(RepositoryProvider repositoryProvider) {
        @SuppressWarnings("unchecked")
        Instance<FaktaOmBeregningTilfelleDtoTjeneste> tjenesteInstances = mock(Instance.class);
        List<FaktaOmBeregningTilfelleDtoTjeneste> tjenester = new ArrayList<>();
        BeregningsgrunnlagRepository beregningsgrunnlagRepository = repositoryProvider.getBeregningsgrunnlagRepository();
        BeregningsgrunnlagDtoUtil dtoUtil = new BeregningsgrunnlagDtoUtil(beregningsgrunnlagRepository, null);
        FaktaOmBeregningAndelDtoTjeneste faktaOmBeregningAndelDtoTjeneste = new FaktaOmBeregningAndelDtoTjeneste(dtoUtil);

        leggTilKunYtelseDtoTjeneste(beregningsgrunnlagRepository, tjenester, dtoUtil);
        leggTilKortvarigArbeidsforholdDtoTjeneste(tjenester, dtoUtil);
        leggTilVurderBesteberegningDtoTjeneste(tjenester);
        leggTilVurderLønnsendringDtoTjeneste(tjenester, faktaOmBeregningAndelDtoTjeneste);
        leggTilVurderATFLISammeOrgDtoTjeneste(tjenester, faktaOmBeregningAndelDtoTjeneste);
        leggTilNyoppstartetFLDtoTjeneste(tjenester, faktaOmBeregningAndelDtoTjeneste);
        leggTilVurderMottarYtelseDtoTjeneste(tjenester, dtoUtil, faktaOmBeregningAndelDtoTjeneste);
        when(tjenesteInstances.iterator()).thenReturn(tjenester.iterator());
        when(tjenesteInstances.stream()).thenReturn(tjenester.stream());
        return tjenesteInstances;
    }

    private static void leggTilVurderMottarYtelseDtoTjeneste(List<FaktaOmBeregningTilfelleDtoTjeneste> tjenester, BeregningsgrunnlagDtoUtil dtoUtil, FaktaOmBeregningAndelDtoTjeneste faktaOmBeregningAndelDtoTjeneste) {
        tjenester.add(new VurderMottarYtelseDtoTjeneste(dtoUtil, faktaOmBeregningAndelDtoTjeneste));
    }


    private static void leggTilNyoppstartetFLDtoTjeneste(List<FaktaOmBeregningTilfelleDtoTjeneste> tjenester, FaktaOmBeregningAndelDtoTjeneste faktaOmBeregningAndelDtoTjeneste) {
        tjenester.add(new NyOppstartetFLDtoTjeneste(faktaOmBeregningAndelDtoTjeneste));
    }

    private static void leggTilVurderATFLISammeOrgDtoTjeneste(List<FaktaOmBeregningTilfelleDtoTjeneste> tjenester, FaktaOmBeregningAndelDtoTjeneste faktaOmBeregningAndelDtoTjeneste) {
        tjenester.add(new VurderATFLISammeOrgDtoTjeneste(faktaOmBeregningAndelDtoTjeneste));
    }

    private static void leggTilVurderBesteberegningDtoTjeneste(List<FaktaOmBeregningTilfelleDtoTjeneste> tjenester) {
        tjenester.add(new VurderBesteberegningTilfelleDtoTjeneste());
    }

    private static void leggTilVurderLønnsendringDtoTjeneste(List<FaktaOmBeregningTilfelleDtoTjeneste> tjenester, FaktaOmBeregningAndelDtoTjeneste faktaOmBeregningAndelDtoTjeneste) {
        tjenester.add(new VurderLønnsendringDtoTjeneste(faktaOmBeregningAndelDtoTjeneste));
    }

    private static void leggTilKortvarigArbeidsforholdDtoTjeneste(List<FaktaOmBeregningTilfelleDtoTjeneste> tjenester,
                                                                  BeregningsgrunnlagDtoUtil dtoUtil) {
        tjenester.add(new KortvarigeArbeidsforholdDtoTjeneste(dtoUtil));
    }

    private static void leggTilKunYtelseDtoTjeneste(BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                                    List<FaktaOmBeregningTilfelleDtoTjeneste> tjenester,
                                                    BeregningsgrunnlagDtoUtil dtoUtil) {
        tjenester.add(new KunYtelseDtoTjeneste(beregningsgrunnlagRepository, dtoUtil));
    }
}
