package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell.MedisinskVilkårGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell.PleiePeriode;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell.Pleielokasjon;

public class InngangsvilkårOversetter {

    public MedisinskVilkårGrunnlag oversettTilRegelModellMedisinsk(DatoIntervallEntitet periode, SykdomGrunnlagBehandling sykdomGrunnlagBehandling) {
        final Periode vilkårsperiode = new Periode(periode.getFomDato(), periode.getTomDato());

        var grunnlag = sykdomGrunnlagBehandling.getGrunnlag();
        final var vilkårsGrunnlag = new MedisinskVilkårGrunnlag(periode.getFomDato(), periode.getTomDato());

        List<LocalDateInterval> relevantLivetsSlutt = mapRelevantePerioderLivetsSlytt(vilkårsperiode, grunnlag);
        List<PleiePeriode> innleggelsesPerioder = mapRelevanteInnleggelsesperioder(sykdomGrunnlagBehandling, vilkårsperiode);

        vilkårsGrunnlag
            .medLivetsSluttBehov(relevantLivetsSlutt)
            .medInnleggelsesPerioder(innleggelsesPerioder);

        return vilkårsGrunnlag;
    }

    private List<LocalDateInterval> mapRelevantePerioderLivetsSlytt(Periode vilkårsperiode, SykdomGrunnlag grunnlag) {
        final var tidslinjeLivetsSlutt = SykdomUtils.tilTidslinjeForType(grunnlag.getVurderinger(), SykdomVurderingType.LIVETS_SLUTTFASE)
            .filterValue(v -> v.getResultat() == Resultat.OPPFYLT);
        final var relevantLivetsSlutt = tidslinjeLivetsSlutt
            .stream()
            .map(it -> it.getLocalDateInterval())
            .filter(it -> new Periode(it.getFomDato(), it.getTomDato()).overlaps(vilkårsperiode))
            .collect(Collectors.toList());
        return relevantLivetsSlutt;
    }

    private List<PleiePeriode> mapRelevanteInnleggelsesperioder(SykdomGrunnlagBehandling sykdomGrunnlagBehandling, Periode vilkårsperiode) {
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
