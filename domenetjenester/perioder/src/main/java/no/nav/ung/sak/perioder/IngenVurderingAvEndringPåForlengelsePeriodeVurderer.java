package no.nav.ung.sak.perioder;

import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingskontroll.VilkårTypeRef;
import no.nav.ung.sak.tid.DatoIntervallEntitet;

@FagsakYtelseTypeRef
@VilkårTypeRef
public class IngenVurderingAvEndringPåForlengelsePeriodeVurderer implements EndringPåForlengelsePeriodeVurderer{

    @Override
    public boolean harPeriodeEndring(EndringPåForlengelseInput input, DatoIntervallEntitet periode) {
        return false;
    }
}
