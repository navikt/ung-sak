package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon;

import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon.InstitusjonGodkjenningStatus.GODKJENT;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon.InstitusjonGodkjenningStatus.IKKE_GODKJENT;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon.InstitusjonGodkjenningStatus.MANGLER_VURDERING;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.GodkjentOpplæringsinstitusjonTjeneste;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.GodkjentOpplæringsinstitusjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertInstitusjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.KursPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttaksPerioderGrunnlag;

public class VurderInstitusjonTjeneste {

    private final VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private final VurdertOpplæringRepository vurdertOpplæringRepository;
    private final GodkjentOpplæringsinstitusjonTjeneste godkjentOpplæringsinstitusjonTjeneste;
    private final UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;

    public VurderInstitusjonTjeneste(VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                                     VurdertOpplæringRepository vurdertOpplæringRepository,
                                     GodkjentOpplæringsinstitusjonTjeneste godkjentOpplæringsinstitusjonTjeneste,
                                     UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository) {
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.vurdertOpplæringRepository = vurdertOpplæringRepository;
        this.godkjentOpplæringsinstitusjonTjeneste = godkjentOpplæringsinstitusjonTjeneste;
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
    }

    public LocalDateTimeline<InstitusjonGodkjenningStatus> hentTidslinjeTilVurderingMedInstitusjonsGodkjenning(Long behandlingsId) {

        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(behandlingsId, VilkårType.NØDVENDIG_OPPLÆRING);

        LocalDateTimeline<InstitusjonGodkjenningStatus> tidslinjeTilVurdering = TidslinjeUtil.tilTidslinjeKomprimert(perioderTilVurdering)
            .map(segment -> List.of(new LocalDateSegment<>(segment.getLocalDateInterval(), MANGLER_VURDERING)));

        LocalDateTimeline<InstitusjonGodkjenningStatus> tidslinjeMedInstitusjonsgodkjenning = lagTidslinjeMedInstitusjonsGodkjenning(behandlingsId);

        return tidslinjeTilVurdering.combine(tidslinjeMedInstitusjonsgodkjenning, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.LEFT_JOIN);
    }

    private LocalDateTimeline<InstitusjonGodkjenningStatus> lagTidslinjeMedInstitusjonsGodkjenning(Long behandlingId) {
        Optional<VurdertOpplæringGrunnlag> vurdertOpplæringGrunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandlingId);
        LocalDateTimeline<InstitusjonFraSøknad> tidslinjeMedInstitusjonsnavn = hentTidslinjeMedInstitusjonFraSøknad(behandlingId);

        return tidslinjeMedInstitusjonsnavn.map(segment -> {
            Optional<GodkjentOpplæringsinstitusjon> godkjentOpplæringsinstitusjon =
                segment.getValue().getUuid() != null ?
                    godkjentOpplæringsinstitusjonTjeneste.hentMedUuid(segment.getValue().getUuid())
                    : Optional.empty();

            if (godkjentOpplæringsinstitusjon.isPresent()) {
                LocalDateTimeline<InstitusjonFraSøknad> tidslinje = new LocalDateTimeline<>(List.of(segment));

                LocalDateTimeline<InstitusjonGodkjenningStatus> godkjentTidslinje = godkjentOpplæringsinstitusjon.get().getTidslinje().intersection(tidslinje).mapValue(v -> GODKJENT);
                LocalDateTimeline<InstitusjonGodkjenningStatus> ikkeGodkjentTidslinje = tidslinje.disjoint(godkjentTidslinje).mapValue(v -> IKKE_GODKJENT);

                List<LocalDateSegment<InstitusjonGodkjenningStatus>> alleSegmenter = new ArrayList<>();
                alleSegmenter.addAll(godkjentTidslinje.toSegments());
                alleSegmenter.addAll(ikkeGodkjentTidslinje.toSegments());
                return alleSegmenter;

            } else if (vurdertOpplæringGrunnlag.isPresent()) {
                Optional<VurdertInstitusjon> vurdertInstitusjon = vurdertOpplæringGrunnlag.get().getVurdertInstitusjonHolder().finnVurdertInstitusjon(segment.getValue().getNavn());

                if (vurdertInstitusjon.isPresent()) {
                    InstitusjonGodkjenningStatus godkjenning = vurdertInstitusjon.get().getGodkjent() ? GODKJENT : IKKE_GODKJENT;
                    return List.of(new LocalDateSegment<>(segment.getLocalDateInterval(), godkjenning));
                }
            }

            return List.of(new LocalDateSegment<>(segment.getLocalDateInterval(), MANGLER_VURDERING));
        });
    }

    private LocalDateTimeline<InstitusjonFraSøknad> hentTidslinjeMedInstitusjonFraSøknad(Long behandlingId) {
        Optional<UttaksPerioderGrunnlag> uttaksPerioderGrunnlag = uttakPerioderGrunnlagRepository.hentGrunnlag(behandlingId);

        if (uttaksPerioderGrunnlag.isPresent()) {
            Set<PerioderFraSøknad> perioderFraSøknad = uttaksPerioderGrunnlag.get().getRelevantSøknadsperioder().getPerioderFraSøknadene();

            Set<KursPeriode> kursperioderFraSøknad = new LinkedHashSet<>();
            perioderFraSøknad.forEach(periodeFraSøknad -> kursperioderFraSøknad.addAll(periodeFraSøknad.getKurs()));

            return new LocalDateTimeline<>(kursperioderFraSøknad.stream()
                .map(kursPeriode -> new LocalDateSegment<>(kursPeriode.getPeriode().getFomDato(), kursPeriode.getPeriode().getTomDato(),
                    new InstitusjonFraSøknad(kursPeriode.getInstitusjon(), kursPeriode.getInstitusjonUuid())))
                .collect(Collectors.toCollection(TreeSet::new)));
        }
        throw new IllegalArgumentException("Fant ingen opplæringsinstitusjon på søknaden.");
    }

    private static class InstitusjonFraSøknad {
        private final String navn;
        private final UUID uuid;

        InstitusjonFraSøknad(String navn, UUID uuid) {
            this.navn = navn;
            this.uuid = uuid;
        }

        String getNavn() {
            return navn;
        }

        UUID getUuid() {
            return uuid;
        }
    }
}
