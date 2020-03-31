package no.nav.k9.sak.ytelse.beregning.adapter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatRegelmodell;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultat;

@ApplicationScoped
public class MapBeregningsresultatFraVLTilRegel {

    @Inject
    MapBeregningsresultatFraVLTilRegel() {
    }

    public BeregningsresultatRegelmodell mapFra(Beregningsgrunnlag beregningsgrunnlag, UttakResultat resultat) {

        var regelBeregningsgrunnlag = MapBeregningsgrunnlagFraVLTilRegel.map(beregningsgrunnlag);
        return new BeregningsresultatRegelmodell(regelBeregningsgrunnlag, resultat);
    }

}
