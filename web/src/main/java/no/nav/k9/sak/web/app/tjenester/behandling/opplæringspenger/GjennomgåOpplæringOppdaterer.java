package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.opplæringspenger.VurderGjennomgåttOpplæringDto;
import no.nav.k9.sak.kontrakt.opplæringspenger.VurderReisetidDto;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringPeriode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringPerioderHolder;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertReisetid;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderGjennomgåttOpplæringDto.class, adapter = AksjonspunktOppdaterer.class)
public class GjennomgåOpplæringOppdaterer implements AksjonspunktOppdaterer<VurderGjennomgåttOpplæringDto> {

    private VurdertOpplæringRepository vurdertOpplæringRepository;

    public GjennomgåOpplæringOppdaterer() {
    }

    @Inject
    public GjennomgåOpplæringOppdaterer(VurdertOpplæringRepository vurdertOpplæringRepository) {
        this.vurdertOpplæringRepository = vurdertOpplæringRepository;
    }

    @Override
    public OppdateringResultat oppdater(VurderGjennomgåttOpplæringDto dto, AksjonspunktOppdaterParameter param) {
        List<VurdertOpplæringPeriode> vurdertOpplæringPerioder = mapDtoTilVurdertOpplæringPerioder(dto);

        var aktivHolder = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(param.getBehandlingId())
            .map(VurdertOpplæringGrunnlag::getVurdertePerioder);

        if (aktivHolder.isPresent()) {
            LocalDateTimeline<VurdertOpplæringPeriode> vurdertOpplæringTidslinje = utledKombinertTidslinje(aktivHolder.get().getPerioder(), vurdertOpplæringPerioder);

            vurdertOpplæringPerioder = vurdertOpplæringTidslinje
                .stream()
                .map(datoSegment -> new VurdertOpplæringPeriode(datoSegment.getValue()))
                .toList();
        }

        var nyHolder = new VurdertOpplæringPerioderHolder(vurdertOpplæringPerioder);

        vurdertOpplæringRepository.lagre(param.getBehandlingId(), nyHolder);
        return OppdateringResultat.nyttResultat();
    }

    private List<VurdertOpplæringPeriode> mapDtoTilVurdertOpplæringPerioder(VurderGjennomgåttOpplæringDto dto) {
        List<VurdertOpplæringPeriode> vurdertOpplæringPerioder = dto.getPerioder()
            .stream()
            .map(periodeDto -> new VurdertOpplæringPeriode(periodeDto.getPeriode().getFom(), periodeDto.getPeriode().getTom(),
                periodeDto.getGjennomførtOpplæring(),
                mapReisetid(periodeDto.getReisetid()),
                periodeDto.getBegrunnelse()))
            .toList();
        sjekkOverlappendePerioder(vurdertOpplæringPerioder);
        return vurdertOpplæringPerioder;
    }

    private VurdertReisetid mapReisetid(VurderReisetidDto reisetidDto) {
        if (reisetidDto == null) {
            return null;
        }
        return new VurdertReisetid(
            finnReiseperiode(reisetidDto.getReisetidTil()),
            finnReiseperiode(reisetidDto.getReisetidHjem()),
            reisetidDto.getBegrunnelse());
    }

    private DatoIntervallEntitet finnReiseperiode(Periode periode) {
        if (periode == null || periode.getFom() == null || periode.getTom() == null) {
            return null;
        }
        return DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom());
    }

    private void sjekkOverlappendePerioder(List<VurdertOpplæringPeriode> vurdertOpplæringPerioder) {
        List<DatoIntervallEntitet> perioder = new ArrayList<>();

        for (VurdertOpplæringPeriode vurdertOpplæringPeriode : vurdertOpplæringPerioder) {
            perioder.add(vurdertOpplæringPeriode.getPeriode());

            if (vurdertOpplæringPeriode.getReisetid() != null) {
                if (vurdertOpplæringPeriode.getReisetid().getReiseperiodeTil() != null) {
                    perioder.add(vurdertOpplæringPeriode.getReisetid().getReiseperiodeTil());
                }
                if (vurdertOpplæringPeriode.getReisetid().getReiseperiodeHjem() != null) {
                    perioder.add(vurdertOpplæringPeriode.getReisetid().getReiseperiodeHjem());
                }
            }
        }

        for (DatoIntervallEntitet periode : perioder) {
            for (DatoIntervallEntitet periode2 : perioder) {
                if (periode != periode2 && periode.overlapper(periode2)) {
                    throw new IllegalArgumentException("Overlapp mellom " + periode + " og " + periode2 + " i vurdert opplæring.");
                }
            }
        }
    }

    private LocalDateTimeline<VurdertOpplæringPeriode> utledKombinertTidslinje(List<VurdertOpplæringPeriode> eksisterende,
                                                                               List<VurdertOpplæringPeriode> ny) {
        LocalDateTimeline<VurdertOpplæringPeriode> eksisterendeTidslinje = toTidslinje(eksisterende);
        LocalDateTimeline<VurdertOpplæringPeriode> nyTidslinje = toTidslinje(ny);

        return eksisterendeTidslinje.combine(nyTidslinje, (datoInterval, datoSegment, datoSegment2) -> {
                    VurdertOpplæringPeriode value = datoSegment2 == null ? datoSegment.getValue() : datoSegment2.getValue();
                    return new LocalDateSegment<>(datoInterval, value);
                },
                LocalDateTimeline.JoinStyle.CROSS_JOIN)
            .compress();
    }

    private LocalDateTimeline<VurdertOpplæringPeriode> toTidslinje(List<VurdertOpplæringPeriode> perioder) {
        final var segments = perioder
            .stream()
            .map(vurdertOpplæringPeriode -> new LocalDateSegment<>(vurdertOpplæringPeriode.getPeriode().getFomDato(), vurdertOpplæringPeriode.getPeriode().getTomDato(), vurdertOpplæringPeriode))
            .collect(Collectors.toList());
        return new LocalDateTimeline<>(segments);
    }
}
