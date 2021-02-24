package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.inngangsvilkår.VilkårUtfallOversetter;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.MedisinskVilkårResultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.Medisinskvilkår;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.MedisinskvilkårGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;

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

    public VilkårData vurderPerioder(BehandlingskontrollKontekst kontekst, DatoIntervallEntitet periodeTilVurdering, SykdomGrunnlagBehandling sykdomGrunnlagBehandling) {
        MedisinskvilkårGrunnlag grunnlag = inngangsvilkårOversetter.oversettTilRegelModellMedisinsk(kontekst.getBehandlingId(), periodeTilVurdering, sykdomGrunnlagBehandling);
        MedisinskVilkårResultat resultat = new MedisinskVilkårResultat();

        final var evaluation = new Medisinskvilkår().evaluer(grunnlag, resultat);

        final var vilkårData = utfallOversetter.oversett(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR, evaluation, grunnlag, periodeTilVurdering);
        vilkårData.setEkstraVilkårresultat(resultat);
        return vilkårData;
    }
}
