package no.nav.k9.sak.web.app.tjenester.behandling.tilsyn;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.kontrakt.tilsyn.BeredskapDto;
import no.nav.k9.sak.kontrakt.tilsyn.BeskrivelseDto;
import no.nav.k9.sak.kontrakt.tilsyn.EtablertTilsynNattevåkOgBeredskapDto;
import no.nav.k9.sak.kontrakt.tilsyn.EtablertTilsynPeriodeDto;
import no.nav.k9.sak.kontrakt.tilsyn.Kilde;
import no.nav.k9.sak.kontrakt.tilsyn.NattevåkDto;
import no.nav.k9.sak.kontrakt.tilsyn.VurderingDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.EtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.delt.UtledetEtablertTilsyn;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsyn;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynBeskrivelse;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynForPleietrengende;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynPeriode;

@Dependent
public class EtablertTilsynNattevåkOgBeredskapMapper {

    private EtablertTilsynTjeneste etablertTilsynTjeneste;

    @Inject
    public EtablertTilsynNattevåkOgBeredskapMapper(EtablertTilsynTjeneste etablertTilsynTjeneste) {
        this.etablertTilsynTjeneste = etablertTilsynTjeneste;
    }

    public EtablertTilsynNattevåkOgBeredskapDto tilDto(BehandlingReferanse behandlingRef,
            Optional<UnntakEtablertTilsynForPleietrengende> unntakEtablertTilsynForPleietrengende) {
        var beredskap = unntakEtablertTilsynForPleietrengende.map(UnntakEtablertTilsynForPleietrengende::getBeredskap);
        var nattevåk = unntakEtablertTilsynForPleietrengende.map(UnntakEtablertTilsynForPleietrengende::getNattevåk);

        return new EtablertTilsynNattevåkOgBeredskapDto(
            tilEtablertTilsyn(behandlingRef),
            tilNattevåk(nattevåk, behandlingRef.getAktørId()),
            tilBeredskap(beredskap, behandlingRef.getAktørId()),
            unntakEtablertTilsynForPleietrengende.get().getOpprettetAv(),
            unntakEtablertTilsynForPleietrengende.get().getOpprettetTidspunkt());
    }

    private List<EtablertTilsynPeriodeDto> tilEtablertTilsyn(BehandlingReferanse behandlingRef) {
        final LocalDateTimeline<UtledetEtablertTilsyn> etablertTilsyntidslinje = etablertTilsynTjeneste.beregnTilsynstidlinje(behandlingRef);
        return etablertTilsyntidslinje.stream().map(entry -> new EtablertTilsynPeriodeDto(new Periode(entry.getFom(), entry.getTom()), entry.getValue().getVarighet(), entry.getValue().getKilde())).toList();
    }

    private NattevåkDto tilNattevåk(Optional<UnntakEtablertTilsyn> nattevåk, AktørId søkersAktørId) {
        if (nattevåk.isEmpty()) {
            return new NattevåkDto(List.of(), List.of());
        }
        return new NattevåkDto(tilBeskrivelser(nattevåk.get().getBeskrivelser(), søkersAktørId), tilVurderinger(nattevåk.get().getPerioder(), søkersAktørId));
    }

    private BeredskapDto tilBeredskap(Optional<UnntakEtablertTilsyn> beredskap, AktørId søkersAktørId) {
        if (beredskap.isEmpty()) {
            return new BeredskapDto(List.of(), List.of());
        }
        return new BeredskapDto(tilBeskrivelser(beredskap.get().getBeskrivelser(), søkersAktørId), tilVurderinger(beredskap.get().getPerioder(), søkersAktørId));
    }

    private List<BeskrivelseDto> tilBeskrivelser(List<UnntakEtablertTilsynBeskrivelse> uetBeskrivelser, AktørId søkersAktørId) {
        return uetBeskrivelser.stream().map(uetBeskrivelse ->
            new BeskrivelseDto(
                new Periode(uetBeskrivelse.getPeriode().getFomDato(), uetBeskrivelse.getPeriode().getTomDato()),
                uetBeskrivelse.getTekst(),
                uetBeskrivelse.getMottattDato(),
                tilKilde(søkersAktørId, uetBeskrivelse.getSøker())
            )
        ).toList();
    }

    private List<VurderingDto> tilVurderinger(List<UnntakEtablertTilsynPeriode> uetPerioder, AktørId søkersAktørId) {
        return uetPerioder.stream().map(uetPeriode ->
            new VurderingDto(
                uetPeriode.getId(),
                new Periode(uetPeriode.getPeriode().getFomDato(), uetPeriode.getPeriode().getTomDato()),
                uetPeriode.getBegrunnelse(),
                uetPeriode.getResultat(),
                tilKilde(søkersAktørId, uetPeriode.getAktørId()),
                uetPeriode.getOpprettetAv(),
                uetPeriode.getOpprettetTidspunkt()
            )
        ).toList();
    }

    private Kilde tilKilde(AktørId periodeAktørId, AktørId søkersAktørId) {
        if (Objects.equals(periodeAktørId, søkersAktørId)) {
            return Kilde.SØKER;
        }
        return Kilde.ANDRE;
    }

}
