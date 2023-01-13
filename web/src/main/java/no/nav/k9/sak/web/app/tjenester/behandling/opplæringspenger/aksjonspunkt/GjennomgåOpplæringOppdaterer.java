package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.aksjonspunkt;

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
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.opplæringspenger.VurderGjennomgåttOpplæringDto;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringPeriode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringPerioderHolder;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomDokumentRepository;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderGjennomgåttOpplæringDto.class, adapter = AksjonspunktOppdaterer.class)
public class GjennomgåOpplæringOppdaterer implements AksjonspunktOppdaterer<VurderGjennomgåttOpplæringDto> {

    private VurdertOpplæringRepository vurdertOpplæringRepository;
    private BehandlingRepository behandlingRepository;
    private PleietrengendeSykdomDokumentRepository pleietrengendeSykdomDokumentRepository;

    public GjennomgåOpplæringOppdaterer() {
    }

    @Inject
    public GjennomgåOpplæringOppdaterer(VurdertOpplæringRepository vurdertOpplæringRepository,
                                        BehandlingRepository behandlingRepository,
                                        PleietrengendeSykdomDokumentRepository pleietrengendeSykdomDokumentRepository) {
        this.vurdertOpplæringRepository = vurdertOpplæringRepository;
        this.behandlingRepository = behandlingRepository;
        this.pleietrengendeSykdomDokumentRepository = pleietrengendeSykdomDokumentRepository;
    }

    @Override
    public OppdateringResultat oppdater(VurderGjennomgåttOpplæringDto dto, AksjonspunktOppdaterParameter param) {
        final Behandling behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        final List<PleietrengendeSykdomDokument> alleDokumenter = pleietrengendeSykdomDokumentRepository.hentAlleDokumenterFor(behandling.getFagsak().getPleietrengendeAktørId());

        List<VurdertOpplæringPeriode> vurdertOpplæringPerioder = mapDtoTilVurdertOpplæringPerioder(dto, alleDokumenter);

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

    private List<VurdertOpplæringPeriode> mapDtoTilVurdertOpplæringPerioder(VurderGjennomgåttOpplæringDto dto, List<PleietrengendeSykdomDokument> alleDokumenter) {

        List<VurdertOpplæringPeriode> vurdertOpplæringPerioder = dto.getPerioder()
            .stream()
            .map(periodeDto -> new VurdertOpplæringPeriode(periodeDto.getPeriode().getFom(), periodeDto.getPeriode().getTom(),
                periodeDto.getGjennomførtOpplæring(),
                periodeDto.getBegrunnelse(),
                alleDokumenter.stream().filter(dokument -> periodeDto.getTilknyttedeDokumenter().contains("" + dokument.getId())).collect(Collectors.toList())))
            .toList();
        sjekkOverlappendePerioder(vurdertOpplæringPerioder);
        return vurdertOpplæringPerioder;
    }

    private void sjekkOverlappendePerioder(List<VurdertOpplæringPeriode> vurdertOpplæringPerioder) {
        List<DatoIntervallEntitet> perioder = new ArrayList<>();

        perioder.addAll(vurdertOpplæringPerioder.stream().map(VurdertOpplæringPeriode::getPeriode).toList());

        new LocalDateTimeline<>(perioder.stream().map(periode -> new LocalDateSegment<>(periode.getFomDato(), periode.getTomDato(), true)).toList());
    }

    private LocalDateTimeline<VurdertOpplæringPeriode> utledKombinertTidslinje(List<VurdertOpplæringPeriode> eksisterende,
                                                                               List<VurdertOpplæringPeriode> ny) {
        LocalDateTimeline<VurdertOpplæringPeriode> eksisterendeTidslinje = toTidslinje(eksisterende);
        LocalDateTimeline<VurdertOpplæringPeriode> nyTidslinje = toTidslinje(ny);

        return eksisterendeTidslinje.combine(nyTidslinje, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN).compress();
    }

    private LocalDateTimeline<VurdertOpplæringPeriode> toTidslinje(List<VurdertOpplæringPeriode> perioder) {
        final var segments = perioder
            .stream()
            .map(vurdertOpplæringPeriode -> new LocalDateSegment<>(vurdertOpplæringPeriode.getPeriode().getFomDato(), vurdertOpplæringPeriode.getPeriode().getTomDato(), vurdertOpplæringPeriode))
            .collect(Collectors.toList());
        return new LocalDateTimeline<>(segments);
    }
}
