package no.nav.folketrygdloven.beregningsgrunnlag.verdikjede;

import no.finn.unleash.FakeUnleash;
import no.nav.folketrygdloven.beregningsgrunnlag.AksjonspunktUtlederFaktaOmBeregning;
import no.nav.folketrygdloven.beregningsgrunnlag.AksjonspunktUtlederForeslåBeregning;
import no.nav.folketrygdloven.beregningsgrunnlag.BeregningsgrunnlagAksjonspunktUtleder;
import no.nav.folketrygdloven.beregningsgrunnlag.FastsettBeregningsgrunnlagPerioderTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.FastsettSkjæringstidspunktOgStatuser;
import no.nav.folketrygdloven.beregningsgrunnlag.FordelBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.ForeslåBeregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.VurderBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.MapBGSkjæringstidspunktOgStatuserFraRegelTilVL;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.MapBeregningsgrunnlagFraRegelTilVL;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.MapFastsettBeregningsgrunnlagPerioderFraRegelTilVLNaturalytelse;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.MapFastsettBeregningsgrunnlagPerioderFraRegelTilVLRefusjonOgGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.MapBGStatuserFraVLTilRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering.MapFastsettBeregningsgrunnlagPerioderFraVLTilRegelNaturalYtelse;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering.MapFastsettBeregningsgrunnlagPerioderFraVLTilRegelRefusjonOgGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.FaktaOmBeregningTilfelleTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.TilfelleUtlederMockTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.refusjon.InntektsmeldingMedRefusjonTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningIAYTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.ytelse.fp.FullføreBeregningsgrunnlagFPImpl;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

class BeregningTjenesteProvider {

    static BeregningTjenesteWrapper provide(UnittestRepositoryRule repoRule, InntektArbeidYtelseTjeneste iayTjeneste) {
        RepositoryProvider repositoryProvider = new RepositoryProvider(repoRule.getEntityManager());

        InntektsmeldingTjeneste inntektsmeldingTjeneste = new InntektsmeldingTjeneste(iayTjeneste);

        MapBeregningsgrunnlagFraRegelTilVL oversetterFraRegel = new MapBeregningsgrunnlagFraRegelTilVL();
        MapBGSkjæringstidspunktOgStatuserFraRegelTilVL oversetterFraSTPRegel = new MapBGSkjæringstidspunktOgStatuserFraRegelTilVL(
            repositoryProvider.getBeregningsgrunnlagRepository()
        );
        var fastsettSkjæringstidspunktOgStatuser = new FastsettSkjæringstidspunktOgStatuser(oversetterFraSTPRegel, new MapBGStatuserFraVLTilRegel());
        FaktaOmBeregningTilfelleTjeneste faktaOmBeregningTilfelleTjeneste = new FaktaOmBeregningTilfelleTjeneste(
            TilfelleUtlederMockTjeneste.getUtlederInstances());
        BeregningsgrunnlagAksjonspunktUtleder aksjonspunktUtlederFaktaOmBeregning = new AksjonspunktUtlederFaktaOmBeregning(faktaOmBeregningTilfelleTjeneste);
        var oversetterTilRegel = LagMapBeregningsgrunnlagFraVLTilRegel.lagMapper(repositoryProvider.getBeregningsgrunnlagRepository(), new FakeUnleash());
        InntektsmeldingMedRefusjonTjeneste inntektsmeldingMedRefusjonTjeneste = new InntektsmeldingMedRefusjonTjeneste(inntektsmeldingTjeneste);
        AksjonspunktUtlederForeslåBeregning aksjonspunktUtleder = new AksjonspunktUtlederForeslåBeregning();
        var foreslåBeregningsgrunnlagTjeneste = new ForeslåBeregningsgrunnlag(oversetterTilRegel, oversetterFraRegel, aksjonspunktUtleder, new FakeUnleash());
        var fullføreBeregningsgrunnlagTjeneste = new FullføreBeregningsgrunnlagFPImpl(oversetterFraRegel, oversetterTilRegel);

        MapFastsettBeregningsgrunnlagPerioderFraVLTilRegelNaturalYtelse oversetterTilRegelNaturalytelse = new MapFastsettBeregningsgrunnlagPerioderFraVLTilRegelNaturalYtelse(inntektsmeldingMedRefusjonTjeneste);
        MapFastsettBeregningsgrunnlagPerioderFraVLTilRegelRefusjonOgGradering oversetterTilRegelRefusjonOgGradering = new MapFastsettBeregningsgrunnlagPerioderFraVLTilRegelRefusjonOgGradering(inntektsmeldingMedRefusjonTjeneste);
        MapFastsettBeregningsgrunnlagPerioderFraRegelTilVLNaturalytelse oversetterFraRegelNaturalytelse = new MapFastsettBeregningsgrunnlagPerioderFraRegelTilVLNaturalytelse();
        MapFastsettBeregningsgrunnlagPerioderFraRegelTilVLRefusjonOgGradering oversetterFraRegelRefusjonOgGradering = new MapFastsettBeregningsgrunnlagPerioderFraRegelTilVLRefusjonOgGradering();
        var fastsettBeregningsgrunnlagPerioderTjeneste = new FastsettBeregningsgrunnlagPerioderTjeneste(oversetterTilRegelNaturalytelse, new UnitTestLookupInstanceImpl<>(oversetterTilRegelRefusjonOgGradering), oversetterFraRegelNaturalytelse, oversetterFraRegelRefusjonOgGradering);
        var fordelBeregningsgrunnlagTjeneste = new FordelBeregningsgrunnlagTjeneste(fastsettBeregningsgrunnlagPerioderTjeneste, oversetterFraRegel, oversetterTilRegel);

        BeregningIAYTestUtil beregningIAYTestUtil = new BeregningIAYTestUtil(iayTjeneste);
        VurderBeregningsgrunnlagTjeneste vurderBeregningsgrunnlagTjeneste = new VurderBeregningsgrunnlagTjeneste(oversetterFraRegel, oversetterTilRegel);
        return new BeregningTjenesteWrapper(foreslåBeregningsgrunnlagTjeneste, fullføreBeregningsgrunnlagTjeneste, fordelBeregningsgrunnlagTjeneste, aksjonspunktUtlederFaktaOmBeregning, fastsettBeregningsgrunnlagPerioderTjeneste, fastsettSkjæringstidspunktOgStatuser, beregningIAYTestUtil, iayTjeneste, inntektsmeldingTjeneste, oversetterTilRegel, vurderBeregningsgrunnlagTjeneste);
    }

}
