package no.nav.k9.sak.ytelse.beregning.adapter;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatRegelmodell;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultat;

@Dependent
public class MapBeregningsresultatFraVLTilRegel {

    @Inject
    MapBeregningsresultatFraVLTilRegel() {
    }

    public BeregningsresultatRegelmodell mapFra(Beregningsgrunnlag beregningsgrunnlag, UttakResultat resultat) {

        var regelBeregningsgrunnlag = MapBeregningsgrunnlagFraVLTilRegel.map(beregningsgrunnlag);
        return new BeregningsresultatRegelmodell(regelBeregningsgrunnlag, resultat);
    }

    public BeregningsresultatRegelmodell mapFra(List<Beregningsgrunnlag> beregningsgrunnlag, UttakResultat resultat) {

        var regelBeregningsgrunnlag = beregningsgrunnlag.stream().map(MapBeregningsgrunnlagFraVLTilRegel::map).collect(Collectors.toList());
        return new BeregningsresultatRegelmodell(regelBeregningsgrunnlag, resultat);
    }

}
