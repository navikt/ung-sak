package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;

@Dependent
public class PleietrengendeKravprioritet {
    
    private final FagsakRepository fagsakRepository;
    private final BehandlingRepository behandlingRepository;
    private VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste;
    
    @Inject
    public PleietrengendeKravprioritet(FagsakRepository fagsakRepository,
            BehandlingRepository behandlingRepository,
                @FagsakYtelseTypeRef("PSB") VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.søknadsfristTjeneste = søknadsfristTjeneste;
    }
    

    @SuppressWarnings("unchecked")
    public LocalDateTimeline<List<Kravprioritet>> vurderKravprioritet(AktørId pleietrengende) {
        final List<Fagsak> fagsaker = fagsakRepository.finnFagsakRelatertTil(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, pleietrengende, null, null, null);        
        
        LocalDateTimeline<List<Kravprioritet>> kravprioritetstidslinje = LocalDateTimeline.EMPTY_TIMELINE;
        for (Fagsak fagsak : fagsaker) {
            final LocalDateTimeline<Kravprioritet> fagsakTidslinje = finnEldsteKravTidslinjeForFagsak(fagsak);
            kravprioritetstidslinje = kravprioritetstidslinje.union(fagsakTidslinje, sortertMedEldsteKravFørst());
        }
        
        return kravprioritetstidslinje.compress();
    }
    
    
    private LocalDateTimeline<Kravprioritet> finnEldsteKravTidslinjeForFagsak(Fagsak fagsak) {
        @SuppressWarnings("unchecked")
        LocalDateTimeline<Kravprioritet> fagsakTidslinje = LocalDateTimeline.EMPTY_TIMELINE;
        final Optional<Behandling> behandlingOpt = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId());
        if (behandlingOpt.isEmpty()) {
            return fagsakTidslinje;
        }
        
        final Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> kravDokumenter = søknadsfristTjeneste.vurderSøknadsfrist(BehandlingReferanse.fra(behandlingOpt.get()));
        for (Map.Entry<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> kravdokument : kravDokumenter.entrySet()) {
            final LocalDateTimeline<Kravprioritet> periodetidslinje = new LocalDateTimeline<>(kravdokument.getValue()
                .stream()
                .filter(vsp -> vsp.getUtfall() == no.nav.k9.kodeverk.vilkår.Utfall.OPPFYLT)
                .map(vsp -> new LocalDateSegment<>(vsp.getPeriode().toLocalDateInterval(), new Kravprioritet(fagsak, kravdokument.getKey().getInnsendingsTidspunkt())))
                .collect(Collectors.toList())
            );
            fagsakTidslinje = fagsakTidslinje.union(periodetidslinje, new LocalDateSegmentCombinator<Kravprioritet, Kravprioritet, Kravprioritet>() {
                @Override
                public LocalDateSegment<Kravprioritet> combine(LocalDateInterval datoInterval,
                        LocalDateSegment<Kravprioritet> datoSegment,
                        LocalDateSegment<Kravprioritet> datoSegment2) {
                    if (datoSegment == null) {
                        return new LocalDateSegment<>(datoInterval, datoSegment2.getValue());
                    }
                    if (datoSegment2 == null) {
                        return new LocalDateSegment<>(datoInterval, datoSegment.getValue());
                    }
                    if (datoSegment.getValue().compareTo(datoSegment2.getValue()) <= 0) {
                        return new LocalDateSegment<>(datoInterval, datoSegment.getValue());    
                    } else {
                        return new LocalDateSegment<>(datoInterval, datoSegment2.getValue());
                    }
                }
            });
        }
        return fagsakTidslinje.compress();
    }
    
    private LocalDateSegmentCombinator<List<Kravprioritet>, Kravprioritet, List<Kravprioritet>> sortertMedEldsteKravFørst() {
        return new LocalDateSegmentCombinator<List<Kravprioritet>, Kravprioritet, List<Kravprioritet>>() {
            @Override
            public LocalDateSegment<List<Kravprioritet>> combine(LocalDateInterval datoInterval,
                    LocalDateSegment<List<Kravprioritet>> datoSegment,
                    LocalDateSegment<Kravprioritet> datoSegment2) {
                
                if (datoSegment == null) {
                    return new LocalDateSegment<>(datoInterval, List.of(datoSegment2.getValue()));
                }
                if (datoSegment2 == null) {
                    return new LocalDateSegment<>(datoInterval, datoSegment.getValue());
                }
                final List<Kravprioritet> liste = new ArrayList<>(datoSegment.getValue());
                liste.add(datoSegment2.getValue());
                Collections.sort(liste);
                
                return new LocalDateSegment<>(datoInterval, liste);
            }
        };
    }
    
    public static final class Kravprioritet implements Comparable<Kravprioritet> {
        private final Fagsak fagsak;
        private final LocalDateTime tidspunktForKrav;
        
        public Kravprioritet(Fagsak fagsak, LocalDateTime tidspunktForKrav) {
            this.fagsak = fagsak;
            this.tidspunktForKrav = tidspunktForKrav;
        }
        
        public Saksnummer getSaksnummer() {
            return fagsak.getSaksnummer();
        }
        
        public Fagsak getFagsak() {
            return fagsak;
        }
        
        public LocalDateTime getTidspunktForKrav() {
            return tidspunktForKrav;
        }
        
        public int compareTo(Kravprioritet other) {
            final int result = tidspunktForKrav.compareTo(other.tidspunktForKrav);
            if (result == 0) {
                return fagsak.getSaksnummer().compareTo(other.getFagsak().getSaksnummer());
            }
            return result;
        }
    }

}
