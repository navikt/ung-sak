package no.nav.k9.sak.web.app.tjenester.behandling.sykdom;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.kontrakt.ResourceLink;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.app.tjenester.behandling.BehandlingDtoUtil;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingService.SykdomVurderingerOgPerioder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingVersjon;

public class SykdomVurderingOversiktMapper {
    public SykdomVurderingOversikt map(UUID behandlingUuid, Saksnummer saksnummer, SykdomVurderingerOgPerioder sykdomVurderingerOgPerioder) {
        final List<SykdomVurderingOversiktElement>  elements = tilSykdomVurderingOversiktElement(
                behandlingUuid, saksnummer, sykdomVurderingerOgPerioder.getSaksnummerForPerioder(), sykdomVurderingerOgPerioder.getVurderingerTidslinje()
            )
            .compress()
            .stream()
            .map(ds -> ds.getValue())
            .collect(Collectors.toList());

        return new SykdomVurderingOversikt(
                elements,
                sykdomVurderingerOgPerioder.getResterendeVurderingsperioder(),
                sykdomVurderingerOgPerioder.getNyeSøknadsperioder(),
                sykdomVurderingerOgPerioder.getPerioderSomKanVurderes(),
                Arrays.asList(linkForNyVurdering(behandlingUuid.toString()))
                );
    }
    
    private LocalDateTimeline<SykdomVurderingOversiktElement> tilSykdomVurderingOversiktElement(UUID behandlingUuid, Saksnummer saksnummer, LocalDateTimeline<Set<Saksnummer>> søktePerioder, LocalDateTimeline<SykdomVurderingVersjon> vurderingerTidslinje) {
        return vurderingerTidslinje.combine(søktePerioder, new LocalDateSegmentCombinator<SykdomVurderingVersjon, Set<Saksnummer>, SykdomVurderingOversiktElement>() {
            @Override
            public LocalDateSegment<SykdomVurderingOversiktElement> combine(LocalDateInterval datoInterval, LocalDateSegment<SykdomVurderingVersjon> vurdering, LocalDateSegment<Set<Saksnummer>> relevanteSaksnummer) {
                final String sykdomVurderingId = "" + vurdering.getValue().getSykdomVurdering().getId();
                final Set<Saksnummer> s = relevanteSaksnummer != null ? relevanteSaksnummer.getValue() : Collections.emptySet();
                final int antallSomBrukerVurdering = s.size();
                return new LocalDateSegment<>(datoInterval, new SykdomVurderingOversiktElement(
                    sykdomVurderingId,
                    vurdering.getValue().getResultat(),
                    new Periode(vurdering.getFom(), vurdering.getTom()),
                    s.contains(saksnummer),
                    antallSomBrukerVurdering > 1 || (antallSomBrukerVurdering == 1 && !s.contains(saksnummer)),
                    behandlingUuid.equals(vurdering.getValue().getEndretBehandlingUuid()),
                    Arrays.asList(linkForGetVurdering(behandlingUuid.toString(), sykdomVurderingId), linkForEndreVurdering(behandlingUuid.toString()))
                ));
            }
        }, LocalDateTimeline.JoinStyle.LEFT_JOIN);
    }

    private ResourceLink linkForGetVurdering(String behandlingUuid, String sykdomVurderingId) {
        return ResourceLink.get(BehandlingDtoUtil.getApiPath(SykdomVurderingRestTjeneste.VURDERING_PATH), "sykdom-vurdering", Map.of(BehandlingUuidDto.NAME, behandlingUuid, SykdomVurderingIdDto.NAME, sykdomVurderingId));
    }

    private ResourceLink linkForNyVurdering(String behandlingUuid) {
        return ResourceLink.post(BehandlingDtoUtil.getApiPath(SykdomVurderingRestTjeneste.VURDERING_PATH), "sykdom-vurdering-opprettelse", new SykdomVurderingOpprettelseDto(behandlingUuid));
    }

    private ResourceLink linkForEndreVurdering(String behandlingUuid) {
        return ResourceLink.post(BehandlingDtoUtil.getApiPath(SykdomVurderingRestTjeneste.VURDERING_VERSJON_PATH), "sykdom-vurdering-endring", new SykdomVurderingEndringDto(behandlingUuid));
    }

}
