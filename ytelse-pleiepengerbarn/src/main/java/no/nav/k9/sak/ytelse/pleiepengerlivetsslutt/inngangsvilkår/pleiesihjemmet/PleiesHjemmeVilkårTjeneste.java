package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.pleiesihjemmet;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.inngangsvilkår.VilkårUtfallOversetter;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.pleiesihjemmet.regelmodell.PleiePeriode;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.pleiesihjemmet.regelmodell.Pleielokasjon;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.pleiesihjemmet.regelmodell.PleiesHjemmeVilkår;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.pleiesihjemmet.regelmodell.PleiesHjemmeVilkårGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.pleiesihjemmet.regelmodell.PleiesHjemmeVilkårResultat;

public class PleiesHjemmeVilkårTjeneste {

    private VilkårUtfallOversetter utfallOversetter = new VilkårUtfallOversetter();

    PleiesHjemmeVilkårTjeneste() {

    }

    public VilkårData vurderPerioder(DatoIntervallEntitet periodeTilVurdering, SykdomGrunnlagBehandling sykdomGrunnlagBehandling) {
        Periode vilkårsperiode = new Periode(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato());
        List<PleiePeriode> innleggelsesPerioder = mapTilInnleggelsesperioder(sykdomGrunnlagBehandling, vilkårsperiode);

        PleiesHjemmeVilkårGrunnlag grunnlag = new PleiesHjemmeVilkårGrunnlag(vilkårsperiode, innleggelsesPerioder);
        PleiesHjemmeVilkårResultat vilkårResultat = new PleiesHjemmeVilkårResultat();

        final var evaluation = new PleiesHjemmeVilkår().evaluer(grunnlag, vilkårResultat);

        final var vilkårData = utfallOversetter.oversett(VilkårType.PLEIES_I_HJEMMMET, evaluation, grunnlag, periodeTilVurdering);
        vilkårData.setEkstraVilkårresultat(vilkårResultat);
        return vilkårData;
    }

    @NotNull
    private List<PleiePeriode> mapTilInnleggelsesperioder(SykdomGrunnlagBehandling sykdomGrunnlagBehandling, Periode vilkårsperiode) {
        var innleggelser = sykdomGrunnlagBehandling.getGrunnlag().getInnleggelser();
        if (innleggelser == null) {
            return List.of();
        }
        return innleggelser
            .getPerioder()
            .stream()
            .map(sip -> new Periode(sip.getFom(), sip.getTom()))
            .filter(p -> p.overlaps(vilkårsperiode))
            .map(sip -> new PleiePeriode(sip.getFom(), sip.getTom(), Pleielokasjon.INNLAGT))
            .toList();
    }
}
