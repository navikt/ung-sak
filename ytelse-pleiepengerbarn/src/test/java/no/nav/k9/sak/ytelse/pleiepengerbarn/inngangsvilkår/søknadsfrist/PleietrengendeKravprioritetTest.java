package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.PleietrengendeKravprioritet.Kravprioritet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;

@SuppressWarnings("unchecked")
public class PleietrengendeKravprioritetTest {

    @Test
    public void fungererMedIngenKravDokumenter() {
        final LocalDateTimeline<List<Kravprioritet>> kravprioritet = kallVurderKallPrioritetMedMocker(new HashMap<>(), new HashMap<>(), new HashMap<>());
        assertThat(kravprioritet).isNotNull();
        assertThat(kravprioritet.toSegments().size()).isEqualTo(0);
    }
    
    @Test
    public void skalFungereMedEnPartOgEnPeriode() {
        final Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> kravdokumenter1 = new HashMap<>();
        final LocalDateTime førsteKrav = LocalDateTime.now().minusHours(2);
        kravdokumenter1.put(
            new KravDokument(null, førsteKrav, null), List.of(
                new VurdertSøktPeriode<Søknadsperiode>(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(4), LocalDate.now().minusDays(2)), Utfall.OPPFYLT, null)
            )
        );

        final LocalDateTimeline<List<Kravprioritet>> kravprioritet = kallVurderKallPrioritetMedMocker(kravdokumenter1);
        assertThat(kravprioritet).isNotNull();
        
        final NavigableSet<LocalDateSegment<List<Kravprioritet>>> segments = kravprioritet.toSegments();
        assertThat(segments.size()).isEqualTo(1);
        var iterator = segments.iterator();
        assertSegmentFørsteprioritet(iterator.next(), 0, LocalDate.now().minusDays(4), LocalDate.now().minusDays(2), førsteKrav);
    }
    
    @Test
    public void skalFungereMedEnPartOgFlerePerioder() {
        final Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> kravdokumenter1 = new HashMap<>();
        final LocalDateTime førsteKrav = LocalDateTime.now().minusHours(2);
        final LocalDateTime andreKrav = LocalDateTime.now().minusHours(1);
        kravdokumenter1.put(new KravDokument(null, andreKrav, null), List.of(
            new VurdertSøktPeriode<Søknadsperiode>(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(4), LocalDate.now().minusDays(2)), Utfall.OPPFYLT, null),
            new VurdertSøktPeriode<Søknadsperiode>(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(20), LocalDate.now().minusDays(14)), Utfall.OPPFYLT, null)
        ));
        kravdokumenter1.put(new KravDokument(null, LocalDateTime.now().minusMonths(1), null), List.of(
            new VurdertSøktPeriode<Søknadsperiode>(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(14), LocalDate.now()), Utfall.IKKE_OPPFYLT, null)
        ));
        kravdokumenter1.put(new KravDokument(null, førsteKrav, null), List.of(
            new VurdertSøktPeriode<Søknadsperiode>(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(7), LocalDate.now().minusDays(3)), Utfall.OPPFYLT, null)
        ));

        final LocalDateTimeline<List<Kravprioritet>> kravprioritet = kallVurderKallPrioritetMedMocker(kravdokumenter1);
        assertThat(kravprioritet).isNotNull();
        
        final NavigableSet<LocalDateSegment<List<Kravprioritet>>> segments = kravprioritet.toSegments();
        assertThat(segments.size()).isEqualTo(3);
        
        var iterator = segments.iterator();
        assertSegmentFørsteprioritet(iterator.next(), 0, LocalDate.now().minusDays(20), LocalDate.now().minusDays(14), andreKrav);
        assertSegmentFørsteprioritet(iterator.next(), 0, LocalDate.now().minusDays(7), LocalDate.now().minusDays(3), førsteKrav);
        assertSegmentFørsteprioritet(iterator.next(), 0, LocalDate.now().minusDays(2), LocalDate.now().minusDays(2), andreKrav);
    }
    
    @Test
    public void skalFungereMedFlereParter() {
        final Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> kravdokumenter1 = new HashMap<>();
        final LocalDateTime førsteKrav = LocalDateTime.now().minusHours(2);
        final LocalDateTime andreKrav = LocalDateTime.now().minusHours(1);
        kravdokumenter1.put(new KravDokument(null, andreKrav, null), List.of(
            new VurdertSøktPeriode<Søknadsperiode>(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(4), LocalDate.now().minusDays(2)), Utfall.OPPFYLT, null),
            new VurdertSøktPeriode<Søknadsperiode>(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(20), LocalDate.now().minusDays(14)), Utfall.OPPFYLT, null)
        ));
        kravdokumenter1.put(new KravDokument(null, LocalDateTime.now().minusMonths(1), null), List.of(
            new VurdertSøktPeriode<Søknadsperiode>(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(14), LocalDate.now()), Utfall.IKKE_OPPFYLT, null)
        ));
        kravdokumenter1.put(new KravDokument(null, førsteKrav, null), List.of(
            new VurdertSøktPeriode<Søknadsperiode>(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(7), LocalDate.now().minusDays(3)), Utfall.OPPFYLT, null)
        ));
        
        final Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> kravdokumenter2 = new HashMap<>();
        final LocalDateTime nullteKrav = LocalDateTime.now().minusHours(3);
        kravdokumenter2.put(new KravDokument(null, nullteKrav, null), List.of(
            new VurdertSøktPeriode<Søknadsperiode>(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(16), LocalDate.now().minusDays(15)), Utfall.OPPFYLT, null)
        ));

        final LocalDateTimeline<List<Kravprioritet>> kravprioritet = kallVurderKallPrioritetMedMocker(kravdokumenter1, kravdokumenter2);
        assertThat(kravprioritet).isNotNull();
        
        final NavigableSet<LocalDateSegment<List<Kravprioritet>>> segments = kravprioritet.toSegments();
        assertThat(segments.size()).isEqualTo(5);
        
        final var iterator = segments.iterator();
        
        assertSegmentFørsteprioritet(iterator.next(), 0, LocalDate.now().minusDays(20), LocalDate.now().minusDays(17), andreKrav);
        
        final var segment = iterator.next();
        assertSegmentFørsteprioritet(segment, 1, LocalDate.now().minusDays(16), LocalDate.now().minusDays(15), nullteKrav);
        assertSegmentAndreprioritet(segment, 0, LocalDate.now().minusDays(16), LocalDate.now().minusDays(15), andreKrav);
        assertSegmentFørsteprioritet(iterator.next(), 0, LocalDate.now().minusDays(14), LocalDate.now().minusDays(14), andreKrav);
        assertSegmentFørsteprioritet(iterator.next(), 0, LocalDate.now().minusDays(7), LocalDate.now().minusDays(3), førsteKrav);
        assertSegmentFørsteprioritet(iterator.next(), 0, LocalDate.now().minusDays(2), LocalDate.now().minusDays(2), andreKrav);
    }

    private void assertSegmentFørsteprioritet(LocalDateSegment<List<Kravprioritet>> segment, long fagsakId, LocalDate fom, LocalDate tom, LocalDateTime kravDato) {
        assertSegmentPrioritet(0, segment, fagsakId, fom, tom, kravDato);
    }
    
    private void assertSegmentAndreprioritet(LocalDateSegment<List<Kravprioritet>> segment, long fagsakId, LocalDate fom, LocalDate tom, LocalDateTime kravDato) {
        assertSegmentPrioritet(1, segment, fagsakId, fom, tom, kravDato);
    }
    
    private void assertSegmentPrioritet(int prioritet, LocalDateSegment<List<Kravprioritet>> segment, long fagsakId, LocalDate fom, LocalDate tom, LocalDateTime kravDato) {
        assertThat(segment.getLocalDateInterval()).isEqualTo(new LocalDateInterval(fom, tom));
        assertThat(segment.getValue().get(prioritet).getSaksnummer()).isEqualTo(new Saksnummer("s" + fagsakId));
        assertThat(segment.getValue().get(prioritet).getTidspunktForKrav()).isEqualTo(kravDato);
    }
    

    private LocalDateTimeline<List<Kravprioritet>> kallVurderKallPrioritetMedMocker(Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>>... fagsakKravdokumenter) {
        final FagsakRepository fagsakRepository = createFagsakRepositoryMock(fagsakKravdokumenter.length);
        final BehandlingRepository behandlingRepository = createBehandlingRepositoryMock();
        final Map<Long, Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>>> inputData = new HashMap<>();
        for (int fagsakId=0; fagsakId<fagsakKravdokumenter.length; fagsakId++) {
            inputData.put((long) fagsakId, fagsakKravdokumenter[fagsakId]);    
        }
        
        final VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste = mock(VurderSøknadsfristTjeneste.class);
        when(søknadsfristTjeneste.vurderSøknadsfrist(Mockito.any(BehandlingReferanse.class))).then(br -> {
            return inputData.get(((BehandlingReferanse) br.getArgument(0)).getBehandlingId());
        });
        
        final PleietrengendeKravprioritet pleietrengendeKravprioritet = new PleietrengendeKravprioritet(fagsakRepository, behandlingRepository, søknadsfristTjeneste);
        return pleietrengendeKravprioritet.vurderKravprioritet(new AktørId("utmocket"));
    }
    
    private FagsakRepository createFagsakRepositoryMock(int antall) {
        final FagsakRepository fagsakRepository = mock(FagsakRepository.class);
        
        final List<Fagsak> fagsaker = new ArrayList<>();
        for (int fagsakId=0; fagsakId<antall; fagsakId++) {
            final Fagsak fagsak = mock(Fagsak.class);
            when(fagsak.getId()).thenReturn((long) fagsakId);
            when(fagsak.getSaksnummer()).thenReturn(new Saksnummer("s" + fagsakId));
            fagsaker.add(fagsak);

        }
            
        when(fagsakRepository.finnFagsakRelatertTil(any(), any(), any(), any(), any())).thenReturn(fagsaker);
        return fagsakRepository;
    }
    
    private BehandlingRepository createBehandlingRepositoryMock() {
        final BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(any())).then(p -> {
            final Behandling behandling = mock(Behandling.class);
            when(behandling.getId()).thenReturn((long) p.getArgument(0));
            when(behandling.getFagsak()).thenReturn(mock(Fagsak.class));
            return Optional.of(behandling);
        });
        return behandlingRepository;
    }
}
