package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger;

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
import no.nav.k9.sak.kontrakt.opplæringspenger.VurderNødvendighetDto;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæring;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringHolder;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderNødvendighetDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderNødvendighetOppdaterer implements AksjonspunktOppdaterer<VurderNødvendighetDto> {

    private VurdertOpplæringRepository vurdertOpplæringRepository;

    public VurderNødvendighetOppdaterer() {
    }

    @Inject
    public VurderNødvendighetOppdaterer(VurdertOpplæringRepository vurdertOpplæringRepository) {
        this.vurdertOpplæringRepository = vurdertOpplæringRepository;
    }

    @Override
    public OppdateringResultat oppdater(VurderNødvendighetDto dto, AksjonspunktOppdaterParameter param) {
        List<VurdertOpplæring> vurdertOpplæring = mapDtoTilVurdertOpplæring(dto);

        var aktivVurdertInstitusjonHolder = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(param.getBehandlingId())
            .map(VurdertOpplæringGrunnlag::getVurdertOpplæringHolder);

        if (aktivVurdertInstitusjonHolder.isPresent()) {
            LocalDateTimeline<VurdertOpplæring> vurdertOpplæringTidslinje = utledKombinertTidslinje(aktivVurdertInstitusjonHolder.get().getVurdertOpplæring(), vurdertOpplæring);

            vurdertOpplæring = vurdertOpplæringTidslinje
                .stream()
                .map(datoSegment -> new VurdertOpplæring(datoSegment.getValue()).medPeriode(datoSegment.getFom(), datoSegment.getTom()))
                .toList();
        }

        VurdertOpplæringHolder nyHolder = new VurdertOpplæringHolder(vurdertOpplæring);

        vurdertOpplæringRepository.lagre(param.getBehandlingId(), nyHolder);
        return OppdateringResultat.nyttResultat();
    }

    private List<VurdertOpplæring> mapDtoTilVurdertOpplæring(VurderNødvendighetDto dto) {
        List<VurdertOpplæring> vurdertOpplæring = dto.getPerioder()
            .stream()
            .map(periodeDto -> new VurdertOpplæring(periodeDto.getFom(), periodeDto.getTom(), periodeDto.isNødvendigOpplæring(), periodeDto.getBegrunnelse(), periodeDto.getInstitusjon()))
            .toList();
        sjekkOverlappendePerioder(vurdertOpplæring);
        return vurdertOpplæring;
    }

    private void sjekkOverlappendePerioder(List<VurdertOpplæring> vurdertOpplæring) {
        List<DatoIntervallEntitet> perioder = vurdertOpplæring.stream().map(VurdertOpplæring::getPeriode).toList();
        for (DatoIntervallEntitet periode : perioder) {
            for (DatoIntervallEntitet periode2 : perioder) {
                if (periode != periode2 && periode.overlapper(periode2)) {
                    throw new IllegalArgumentException("Overlapp mellom " + periode + " og " + periode2 + " i vurdert opplæring.");
                }
            }
        }
    }

    private LocalDateTimeline<VurdertOpplæring> utledKombinertTidslinje(List<VurdertOpplæring> eksisterende,
                                                                        List<VurdertOpplæring> ny) {
        LocalDateTimeline<VurdertOpplæring> eksisterendeTidslinje = toTidslinje(eksisterende);
        LocalDateTimeline<VurdertOpplæring> nyTidslinje = toTidslinje(ny);

        return eksisterendeTidslinje.combine(nyTidslinje, (datoInterval, datoSegment, datoSegment2) -> {
                    VurdertOpplæring value = datoSegment2 == null ? datoSegment.getValue() : datoSegment2.getValue();
                    return new LocalDateSegment<>(datoInterval, value);
                },
                LocalDateTimeline.JoinStyle.CROSS_JOIN)
            .compress();
    }

    private LocalDateTimeline<VurdertOpplæring> toTidslinje(List<VurdertOpplæring> perioder) {
        final var segments = perioder
            .stream()
            .map(vurdertOpplæring -> new LocalDateSegment<>(vurdertOpplæring.getPeriode().getFomDato(), vurdertOpplæring.getPeriode().getTomDato(), vurdertOpplæring))
            .collect(Collectors.toList());
        return new LocalDateTimeline<>(segments);
    }
}
