package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk;

import java.util.stream.Collectors;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell.MedisinskvilkårGrunnlag;

public class InngangsvilkårOversetter {

    public MedisinskvilkårGrunnlag oversettTilRegelModellMedisinsk(DatoIntervallEntitet periode, SykdomGrunnlagBehandling sykdomGrunnlagBehandling) {
        final Periode vilkårsperiode = new Periode(periode.getFomDato(), periode.getTomDato());

        var grunnlag = sykdomGrunnlagBehandling.getGrunnlag();
        final var vilkårsGrunnlag = new MedisinskvilkårGrunnlag(periode.getFomDato(), periode.getTomDato());

        final var tidslinjeLivetsSlutt = SykdomUtils.tilTidslinjeForType(grunnlag.getVurderinger(), SykdomVurderingType.LIVETS_SLUTTFASE)
            .filterValue(v -> v.getResultat() == Resultat.OPPFYLT);
        final var relevantLivetsSlutt = tidslinjeLivetsSlutt
            .stream()
            .map(it -> it.getLocalDateInterval())
            .filter(it -> new Periode(it.getFomDato(), it.getTomDato()).overlaps(vilkårsperiode))
            .collect(Collectors.toList());

        vilkårsGrunnlag.medLivetsSluttBehov(relevantLivetsSlutt);

        return vilkårsGrunnlag;
    }

}
