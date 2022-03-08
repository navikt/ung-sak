package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.pleiesihjemmet;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.inngangsvilkår.VilkårUtfallOversetter;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.pleiesihjemmet.regelmodell.PleiesIHjemmetVilkår;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.pleiesihjemmet.regelmodell.PleiesIHjemmetVilkårGrunnlag;

public class PleiesIHjemmetTjeneste {

    private VilkårUtfallOversetter utfallOversetter = new VilkårUtfallOversetter();

    public List<VilkårData> vurderPerioder(VilkårType vilkårType, NavigableSet<DatoIntervallEntitet> perioder) {
        final List<VilkårData> resultat = new ArrayList<>();
        for (var periode : perioder) {
            final var vilkårsGrunnlag = new PleiesIHjemmetVilkårGrunnlag(periode.getFomDato(), periode.getTomDato());

            final var evaluation = new PleiesIHjemmetVilkår().evaluer(vilkårsGrunnlag);

            final var vilkårData = utfallOversetter.oversett(vilkårType, evaluation, vilkårsGrunnlag, periode);
            resultat.add(vilkårData);
        }
        return resultat;
    }
}
