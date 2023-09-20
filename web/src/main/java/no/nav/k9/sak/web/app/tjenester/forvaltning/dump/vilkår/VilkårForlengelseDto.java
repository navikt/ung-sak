package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.vilkår;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

class VilkårForlengelseDto {

    private DatoIntervallEntitet periode;
    private VilkårType vilkårType;
    private boolean erTilVurdering;
    private boolean erForlengelse;

    public VilkårForlengelseDto(DatoIntervallEntitet periode, VilkårType vilkårType, boolean erTilVurdering, boolean erForlengelse) {
        this.periode = periode;
        this.vilkårType = vilkårType;
        this.erTilVurdering = erTilVurdering;
        this.erForlengelse = erForlengelse;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public VilkårType getVilkårType() {
        return vilkårType;
    }

    public boolean isErTilVurdering() {
        return erTilVurdering;
    }

    public boolean isErForlengelse() {
        return erForlengelse;
    }
}
