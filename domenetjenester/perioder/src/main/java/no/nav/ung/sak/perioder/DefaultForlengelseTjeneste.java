package no.nav.ung.sak.perioder;


import java.util.NavigableSet;
import java.util.TreeSet;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

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
