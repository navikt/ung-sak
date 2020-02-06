package no.nav.foreldrepenger.inngangsvilkaar.medisinsk;

import java.time.LocalDate;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.impl.InngangsvilkårOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.impl.VilkårUtfallOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medisinsk.MedisinskVilkårResultat;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medisinsk.Medisinskvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medisinsk.MedisinskvilkårGrunnlag;
import no.nav.k9.kodeverk.vilkår.VilkårType;

@ApplicationScoped
public class MedisinskVilkårTjeneste {

    private InngangsvilkårOversetter inngangsvilkårOversetter;
    private VilkårUtfallOversetter utfallOversetter;

    MedisinskVilkårTjeneste() {
        // CDI
    }

    @Inject
    MedisinskVilkårTjeneste(InngangsvilkårOversetter inngangsvilkårOversetter) {
        this.inngangsvilkårOversetter = inngangsvilkårOversetter;
        this.utfallOversetter = new VilkårUtfallOversetter();
    }

    public VilkårData vurderPerioder(BehandlingskontrollKontekst kontekst, Set<DatoIntervallEntitet> perioderTilVurdering) {
        var startDato = perioderTilVurdering.stream().map(DatoIntervallEntitet::getFomDato).min(LocalDate::compareTo).orElse(LocalDate.now());
        var sluttDato = perioderTilVurdering.stream().map(DatoIntervallEntitet::getTomDato).max(LocalDate::compareTo).orElse(LocalDate.now());

        final var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(startDato, sluttDato);
        MedisinskvilkårGrunnlag grunnlag = inngangsvilkårOversetter.oversettTilRegelModellMedisinsk(kontekst.getBehandlingId(), periodeTilVurdering);
        MedisinskVilkårResultat resultat = new MedisinskVilkårResultat();

        final var evaluation = new Medisinskvilkår().evaluer(grunnlag, resultat);

        final var vilkårData = utfallOversetter.oversett(VilkårType.MEDISINSKEVILKÅR, evaluation, grunnlag, periodeTilVurdering);
        vilkårData.setEkstraVilkårresultat(resultat);
        return vilkårData;
    }
}
