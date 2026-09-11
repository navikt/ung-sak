package no.nav.ung.ytelse.aktivitetspenger.historikkinnslag;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Dependent
public class VilkårsvurderingHistorikkinnslagTjeneste {

    private final BehandlingRepository behandlingRepository;
    private final HistorikkinnslagRepository historikkinnslagRepository;

    @Inject
    public VilkårsvurderingHistorikkinnslagTjeneste(BehandlingRepository behandlingRepository, HistorikkinnslagRepository historikkinnslagRepository) {
        this.behandlingRepository = behandlingRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    public void lagreHistorikkinnslag(HistorikkinnslagInput historikkinnslagInput) {
        Behandling behandling = behandlingRepository.hentBehandling(historikkinnslagInput.getBehandlingId());
        List<Historikkinnslag> innslagenee = lagHistorikkinnslag(behandling.getFagsakId(), historikkinnslagInput);
        innslagenee.forEach(historikkinnslagRepository::lagre);
    }

    public List<Historikkinnslag> lagHistorikkinnslag(Long fagsakId, HistorikkinnslagInput historikkinnslagInput) {
        Historikkinnslag.Builder historikkinnslagBuilder = lagBuilder(fagsakId, historikkinnslagInput);

        if (historikkinnslagInput.getEksisterendeVurderinger().equals(historikkinnslagInput.getNyeVurderinger())) {

            historikkinnslagBuilder.addLinje(HistorikkinnslagLinjeBuilder.plainTekstLinje("Vilkåret ble vurdert uten endringer i utfall."));
        } else if (historikkinnslagInput.getGjelderOpphør()) {
            LocalDateTimeline<HistorikkinnslagData> innvilgetTidsserie = historikkinnslagInput.getNyeVurderinger().filterValue(v -> v.utfall() == Utfall.OPPFYLT);
            LocalDateTimeline<HistorikkinnslagData> avslåttTidsserie = historikkinnslagInput.getNyeVurderinger().disjoint(innvilgetTidsserie);
            LocalDate sisteInnvilgedeDato = innvilgetTidsserie.isEmpty() ? null : innvilgetTidsserie.getMaxLocalDate();
            Avslagsårsak avslagsårsak = avslåttTidsserie.segmenter().getFirst().getValue().avslagsårsak();
            LocalDate opphørsdato = sisteInnvilgedeDato != null ? sisteInnvilgedeDato.plusDays(1) : historikkinnslagInput.getNyeVurderinger().getMinLocalDate();
            historikkinnslagBuilder.addLinje(HistorikkinnslagLinjeBuilder.plainTekstLinje("Opphørsdato satt til " + HistorikkinnslagLinjeBuilder.format(opphørsdato) + ". " + HistorikkinnslagLinjeBuilder.format(avslagsårsak)));
        } else {
            LocalDateTimeline<FørOgEtter<HistorikkinnslagData>> sammenligningTidslinje = historikkinnslagInput.getEksisterendeVurderinger().crossJoin(historikkinnslagInput.getNyeVurderinger(), (intervall, lhs, rhs) -> {
                HistorikkinnslagData lhsVerdi = lhs != null ? lhs.getValue() : null;
                HistorikkinnslagData rhsVerdi = rhs != null ? rhs.getValue() : null;
                return new LocalDateSegment<>(intervall, new FørOgEtter<>(lhsVerdi, rhsVerdi));
            });
            LocalDateTimeline<FørOgEtter<HistorikkinnslagData>> endretVurderingTidslinje = sammenligningTidslinje.filterValue(FørOgEtter::erEndret);
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
            if (endretVurderingTidslinje.isEmpty()) {
                historikkinnslagBuilder.addLinje(HistorikkinnslagLinjeBuilder.plainTekstLinje("Vilkåret ble vurdert uten endringer i utfall."));
            }
        }
        return List.of(historikkinnslagBuilder.build());
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
