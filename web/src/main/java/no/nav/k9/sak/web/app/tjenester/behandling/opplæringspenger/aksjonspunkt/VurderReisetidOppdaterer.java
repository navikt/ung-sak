package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.aksjonspunkt;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
import no.nav.k9.sak.kontrakt.opplæringspenger.VurderReisetidDto;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertReisetid;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertReisetidHolder;
import no.nav.k9.sikkerhet.context.SubjectHandler;

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
        LocalDateTime vurdertTidspunkt = LocalDateTime.now();
        List<VurdertReisetid> vurdertReisetid = dto.getReisetid()
            .stream()
            .map(reisetidDto -> new VurdertReisetid(
                DatoIntervallEntitet.fra(reisetidDto.getPeriode()),
                reisetidDto.isGodkjent(),
                reisetidDto.getBegrunnelse(),
                getCurrentUserId(),
                vurdertTidspunkt))
            .toList();
        sjekkOverlappendePerioder(vurdertReisetid);
        return vurdertReisetid;
    }

    private void sjekkOverlappendePerioder(List<VurdertReisetid> vurdertReisetid) {
        List<DatoIntervallEntitet> perioder = new ArrayList<>(vurdertReisetid.stream().map(VurdertReisetid::getPeriode).toList());
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
                vurdertReisetid.getPeriode().getFomDato(),
                vurdertReisetid.getPeriode().getTomDato(),
                vurdertReisetid))
            .collect(Collectors.toList());
        return new LocalDateTimeline<>(segments);
    }

    private static String getCurrentUserId() {
        String brukerident = SubjectHandler.getSubjectHandler().getUid();
        return brukerident != null ? brukerident : "VL";
    }
}
