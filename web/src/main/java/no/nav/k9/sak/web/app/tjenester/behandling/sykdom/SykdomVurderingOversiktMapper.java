package no.nav.k9.sak.web.app.tjenester.behandling.sykdom;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.kontrakt.ResourceLink;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.app.tjenester.behandling.BehandlingDtoUtil;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingVersjon;


@ApplicationScoped
public class SykdomVurderingOversiktMapper {
    public SykdomVurderingOversikt map(String behandlingUuid, Collection<SykdomVurderingVersjon> vurderinger, LocalDateTimeline<HashSet<Saksnummer>> saksnummerForPerioder) {
        final String saksnummer = "";

        LocalDateTimeline<SykdomVurderingVersjon> vurderingerTidslinje = tilTidslinje(vurderinger);

        final List<SykdomVurderingOversiktElement>  elements = tilSykdomVurderingOversiktElement(
                behandlingUuid, saksnummer, saksnummerForPerioder, vurderingerTidslinje
            )
            .compress()
            .stream()
            .map(ds -> ds.getValue())
            .collect(Collectors.toList());

        return new SykdomVurderingOversikt(
                elements,
                Arrays.asList(new Periode(LocalDate.now().minusDays(4), LocalDate.now().minusDays(3))), // TODO: Riktig verdi
                Arrays.asList(new Periode(LocalDate.now().minusDays(8), LocalDate.now())), // TODO: Riktig verdi
                Arrays.asList(new Periode(LocalDate.now().minusDays(10), LocalDate.now())), // TODO: Riktig verdi
                Arrays.asList(linkForNyVurdering(behandlingUuid))
                );
    }

    private LocalDateTimeline<SykdomVurderingOversiktElement> tilSykdomVurderingOversiktElement(String behandlingUuid, String saksnummer, LocalDateTimeline<HashSet<Saksnummer>> søktePerioder, LocalDateTimeline<SykdomVurderingVersjon> vurderingerTidslinje) {
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
                    UUID.fromString(behandlingUuid).equals(vurdering.getValue().getEndretBehandlingUuid()),
                    Arrays.asList(linkForGetVurdering(behandlingUuid, sykdomVurderingId), linkForEndreVurdering(behandlingUuid))
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

    LocalDateTimeline<SykdomVurderingVersjon> tilTidslinje(Collection<SykdomVurderingVersjon> vurderinger) {
        final Collection<LocalDateSegment<SykdomVurderingVersjon>> segments = new ArrayList<>();
        for (SykdomVurderingVersjon vurdering : vurderinger) {
            for (SykdomVurderingPeriode periode : vurdering.getPerioder()) {
                segments.add(new LocalDateSegment<SykdomVurderingVersjon>(periode.getFom(), periode.getTom(), vurdering));
            }
        }

        final LocalDateTimeline<SykdomVurderingVersjon> tidslinje = new LocalDateTimeline<>(segments, new LocalDateSegmentCombinator<SykdomVurderingVersjon, SykdomVurderingVersjon, SykdomVurderingVersjon>() {
            @Override
            public LocalDateSegment<SykdomVurderingVersjon> combine(LocalDateInterval datoInterval,
                    LocalDateSegment<SykdomVurderingVersjon> datoSegment,
                    LocalDateSegment<SykdomVurderingVersjon> datoSegment2) {
                final Long rangering1 = datoSegment.getValue().getSykdomVurdering().getRangering();
                final Long rangering2 = datoSegment2.getValue().getSykdomVurdering().getRangering();
                final Long versjon1 = datoSegment.getValue().getVersjon();
                final Long versjon2 = datoSegment2.getValue().getVersjon();

                final SykdomVurderingVersjon valgtVurdering;
                if (rangering1.compareTo(rangering2) > 0) {
                    valgtVurdering = datoSegment.getValue();
                } else if (rangering1.compareTo(rangering2) < 0) {
                    valgtVurdering = datoSegment2.getValue();
                } else {
                    valgtVurdering = (versjon1.compareTo(versjon2) > 0) ? datoSegment.getValue() : datoSegment2.getValue();
                }

                return new LocalDateSegment<>(datoInterval, valgtVurdering);
            }
        });

        return tidslinje.compress();
    }

}
