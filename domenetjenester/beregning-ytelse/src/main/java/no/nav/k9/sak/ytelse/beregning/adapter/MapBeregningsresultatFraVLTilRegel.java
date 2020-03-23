package no.nav.k9.sak.ytelse.beregning.adapter;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.ytelse.beregning.UttakResultatInput;
import no.nav.k9.sak.ytelse.beregning.UttakResultatMapper;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatRegelmodell;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultat;

@ApplicationScoped
public class MapBeregningsresultatFraVLTilRegel {

    private Instance<UttakResultatMapper> uttakResultatRepoMapper;

    MapBeregningsresultatFraVLTilRegel() {
        // for CDI
    }

    @Inject
    MapBeregningsresultatFraVLTilRegel(@Any Instance<UttakResultatMapper> uttakResultatRepoMapper) {
        this.uttakResultatRepoMapper = uttakResultatRepoMapper;
    }

    public BeregningsresultatRegelmodell mapFra(Beregningsgrunnlag beregningsgrunnlag,
                                                UttakResultatInput input) {

        var mapper = FagsakYtelseTypeRef.Lookup.find(this.uttakResultatRepoMapper, input.getYtelseType()).orElseThrow();
        var regelBeregningsgrunnlag = MapBeregningsgrunnlagFraVLTilRegel.map(beregningsgrunnlag);
        UttakResultat regelUttakResultat = mapper.hentOgMapUttakResultat(input);
        return new BeregningsresultatRegelmodell(regelBeregningsgrunnlag, regelUttakResultat);
    }

}
