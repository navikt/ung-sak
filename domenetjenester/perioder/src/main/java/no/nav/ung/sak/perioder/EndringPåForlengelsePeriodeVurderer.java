package no.nav.ung.sak.perioder;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingskontroll.VilkårTypeRef;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

public interface EndringPåForlengelsePeriodeVurderer {

    public static EndringPåForlengelsePeriodeVurderer finnVurderer(Instance<EndringPåForlengelsePeriodeVurderer> instances, VilkårType vilkårType, FagsakYtelseType fagsakYtelseType) {
        Instance<EndringPåForlengelsePeriodeVurderer> selected = instances.select(new VilkårTypeRef.VilkårTypeRefLiteral(vilkårType));
        if (selected.isAmbiguous()) {
            return FagsakYtelseTypeRef.Lookup.find(selected, fagsakYtelseType).orElseThrow(() -> new IllegalStateException("Har ikke EndringPåForlengelsePeriodeVurderer for " + fagsakYtelseType));
        } else if (selected.isUnsatisfied()) {
            throw new IllegalArgumentException("Ingen implementasjoner funnet for vilkårtype:" + vilkårType);
        }

        EndringPåForlengelsePeriodeVurderer minInstans = selected.get();
        if (minInstans.getClass().isAnnotationPresent(Dependent.class)) {
            throw new IllegalStateException(
                "Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + minInstans.getClass());
        }
        return minInstans;
    }

    public boolean harPeriodeEndring(EndringPåForlengelseInput input, DatoIntervallEntitet periode);
}
