package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.opplæringspenger.ReisetidDto;
import no.nav.k9.sak.kontrakt.opplæringspenger.VurderReisetidDto;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertReisetid;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertReisetidHolder;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertReisetidPeriode;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderReisetidDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderReisetidOppdaterer implements AksjonspunktOppdaterer<VurderReisetidDto> {

    private VurdertOpplæringRepository vurdertOpplæringRepository;

    VurderReisetidOppdaterer() {
    }

    @Inject
    public VurderReisetidOppdaterer(VurdertOpplæringRepository vurdertOpplæringRepository) {
        this.vurdertOpplæringRepository = vurdertOpplæringRepository;
    }

    @Override
    public OppdateringResultat oppdater(VurderReisetidDto dto, AksjonspunktOppdaterParameter param) {
        List<VurdertReisetid> vurdertReisetid = mapDtoTilVurdertReisetid(dto);

        //TODO: sjekke at opplæringsperiode passer med en periode fra søknad?

        var aktivHolder = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(param.getBehandlingId())
            .map(VurdertOpplæringGrunnlag::getVurdertReisetid);

        if (aktivHolder.isPresent()) {
            LocalDateTimeline<VurdertReisetid> vurdertOpplæringTidslinje = utledKombinertTidslinje(aktivHolder.get().getReisetid(), vurdertReisetid);

            vurdertReisetid = vurdertOpplæringTidslinje
                .stream()
                .map(datoSegment -> new VurdertReisetid(datoSegment.getValue()))
                .toList();
        }

        sjekkOverlappendePerioder(vurdertReisetid);

        var nyHolder = new VurdertReisetidHolder(vurdertReisetid);

        vurdertOpplæringRepository.lagre(param.getBehandlingId(), nyHolder);
        return OppdateringResultat.nyttResultat();
    }

    private List<VurdertReisetid> mapDtoTilVurdertReisetid(VurderReisetidDto dto) {
        List<VurdertReisetid> vurdertReisetid = dto.getReisetid()
            .stream()
            .map(reisetidDto -> new VurdertReisetid(DatoIntervallEntitet.fra(reisetidDto.getOpplæringPeriode()),
                mapTilVurdertReisetidPerioder(reisetidDto),
                reisetidDto.getBegrunnelse()))
            .toList();
        sjekkOverlappendePerioder(vurdertReisetid);
        return vurdertReisetid;
    }

    private List<VurdertReisetidPeriode> mapTilVurdertReisetidPerioder(ReisetidDto dto) {
        List<VurdertReisetidPeriode> perioder = new ArrayList<>();

        perioder.addAll(dto.getReisetidTil().stream()
            .map(periodeDto -> new VurdertReisetidPeriode(DatoIntervallEntitet.fra(periodeDto.getPeriode()), periodeDto.isGodkjent()))
            .toList());
        perioder.addAll(dto.getReisetidHjem().stream()
            .map(periodeDto -> new VurdertReisetidPeriode(DatoIntervallEntitet.fra(periodeDto.getPeriode()), periodeDto.isGodkjent()))
            .toList());

        return perioder;
    }

    private void sjekkOverlappendePerioder(List<VurdertReisetid> vurdertReisetid) {
        List<DatoIntervallEntitet> perioder = new ArrayList<>();

        for (Set<VurdertReisetidPeriode> reisetidPerioder : vurdertReisetid.stream().map(VurdertReisetid::getReiseperioder).toList()) {
            perioder.addAll(reisetidPerioder.stream().map(VurdertReisetidPeriode::getPeriode).toList());
        }

        perioder.addAll(vurdertReisetid.stream().map(VurdertReisetid::getOpplæringperiode).toList());

        new LocalDateTimeline<>(perioder.stream().map(periode -> new LocalDateSegment<>(periode.getFomDato(), periode.getTomDato(), true)).toList());
    }

    private LocalDateTimeline<VurdertReisetid> utledKombinertTidslinje(List<VurdertReisetid> eksisterende,
                                                                       List<VurdertReisetid> ny) {
        LocalDateTimeline<VurdertReisetid> eksisterendeTidslinje = toTidslinje(eksisterende);
        LocalDateTimeline<VurdertReisetid> nyTidslinje = toTidslinje(ny);

        return eksisterendeTidslinje.combine(nyTidslinje, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN).compress();
    }

    private LocalDateTimeline<VurdertReisetid> toTidslinje(List<VurdertReisetid> perioder) {
        final var segments = perioder
            .stream()
            .map(vurdertReisetid -> new LocalDateSegment<>(
                vurdertReisetid.getOpplæringperiode().getFomDato(),
                vurdertReisetid.getOpplæringperiode().getTomDato(),
                vurdertReisetid))
            .collect(Collectors.toList());
        return new LocalDateTimeline<>(segments);
    }
}
