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
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.omsorgenfor.OmsorgenForGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.omsorgenfor.OmsorgenForVilkår;
import no.nav.k9.kodeverk.vilkår.VilkårType;

@ApplicationScoped
public class OmsorgenForTjeneste {

    private InngangsvilkårOversetter inngangsvilkårOversetter;
    private VilkårUtfallOversetter utfallOversetter;

    OmsorgenForTjeneste() {
        // CDI
    }

    @Inject
    OmsorgenForTjeneste(InngangsvilkårOversetter inngangsvilkårOversetter) {
        this.inngangsvilkårOversetter = inngangsvilkårOversetter;
        this.utfallOversetter = new VilkårUtfallOversetter();
    }

    public VilkårData vurderPerioder(BehandlingskontrollKontekst kontekst, Set<DatoIntervallEntitet> perioderTilVurdering) {
        var startDato = perioderTilVurdering.stream().map(DatoIntervallEntitet::getFomDato).min(LocalDate::compareTo).orElse(LocalDate.now());
        var sluttDato = perioderTilVurdering.stream().map(DatoIntervallEntitet::getTomDato).max(LocalDate::compareTo).orElse(LocalDate.now());

        final var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(startDato, sluttDato);
        OmsorgenForGrunnlag grunnlag = inngangsvilkårOversetter.oversettTilRegelModellOmsorgen(kontekst.getBehandlingId(), kontekst.getAktørId(), periodeTilVurdering);

        final var evaluation = new OmsorgenForVilkår().evaluer(grunnlag);

        return utfallOversetter.oversett(VilkårType.OMSORGEN_FOR, evaluation, grunnlag, periodeTilVurdering);
    }
}
