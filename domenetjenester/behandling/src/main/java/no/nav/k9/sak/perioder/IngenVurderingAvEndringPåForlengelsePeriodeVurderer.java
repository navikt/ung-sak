package no.nav.k9.sak.perioder;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.VilkårTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@FagsakYtelseTypeRef
@VilkårTypeRef
public class IngenVurderingAvEndringPåForlengelsePeriodeVurderer implements EndringPåForlengelsePeriodeVurderer{

    @Override
    public boolean harPeriodeEndring(EndringPåForlengelseInput input, DatoIntervallEntitet periode) {
        return false;
    }
}
