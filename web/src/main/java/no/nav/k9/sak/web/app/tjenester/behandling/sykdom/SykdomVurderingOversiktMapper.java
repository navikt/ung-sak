package no.nav.k9.sak.web.app.tjenester.behandling.sykdom;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.ResourceLink;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.app.tjenester.behandling.BehandlingDtoUtil;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingVersjon;


@ApplicationScoped
public class SykdomVurderingOversiktMapper {
    public SykdomVurderingOversikt map(UUID behandlingUuid, Saksnummer saksnummer, LocalDateTimeline<SykdomVurderingVersjon> vurderingerTidslinje, LocalDateTimeline<HashSet<Saksnummer>> saksnummerForPerioder, NavigableSet<DatoIntervallEntitet> søknadsperioder, NavigableSet<DatoIntervallEntitet> vurderingsperioder) {

        final List<SykdomVurderingOversiktElement>  elements = tilSykdomVurderingOversiktElement(
                behandlingUuid, saksnummer, saksnummerForPerioder, vurderingerTidslinje
            )
            .compress()
            .stream()
            .map(ds -> ds.getValue())
            .collect(Collectors.toList());
        
        return new SykdomVurderingOversikt(
                elements,
                finnResterendeVurderingsperioder(vurderingsperioder, vurderingerTidslinje),
                finnNyeSøknadsperioder(søknadsperioder, saksnummerForPerioder),
                søknadsperioder.stream().map(d -> new Periode(d.getFomDato(), d.getTomDato())).collect(Collectors.toList()),
                Arrays.asList(linkForNyVurdering(behandlingUuid.toString()))
                );
    }
    
    private LocalDateTimeline<SykdomVurderingOversiktElement> tilSykdomVurderingOversiktElement(UUID behandlingUuid, Saksnummer saksnummer, LocalDateTimeline<HashSet<Saksnummer>> søktePerioder, LocalDateTimeline<SykdomVurderingVersjon> vurderingerTidslinje) {
        return vurderingerTidslinje.combine(søktePerioder, new LocalDateSegmentCombinator<SykdomVurderingVersjon, HashSet<Saksnummer>, SykdomVurderingOversiktElement>() {
            @Override
            public LocalDateSegment<SykdomVurderingOversiktElement> combine(LocalDateInterval datoInterval, LocalDateSegment<SykdomVurderingVersjon> vurdering, LocalDateSegment<HashSet<Saksnummer>> relevanteSaksnummer) {
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

    
    private List<Periode> finnResterendeVurderingsperioder(NavigableSet<DatoIntervallEntitet> vurderingsperioder, LocalDateTimeline<SykdomVurderingVersjon> vurderingerTidslinje) {
        final LocalDateTimeline<Boolean> vurderingsperioderTidslinje = new LocalDateTimeline<Boolean>(vurderingsperioder.stream().map(p -> new LocalDateSegment<Boolean>(p.getFomDato(), p.getTomDato(), true)).collect(Collectors.toList()));

        return vurderingsperioderTidslinje.combine(vurderingerTidslinje, new LocalDateSegmentCombinator<Boolean, SykdomVurderingVersjon, Boolean>() {
            @Override
            public LocalDateSegment<Boolean> combine(LocalDateInterval datoInterval,
                    LocalDateSegment<Boolean> datoSegment, LocalDateSegment<SykdomVurderingVersjon> datoSegment2) {
                if (datoSegment2 == null) {
                    return null;
                }
                return new LocalDateSegment<>(datoInterval, true);
            }
        }, JoinStyle.LEFT_JOIN).compress().stream().map(l -> new Periode(l.getFom(), l.getTom())).collect(Collectors.toList());
    }
    
    private List<Periode> finnNyeSøknadsperioder(NavigableSet<DatoIntervallEntitet> søknadsperioder, LocalDateTimeline<HashSet<Saksnummer>> saksnummerForPerioder) {
        final LocalDateTimeline<Boolean> søknadsperioderTidslinje = new LocalDateTimeline<Boolean>(søknadsperioder.stream().map(p -> new LocalDateSegment<Boolean>(p.getFomDato(), p.getTomDato(), true)).collect(Collectors.toList()));

        return søknadsperioderTidslinje.combine(saksnummerForPerioder, new LocalDateSegmentCombinator<Boolean, HashSet<Saksnummer>, Boolean>() {
            @Override
            public LocalDateSegment<Boolean> combine(LocalDateInterval datoInterval,
                    LocalDateSegment<Boolean> datoSegment, LocalDateSegment<HashSet<Saksnummer>> datoSegment2) {
                if (datoSegment2 == null) {
                    return new LocalDateSegment<>(datoInterval, true);
                }
                return null;
            }
        }, JoinStyle.LEFT_JOIN).compress().stream().map(l -> new Periode(l.getFom(), l.getTom())).collect(Collectors.toList());
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
