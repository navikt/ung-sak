package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.omsorgenfor;

import java.time.LocalDate;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.inngangsvilkår.VilkårUtfallOversetter;
import no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.omsorgenfor.regelmodell.OmsorgenForGrunnlag;
import no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.omsorgenfor.regelmodell.OmsorgenForVilkår;

@ApplicationScoped
public class OmsorgenForTjeneste {

    private VilkårUtfallOversetter utfallOversetter;

    @Inject
    OmsorgenForTjeneste() {
        this.utfallOversetter = new VilkårUtfallOversetter();
    }

    public VilkårData vurderPerioder(BehandlingskontrollKontekst kontekst, Set<DatoIntervallEntitet> perioderTilVurdering) {
        var startDato = perioderTilVurdering.stream().map(DatoIntervallEntitet::getFomDato).min(LocalDate::compareTo).orElse(LocalDate.now());
        var sluttDato = perioderTilVurdering.stream().map(DatoIntervallEntitet::getTomDato).max(LocalDate::compareTo).orElse(LocalDate.now());

        final var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(startDato, sluttDato);

        OmsorgenForGrunnlag grunnlag = null; // TODO K9
        final var evaluation = new OmsorgenForVilkår().evaluer(grunnlag);

        return utfallOversetter.oversett(VilkårType.OMSORGEN_FOR, evaluation, grunnlag, periodeTilVurdering);
    }
}
