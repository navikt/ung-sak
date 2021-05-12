package no.nav.k9.sak.web.app.tjenester.behandling.sykdom;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.kontrakt.ResourceLink;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingEndringDto;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingIdDto;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingOpprettelseDto;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingOversikt;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingOversiktElement;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.app.tjenester.behandling.BehandlingDtoUtil;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingService.SykdomVurderingerOgPerioder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.PleietrengendeAlderPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingVersjon;

public class SykdomVurderingOversiktMapper {
    public SykdomVurderingOversikt map(UUID behandlingUuid, Saksnummer saksnummer, SykdomVurderingerOgPerioder sykdomVurderingerOgPerioder, LocalDate pleietrengendesFødselsdato) {
        final var elements = tilSykdomVurderingOversiktElement(behandlingUuid, saksnummer, sykdomVurderingerOgPerioder);
        
        return new SykdomVurderingOversikt(
                elements,
                sykdomVurderingerOgPerioder.getResterendeVurderingsperioder(),
                sykdomVurderingerOgPerioder.getResterendeValgfrieVurderingsperioder(),
                sykdomVurderingerOgPerioder.getNyeSøknadsperioder(),
                sykdomVurderingerOgPerioder.getPerioderSomKanVurderes(),
                pleietrengendesFødselsdato,
                harPerioderDerPleietrengendeErOver18år(sykdomVurderingerOgPerioder, pleietrengendesFødselsdato),
                Arrays.asList(linkForNyVurdering(behandlingUuid.toString()))
                );
    }
    
    private List<SykdomVurderingOversiktElement> tilSykdomVurderingOversiktElement(UUID behandlingUuid, Saksnummer saksnummer,  SykdomVurderingerOgPerioder sykdomVurderingerOgPerioder) {        
        var elements = vurderingerTilElement(sykdomVurderingerOgPerioder.getVurderingerTidslinje(), behandlingUuid);
        elements = medInnleggelser(elements, sykdomVurderingerOgPerioder.getInnleggelsesperioder());
        elements = medInformasjonOmSøktePerioder(elements, saksnummer, sykdomVurderingerOgPerioder.getSaksnummerForPerioder());
        
        return SykdomUtils.values(elements);
    }

    private LocalDateTimeline<SykdomVurderingOversiktElement> vurderingerTilElement(LocalDateTimeline<SykdomVurderingVersjon> vurderingerTidslinje, UUID behandlingUuid) {
        return vurderingerTidslinje.map(vurdering -> {
            final String sykdomVurderingId = "" + vurdering.getValue().getSykdomVurdering().getId();
            return List.of(new LocalDateSegment<SykdomVurderingOversiktElement>(vurdering.getLocalDateInterval(), new SykdomVurderingOversiktElement(
                    sykdomVurderingId,
                    vurdering.getValue().getResultat(),
                    new Periode(vurdering.getFom(), vurdering.getTom()),
                    behandlingUuid.equals(vurdering.getValue().getEndretBehandlingUuid()),
                    Arrays.asList(linkForGetVurdering(behandlingUuid.toString(), sykdomVurderingId), linkForEndreVurdering(behandlingUuid.toString()))
                )));
        });
    }

    private LocalDateTimeline<SykdomVurderingOversiktElement> medInnleggelser(LocalDateTimeline<SykdomVurderingOversiktElement> elementsTidslinje, List<Periode> innleggelsesperioder) {
        final LocalDateTimeline<Boolean> innleggelsesperioderTidslinje = SykdomUtils.toLocalDateTimeline(innleggelsesperioder);
        return elementsTidslinje.combine(innleggelsesperioderTidslinje, new LocalDateSegmentCombinator<SykdomVurderingOversiktElement, Boolean, SykdomVurderingOversiktElement>() {
            @Override
            public LocalDateSegment<SykdomVurderingOversiktElement> combine(
                    LocalDateInterval datoInterval,
                    LocalDateSegment<SykdomVurderingOversiktElement> vs,
                    LocalDateSegment<Boolean> innleggelse) {
                
                final SykdomVurderingOversiktElement oldElement = (vs != null && vs.getValue() != null) ? vs.getValue() : new SykdomVurderingOversiktElement();
                final SykdomVurderingOversiktElement newElement = new SykdomVurderingOversiktElement(oldElement);
                newElement.setPeriode(new Periode(datoInterval.getFomDato(), datoInterval.getTomDato()));
                newElement.setErInnleggelsesperiode(innleggelse != null);
                
                return new LocalDateSegment<SykdomVurderingOversiktElement>(datoInterval, newElement);
            }
        }, LocalDateTimeline.JoinStyle.CROSS_JOIN);
    }
    
    private LocalDateTimeline<SykdomVurderingOversiktElement> medInformasjonOmSøktePerioder(
            LocalDateTimeline<SykdomVurderingOversiktElement> elementsTidslinje,
            Saksnummer saksnummer,
            LocalDateTimeline<Set<Saksnummer>> søktePerioder) {
        
        return elementsTidslinje.combine(søktePerioder, new LocalDateSegmentCombinator<SykdomVurderingOversiktElement, Set<Saksnummer>, SykdomVurderingOversiktElement>() {
            @Override
            public LocalDateSegment<SykdomVurderingOversiktElement> combine(LocalDateInterval datoInterval, LocalDateSegment<SykdomVurderingOversiktElement> element, LocalDateSegment<Set<Saksnummer>> relevanteSaksnummer) {
                final SykdomVurderingOversiktElement newElement = new SykdomVurderingOversiktElement(element.getValue());
                
                final Set<Saksnummer> s = relevanteSaksnummer != null ? relevanteSaksnummer.getValue() : Collections.emptySet();
                final int antallSomBrukerVurdering = s.size();
                
                newElement.setPeriode(new Periode(datoInterval.getFomDato(), datoInterval.getTomDato()));
                newElement.setGjelderForSøker(s.contains(saksnummer));
                newElement.setGjelderForSøker(antallSomBrukerVurdering > 1 || (antallSomBrukerVurdering == 1 && !s.contains(saksnummer)));
                
                return new LocalDateSegment<>(datoInterval, newElement);
            }
        }, LocalDateTimeline.JoinStyle.LEFT_JOIN).compress();
    }
    
    private boolean harPerioderDerPleietrengendeErOver18år(SykdomVurderingerOgPerioder sykdomVurderingerOgPerioder,
            LocalDate pleietrengendesFødselsdato) {
        return sykdomVurderingerOgPerioder.getPerioderSomKanVurderes().stream()
                .anyMatch(p -> !pleietrengendesFødselsdato.plusYears(PleietrengendeAlderPeriode.ALDER_FOR_STRENGERE_PSB_VURDERING).isAfter(p.getTom()));
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
