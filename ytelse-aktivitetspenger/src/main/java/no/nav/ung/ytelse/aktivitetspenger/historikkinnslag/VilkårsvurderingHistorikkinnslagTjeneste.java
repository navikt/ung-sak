package no.nav.ung.ytelse.aktivitetspenger.historikkinnslag;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Dependent
public class VilkårsvurderingHistorikkinnslagTjeneste {

    private final BehandlingRepository behandlingRepository;
    private final HistorikkinnslagRepository historikkinnslagRepository;
    private final InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;

    @Inject
    public VilkårsvurderingHistorikkinnslagTjeneste(BehandlingRepository behandlingRepository,
                                                    HistorikkinnslagRepository historikkinnslagRepository,
                                                    InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository) {
        this.behandlingRepository = behandlingRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.inngangsvilkårVurderingRepository = inngangsvilkårVurderingRepository;
    }

    /**
     * denne kan brukes for å fylle ut opprinnelige verdier i HistorikkinnslagInput. Må kalles før nye verdier lagres
     */
    public HistorikkinnslagInput hentInitielleVerdier(Long behandlingId, VilkårType vilkårType) {
        HistorikkinnslagInput input = new HistorikkinnslagInput();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        input.setBehandlingId(behandlingId);
        input.setEksisterendeVilkårVurderinger(inngangsvilkårVurderingRepository.hentVurderingTidslinje(behandlingId, vilkårType));
        input.setVedtatteVilkårVurderinger(behandling.getOriginalBehandlingId().map(id -> inngangsvilkårVurderingRepository.hentVurderingTidslinje(id, vilkårType)).orElse(LocalDateTimeline.empty()));
        return input;
    }

    public void lagreHistorikkinnslag(HistorikkinnslagInput historikkinnslagInput) {
        Behandling behandling = behandlingRepository.hentBehandling(historikkinnslagInput.getBehandlingId());
        List<Historikkinnslag> innslagenee = lagHistorikkinnslag(behandling.getFagsakId(), historikkinnslagInput);
        innslagenee.forEach(historikkinnslagRepository::lagre);
    }

    public List<Historikkinnslag> lagHistorikkinnslag(Long fagsakId, HistorikkinnslagInput historikkinnslagInput) {
        Historikkinnslag.Builder historikkinnslagBuilder = lagBuilder(fagsakId, historikkinnslagInput);

        LocalDateTimeline<FørOgEtter<HistorikkinnslagData>> førOgEtterTidslinje = lagFørOgEtterTidslinje(historikkinnslagInput);
        LocalDateTimeline<FørOgEtter<HistorikkinnslagData>> endretVurderingTidslinje = førOgEtterTidslinje.filterValue(FørOgEtter::erEndret);

        if (endretVurderingTidslinje.isEmpty()) {
            historikkinnslagBuilder.addLinje(HistorikkinnslagLinjeBuilder.plainTekstLinje("Vilkåret ble vurdert uten endringer i utfall."));
        } else if (opphørErEnesteEndring(historikkinnslagInput)) {
            LocalDateTimeline<HistorikkinnslagData> innvilgetTidsserie = historikkinnslagInput.getNyeVurderinger().filterValue(v -> v.utfall() == Utfall.OPPFYLT);
            LocalDateTimeline<HistorikkinnslagData> avslåttTidsserie = historikkinnslagInput.getNyeVurderinger().disjoint(innvilgetTidsserie);
            LocalDate sisteInnvilgedeDato = innvilgetTidsserie.isEmpty() ? null : innvilgetTidsserie.getMaxLocalDate();
            LocalDateSegment<HistorikkinnslagData> sisteEksisterendeSegment = historikkinnslagInput.getEksisterendeVurderinger().segmenter().stream().max(Comparator.naturalOrder()).orElse(null);
            boolean varOpphørFør = sisteEksisterendeSegment != null && sisteEksisterendeSegment.getValue().utfall() == Utfall.IKKE_OPPFYLT;
            if (avslåttTidsserie.isEmpty()) {
                historikkinnslagBuilder.addLinje(HistorikkinnslagLinjeBuilder.plainTekstLinje("Opphør ble fjernet"));
            } else {
                Avslagsårsak avslagsårsak = avslåttTidsserie.segmenter().getFirst().getValue().avslagsårsak();
                LocalDate opphørsdato = sisteInnvilgedeDato != null ? sisteInnvilgedeDato.plusDays(1) : historikkinnslagInput.getNyeVurderinger().getMinLocalDate();
                String tekst = varOpphørFør ? "Opphørsdato endret til " : "Opphørsdato satt til ";
                historikkinnslagBuilder.addLinje(HistorikkinnslagLinjeBuilder.plainTekstLinje(tekst + HistorikkinnslagLinjeBuilder.format(opphørsdato) + ". " + HistorikkinnslagLinjeBuilder.format(avslagsårsak)));
            }
        } else {
            endretVurderingTidslinje.segmenter().forEach(segment -> {
                HistorikkinnslagData før = segment.getValue().før;
                HistorikkinnslagData etter = segment.getValue().etter;
                boolean utfallEndret = før != null && etter != null && før.utfall() != etter.utfall();
                boolean årsakEndret = før != null && etter != null && før.avslagsårsak() != etter.avslagsårsak();
                String tekst;
                if (utfallEndret) {
                    tekst = "Vurdering ble endret for perioden " + HistorikkinnslagLinjeBuilder.format(segment.getLocalDateInterval()) + " til " + HistorikkinnslagLinjeBuilder.format(etter.utfall()) + ".";
                    if (etter.avslagsårsak() != null) {
                        tekst += " " + HistorikkinnslagLinjeBuilder.format(etter.avslagsårsak());
                    }
                } else if (årsakEndret) {
                    tekst = "Avslagsårsak ble endret for perioden " + HistorikkinnslagLinjeBuilder.format(segment.getLocalDateInterval()) + " til " + HistorikkinnslagLinjeBuilder.format(etter.avslagsårsak());
                } else if (etter == null) {
                    tekst = "Perioden " + HistorikkinnslagLinjeBuilder.format(segment.getLocalDateInterval()) + " har ikke lenger en vurdering";
                } else {
                    tekst = "Perioden " + HistorikkinnslagLinjeBuilder.format(segment.getLocalDateInterval()) + " ble vurdert til " + HistorikkinnslagLinjeBuilder.format(etter.utfall()) + ".";
                    if (etter.avslagsårsak() != null) {
                        tekst += " " + HistorikkinnslagLinjeBuilder.format(etter.avslagsårsak());
                    }
                }
                historikkinnslagBuilder.addLinje(HistorikkinnslagLinjeBuilder.plainTekstLinje(tekst));
            });
        }
        return List.of(historikkinnslagBuilder.build());
    }

    private boolean opphørErEnesteEndring(HistorikkinnslagInput historikkinnslagInput) {
        LocalDateTimeline<FørOgEtter<HistorikkinnslagData>> forrigeTilEksisterende = lagFørOgEtterTidslinje(historikkinnslagInput.getVedtatteVilkårVurderinger(), historikkinnslagInput.getNyeVurderinger());
        LocalDateTimeline<FørOgEtter<HistorikkinnslagData>> forrigeTilNy = lagFørOgEtterTidslinje(historikkinnslagInput.getVedtatteVilkårVurderinger(), historikkinnslagInput.getNyeVurderinger());

        return (erUendret(forrigeTilNy) || erOpphørUtenAndreEndringer(forrigeTilNy))
            && (erUendret(forrigeTilEksisterende) || erOpphørUtenAndreEndringer(forrigeTilEksisterende));
    }

    private boolean erUendret(LocalDateTimeline<FørOgEtter<HistorikkinnslagData>> sammensatt) {
        return sammensatt.filterValue(FørOgEtter::erEndret).isEmpty();
    }

    private boolean erOpphørUtenAndreEndringer(LocalDateTimeline<FørOgEtter<HistorikkinnslagData>> sammensatt) {
        LocalDateTimeline<FørOgEtter<HistorikkinnslagData>> endretTidslinje = sammensatt.filterValue(FørOgEtter::erEndret).compress();
        if (endretTidslinje.segmenter().size() > 1) {
            return false;
        }
        boolean sistePeriodeErEndret = endretTidslinje.getMaxLocalDate().equals(sammensatt.getMaxLocalDate());
        if (!sistePeriodeErEndret) {
            return false;
        }
        LocalDateSegment<FørOgEtter<HistorikkinnslagData>> segmentet = endretTidslinje.segmenter().getFirst();
        return segmentet.getValue().før != null
            && segmentet.getValue().før.utfall() == Utfall.OPPFYLT
            && segmentet.getValue().etter != null
            && segmentet.getValue().etter.utfall() == Utfall.IKKE_OPPFYLT;
    }

    private static LocalDateTimeline<FørOgEtter<HistorikkinnslagData>> lagFørOgEtterTidslinje(HistorikkinnslagInput historikkinnslagInput) {
        return lagFørOgEtterTidslinje(historikkinnslagInput.getEksisterendeVurderinger(), historikkinnslagInput.getNyeVurderinger());
    }

    private static LocalDateTimeline<FørOgEtter<HistorikkinnslagData>> lagFørOgEtterTidslinje(LocalDateTimeline<HistorikkinnslagData> før, LocalDateTimeline<HistorikkinnslagData> etter) {
        return før.crossJoin(etter, (intervall, lhs, rhs) -> {
            HistorikkinnslagData lhsVerdi = lhs != null ? lhs.getValue() : null;
            HistorikkinnslagData rhsVerdi = rhs != null ? rhs.getValue() : null;
            return new LocalDateSegment<>(intervall, new FørOgEtter<>(lhsVerdi, rhsVerdi));
        });
    }

    private Historikkinnslag.Builder lagBuilder(long fagsakId, HistorikkinnslagInput input) {
        Historikkinnslag.Builder builder = new Historikkinnslag.Builder()
            .medFagsakId(fagsakId)
            .medBehandlingId(input.getBehandlingId())
            .medAktør(input.getHistorikkAktør());
        if (input.getSkjermlenkeType() != null) {
            builder.medTittel(input.getSkjermlenkeType());
        } else {
            builder.medTittel(input.getVilkårType().getNavn());
        }
        return builder;
    }

    record FørOgEtter<T>(T før, T etter) {
        boolean erEndret() {
            return !Objects.equals(før, etter);
        }
    }

}
