package no.nav.k9.sak.inngangsvilkår.medlemskap;

import java.time.LocalDate;
import java.util.Map;
import java.util.NavigableSet;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;

public class VurdertMedlemskapOgForlengelser {
    private Map<LocalDate, VilkårData> vurderinger;
    private NavigableSet<DatoIntervallEntitet> forlengelser;

    public VurdertMedlemskapOgForlengelser(Map<LocalDate, VilkårData> vurderinger, NavigableSet<DatoIntervallEntitet> forlengelser) {
        this.vurderinger = vurderinger;
        this.forlengelser = forlengelser;
    }

    public Map<LocalDate, VilkårData> getVurderinger() {
        return vurderinger;
    }

    public NavigableSet<DatoIntervallEntitet> getForlengelser() {
        return forlengelser;
    }
}
