package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.TimelineMerger;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsPeriodeDokumenter;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeRepository;


@ApplicationScoped
@FagsakYtelseTypeRef("PSB")
public class PSBVurdererSøknadsfristTjeneste implements VurderSøknadsfristTjeneste<Søknadsperiode> {

    private final DefaultSøknadsfristPeriodeVurderer vurderer = new DefaultSøknadsfristPeriodeVurderer();

    private SøknadsperiodeRepository søknadsperiodeRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;

    PSBVurdererSøknadsfristTjeneste() {
        // CDI
    }

    @Inject
    public PSBVurdererSøknadsfristTjeneste(SøknadsperiodeRepository søknadsperiodeRepository, MottatteDokumentRepository mottatteDokumentRepository) {
        this.søknadsperiodeRepository = søknadsperiodeRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
    }

    @Override
    public Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> vurderSøknadsfrist(BehandlingReferanse referanse) {
        var søktePerioder = hentPerioderTilVurdering(referanse);

        return vurderSøknadsfrist(søktePerioder);
    }

    @Override
    public Map<KravDokument, List<SøktPeriode<Søknadsperiode>>> hentPerioderTilVurdering(BehandlingReferanse referanse) {
        var result = new TreeMap<KravDokument, List<SøktPeriode<Søknadsperiode>>>();

        var mottatteDokumenter = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(referanse.getFagsakId())
            .stream()
            .filter(it -> Brevkode.PLEIEPENGER_SOKNAD.equals(it.getType()))
            .collect(Collectors.toSet());

        if (mottatteDokumenter.isEmpty()) {
            return result;
        }

        var mottatteJournalposter = mottatteDokumenter.stream()
            .map(MottattDokument::getJournalpostId)
            .collect(Collectors.toSet());

        søknadsperiodeRepository.hentPerioderKnyttetTilJournalpost(referanse.getBehandlingId(), mottatteJournalposter)
            .forEach(dokument -> mapTilKravDokumentOgPeriode(result, mottatteDokumenter, dokument));

        return result;
    }

    private void mapTilKravDokumentOgPeriode(Map<KravDokument, List<SøktPeriode<Søknadsperiode>>> result, Set<MottattDokument> mottatteDokumenter, SøknadsPeriodeDokumenter dokument) {
        var innsendingstidspunkt = utledInnsendingstidspunkt(dokument, mottatteDokumenter);
        var kravDokument = new KravDokument(dokument.getJournalpostId(), innsendingstidspunkt, KravDokumentType.SØKNAD);
        var søktePerioder = dokument.getSøknadsperioder().stream().map(it -> new SøktPeriode<>(it.getPeriode(), it)).collect(Collectors.toList());

        result.put(kravDokument, søktePerioder);
    }

    private LocalDateTime utledInnsendingstidspunkt(SøknadsPeriodeDokumenter dokument, Set<MottattDokument> mottatteDokumenter) {
        return mottatteDokumenter.stream()
            .filter(it -> it.getJournalpostId().equals(dokument.getJournalpostId()))
            .findFirst()
            .map(MottattDokument::getInnsendingstidspunkt)
            .orElseThrow();
    }

    @Override
    public Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> vurderSøknadsfrist(Map<KravDokument, List<SøktPeriode<Søknadsperiode>>> søknaderMedPerioder) {
        var result = new HashMap<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>>();
        var sortedKeys = søknaderMedPerioder.keySet()
            .stream()
            .sorted(Comparator.comparing(KravDokument::getInnsendingsTidspunkt))
            .collect(Collectors.toCollection(LinkedHashSet::new));
        sortedKeys.forEach(key -> {
            var value = søknaderMedPerioder.get(key);
            var timeline = new LocalDateTimeline<>(value.stream().map(it -> new LocalDateSegment<>(it.getPeriode().getFomDato(), it.getPeriode().getTomDato(), it)).collect(Collectors.toList()));
            var vurdertTimeline = vurderer.vurderPeriode(key, timeline);

            if (vurdertTimeline.stream().anyMatch(it -> EnumSet.of(Utfall.IKKE_OPPFYLT, Utfall.IKKE_VURDERT).contains(it.getValue().getUtfall()))) {
                var skalEndresUtfallPå = utledAvslåttePerioderSomHarTidligereVærtInnvilget(result, key, vurdertTimeline);
                vurdertTimeline = vurdertTimeline.combine(skalEndresUtfallPå, TimelineMerger::mergeSegments, LocalDateTimeline.JoinStyle.CROSS_JOIN);
            }

            result.put(key, vurdertTimeline.compress()
                .stream()
                .map(this::konsistens)
                .collect(Collectors.toList()));
        });

        return result;
    }

    private LocalDateTimeline<VurdertSøktPeriode<Søknadsperiode>> utledAvslåttePerioderSomHarTidligereVærtInnvilget(HashMap<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> result,
                                                                                                                    KravDokument key,
                                                                                                                    LocalDateTimeline<VurdertSøktPeriode<Søknadsperiode>> vurdertTimeline) {

        var tidligereGodkjentTimeline = hentUtTidligereGodkjent(result, key);

        var avslått = new LocalDateTimeline<>(vurdertTimeline.toSegments()
            .stream()
            .filter(it -> EnumSet.of(Utfall.IKKE_OPPFYLT, Utfall.IKKE_VURDERT).contains(it.getValue().getUtfall()))
            .collect(Collectors.toSet()));

        var skalGodkjennes = avslått.intersection(tidligereGodkjentTimeline)
            .toSegments()
            .stream()
            .map(this::konsistens)
            .peek(it -> it.justerUtfall(Utfall.OPPFYLT))
            .map(it -> new LocalDateSegment<>(it.getPeriode().getFomDato(), it.getPeriode().getTomDato(), it))
            .collect(Collectors.toSet());

        return new LocalDateTimeline<>(skalGodkjennes);
    }

    private LocalDateTimeline<VurdertSøktPeriode<Søknadsperiode>> hentUtTidligereGodkjent(HashMap<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> result,
                                                                                          KravDokument key) {
        var tidligereGodkjentTimeline = new LocalDateTimeline<VurdertSøktPeriode<Søknadsperiode>>(List.of());
        var godkjentePerioder = result.entrySet()
            .stream()
            .filter(it -> key.getType().equals(it.getKey().getType()))
            .map(it -> it.getValue()
                .stream()
                .filter(at -> Utfall.OPPFYLT.equals(at.getUtfall()))
                .map(at -> new LocalDateSegment<>(at.getPeriode().getFomDato(), at.getPeriode().getTomDato(), at))
                .collect(Collectors.toList()))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        for (LocalDateSegment<VurdertSøktPeriode<Søknadsperiode>> segment : godkjentePerioder) {
            var other = new LocalDateTimeline<>(List.of(segment));
            tidligereGodkjentTimeline = tidligereGodkjentTimeline.combine(other, TimelineMerger::mergeSegments, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }

        return tidligereGodkjentTimeline;
    }

    private VurdertSøktPeriode<Søknadsperiode> konsistens(LocalDateSegment<VurdertSøktPeriode<Søknadsperiode>> segment) {
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(segment.getFom(), segment.getTom());
        var fraværPeriode = new Søknadsperiode(segment.getFom(), segment.getTom());

        return new VurdertSøktPeriode<>(periode, fraværPeriode);
    }
}
