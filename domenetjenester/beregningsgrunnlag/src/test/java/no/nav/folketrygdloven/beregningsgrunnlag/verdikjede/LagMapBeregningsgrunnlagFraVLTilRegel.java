package no.nav.folketrygdloven.beregningsgrunnlag.verdikjede;

import no.finn.unleash.Unleash;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.GrunnbeløpTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.MapBeregningsgrunnlagFraVLTilRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.MapInntektsgrunnlagVLTilRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.fp.GrunnbeløpTjenesteImpl;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

public class LagMapBeregningsgrunnlagFraVLTilRegel {

    //brukes bare i test...
    public static MapBeregningsgrunnlagFraVLTilRegel lagMapper(BeregningsgrunnlagRepository beregningsgrunnlagRepository, Unleash unleash) {
        MapInntektsgrunnlagVLTilRegel mapInntektsgrunnlagVLTilRegel = new MapInntektsgrunnlagVLTilRegel(5, unleash);
        GrunnbeløpTjeneste grunnbeløpTjeneste = new GrunnbeløpTjenesteImpl(beregningsgrunnlagRepository, 3);

        return new MapBeregningsgrunnlagFraVLTilRegel(new UnitTestLookupInstanceImpl<>(grunnbeløpTjeneste), mapInntektsgrunnlagVLTilRegel
        );
    }
}
