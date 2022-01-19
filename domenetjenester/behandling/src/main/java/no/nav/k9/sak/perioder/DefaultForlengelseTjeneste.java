package no.nav.k9.sak.perioder;


import java.util.NavigableSet;
import java.util.TreeSet;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

/**
 * Ingen forlengelse
 *
 * Aktuelt for førstegangsbehandling etc.
 */
@FagsakYtelseTypeRef
@BehandlingTypeRef
@ApplicationScoped
public class DefaultForlengelseTjeneste implements ForlengelseTjeneste {

    @Override
    public NavigableSet<DatoIntervallEntitet> utledPerioderSomSkalBehandlesSomForlengelse(BehandlingReferanse referanse, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, VilkårType vilkårType) {
        return new TreeSet<>();
    }
}
