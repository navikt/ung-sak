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
    public SykdomVurderingOversikt map(UUID behandlingUuid, Saksnummer saksnummer, LocalDateTimeline<SykdomVurderingVersjon> vurderingerTidslinje, LocalDateTimeline<Set<Saksnummer>> saksnummerForPerioder, NavigableSet<DatoIntervallEntitet> søknadsperioder, NavigableSet<DatoIntervallEntitet> vurderingsperioder) {

        final List<Periode> nyeSøknadsperioder = finnNyeSøknadsperioder(søknadsperioder, saksnummerForPerioder);
        final LocalDateTimeline<Set<Saksnummer>> saksnummertidslinjeeMedNyePerioder = saksnummertidslinjeeMedNyePerioder(saksnummerForPerioder, søknadsperioder, saksnummer);
        
        final List<SykdomVurderingOversiktElement>  elements = tilSykdomVurderingOversiktElement(
                behandlingUuid, saksnummer, saksnummertidslinjeeMedNyePerioder, vurderingerTidslinje
            )
            .compress()
            .stream()
            .map(ds -> ds.getValue())
            .collect(Collectors.toList());
        // TODO: saksnummerForPerioder + finnNyeSøknadsperioder
        return new SykdomVurderingOversikt(
                elements,
                finnResterendeVurderingsperioder(vurderingsperioder, vurderingerTidslinje),
                nyeSøknadsperioder,
                søknadsperioder.stream().map(d -> new Periode(d.getFomDato(), d.getTomDato())).collect(Collectors.toList()),
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

    
    private List<Periode> finnResterendeVurderingsperioder(NavigableSet<DatoIntervallEntitet> vurderingsperioder, LocalDateTimeline<SykdomVurderingVersjon> vurderingerTidslinje) {
        return toPeriodeList(
                    kunPerioderSomIkkeFinnesI(toLocalDateTimeline(vurderingsperioder), vurderingerTidslinje)
                );
    }
    
    private List<Periode> finnNyeSøknadsperioder(NavigableSet<DatoIntervallEntitet> søknadsperioder, LocalDateTimeline<Set<Saksnummer>> saksnummerForPerioder) {
        return toPeriodeList(
                    kunPerioderSomIkkeFinnesI(toLocalDateTimeline(søknadsperioder), saksnummerForPerioder)
               );
    }
    
    private static LocalDateTimeline<Set<Saksnummer>> saksnummertidslinjeeMedNyePerioder(LocalDateTimeline<Set<Saksnummer>> saksnummerForPerioder, NavigableSet<DatoIntervallEntitet> nyeSøknadsperioder, Saksnummer saksnummer) {
        final LocalDateTimeline<Set<Saksnummer>> nyTidslinje = new LocalDateTimeline<Set<Saksnummer>>(nyeSøknadsperioder.stream().map(p -> new LocalDateSegment<>(p.getFomDato(), p.getTomDato(), Collections.singleton(saksnummer))).collect(Collectors.toList()));
        return saksnummerForPerioder.union(nyTidslinje, new LocalDateSegmentCombinator<Set<Saksnummer>, Set<Saksnummer>, Set<Saksnummer>>() {
            @Override
            public LocalDateSegment<Set<Saksnummer>> combine(LocalDateInterval datoInterval,
                    LocalDateSegment<Set<Saksnummer>> datoSegment,
                    LocalDateSegment<Set<Saksnummer>> datoSegment2) {
                if (datoSegment == null) {
                    return new LocalDateSegment<Set<Saksnummer>>(datoInterval, datoSegment2.getValue());
                }
                if (datoSegment2 == null) {
                    return new LocalDateSegment<Set<Saksnummer>>(datoInterval, datoSegment.getValue());
                }
                
                final Set<Saksnummer> saksnumre = new HashSet<>(datoSegment.getValue());
                saksnumre.addAll(datoSegment2.getValue());
                return new LocalDateSegment<Set<Saksnummer>>(datoInterval, saksnumre);
            }
        });
    }

    private static List<Periode> toPeriodeList(LocalDateTimeline<?> t) {
        return t.stream().map(l -> new Periode(l.getFom(), l.getTom())).collect(Collectors.toList());
    }
    
    private static LocalDateTimeline<Boolean> toLocalDateTimeline(NavigableSet<DatoIntervallEntitet> datoer) {
        return new LocalDateTimeline<Boolean>(datoer.stream().map(p -> new LocalDateSegment<Boolean>(p.getFomDato(), p.getTomDato(), true)).collect(Collectors.toList()));
    }
    
    private static <T, U> LocalDateTimeline<T> kunPerioderSomIkkeFinnesI(LocalDateTimeline<T> perioder, LocalDateTimeline<U> perioderSomSkalTrekkesFra) {
        return perioder.combine(perioderSomSkalTrekkesFra, new LocalDateSegmentCombinator<T, U, T>() {
            @Override
            public LocalDateSegment<T> combine(LocalDateInterval datoInterval,
                    LocalDateSegment<T> datoSegment, LocalDateSegment<U> datoSegment2) {
                if (datoSegment2 == null) {
                    return new LocalDateSegment<>(datoInterval, datoSegment.getValue());
                }
                return null;
            }
        }, JoinStyle.LEFT_JOIN).compress();
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
