package no.nav.k9.sak.web.app.tjenester.behandling.tilsyn;

import no.nav.k9.sak.kontrakt.tilsyn.*;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.untaketablerttilsyn.UnntakEtablertTilsyn;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.untaketablerttilsyn.UnntakEtablertTilsynBeskrivelse;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.untaketablerttilsyn.UnntakEtablertTilsynGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.untaketablerttilsyn.UnntakEtablertTilsynPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttaksPerioderGrunnlag;

import java.util.List;

public class EtablertTilsynNattevåkOgBeredskapMapper {


    public EtablertTilsynNattevåkOgBeredskapDto tilDto(UttaksPerioderGrunnlag uttaksPerioderGrunnlag,
                                                       UnntakEtablertTilsynGrunnlag unntakEtablertTilsynGrunnlag) {
        //TODO hent ut etablert tilsyn
        var beredskap = unntakEtablertTilsynGrunnlag.getUnntakEtablertTilsynForPleietrengende().getBeredskap();
        var nattevåk = unntakEtablertTilsynGrunnlag.getUnntakEtablertTilsynForPleietrengende().getNattevåk();

        return new EtablertTilsynNattevåkOgBeredskapDto(tilEtablertTilsynPerioder(), tilNattevåk(nattevåk), tilBeredskap(beredskap));
    }

    private List<EtablertTilsynPeriodeDto> tilEtablertTilsynPerioder() {

        //TODO

        return List.of();
    }
    private NattevåkDto tilNattevåk(UnntakEtablertTilsyn nattevåk) {
        return new NattevåkDto(tilBeskrivelser(nattevåk.getBeskrivelser()), tilVurderinger(nattevåk.getPerioder()));
    }
    private BeredskapDto tilBeredskap(UnntakEtablertTilsyn beredskap) {
        return new BeredskapDto(tilBeskrivelser(beredskap.getBeskrivelser()), tilVurderinger(beredskap.getPerioder()));
    }

    private List<BeskrivelseDto> tilBeskrivelser(List<UnntakEtablertTilsynBeskrivelse> uetBeskrivelser) {
        return uetBeskrivelser.stream().map(uetBeskrivelse ->
            new BeskrivelseDto(
                new Periode(uetBeskrivelse.getPeriode().getFomDato(), uetBeskrivelse.getPeriode().getTomDato()),
                uetBeskrivelse.getTekst(),
                uetBeskrivelse.getMottattDato(),
                Kilde.SØKER //TODO utled dette
            )
        ).toList();
    }

    private List<VurderingDto> tilVurderinger(List<UnntakEtablertTilsynPeriode> uetPerioder) {
        return uetPerioder.stream().map(uetPeriode ->
            new VurderingDto(
                uetPeriode.getId(),
                new Periode(uetPeriode.getPeriode().getFomDato(), uetPeriode.getPeriode().getTomDato()),
                uetPeriode.getBegrunnelse(),
                uetPeriode.getResultat(),
                Kilde.SØKER //TODO utled dette
            )
        ).toList();
    }

}
