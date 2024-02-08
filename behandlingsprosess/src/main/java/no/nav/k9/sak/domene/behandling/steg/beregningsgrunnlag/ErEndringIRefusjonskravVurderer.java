package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import static no.nav.k9.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.behandling.steg.kompletthet.KompletthetForBeregningTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Beløp;

@ApplicationScoped
public class ErEndringIRefusjonskravVurderer {


    private BehandlingRepository behandlingRepository;
    private KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private MottatteDokumentRepository mottatteDokumentRepository;

    public ErEndringIRefusjonskravVurderer() {
    }

    @Inject
    public ErEndringIRefusjonskravVurderer(BehandlingRepository behandlingRepository, KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste, InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste, MottatteDokumentRepository mottatteDokumentRepository) {
        this.behandlingRepository = behandlingRepository;
        this.kompletthetForBeregningTjeneste = kompletthetForBeregningTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
    }

    public LocalDateTimeline<Boolean> finnEndringstidslinjeForRefusjon(BehandlingReferanse referanse,
                                                                       DatoIntervallEntitet periode) {
        if (!referanse.erRevurdering()) {
            throw new IllegalArgumentException("Endringsutleder for refusjon skal kun kjøres i kontekst av en revurdering");
        }
        var originalBehandling = behandlingRepository.hentBehandling(referanse.getOriginalBehandlingId().orElseThrow());

        var inntektsmeldinger = inntektArbeidYtelseTjeneste.hentUnikeInntektsmeldingerForSak(referanse.getSaksnummer());
        var inntektsmeldingerForrigeVedtak = FinnInntektsmeldingForrigeBehandling.finnInntektsmeldingerFraForrigeBehandling(referanse, inntektsmeldinger, finnMottatteInntektsmeldinger(referanse));

        var relevanteInntektsmeldinger = kompletthetForBeregningTjeneste.utledInntektsmeldingerSomSendesInnTilBeregningForPeriode(referanse, inntektsmeldinger, periode);
        var relevanteInntektsmeldingerForrigeVedtak = kompletthetForBeregningTjeneste.utledInntektsmeldingerSomSendesInnTilBeregningForPeriode(BehandlingReferanse.fra(originalBehandling), inntektsmeldingerForrigeVedtak, periode);

        return ErEndringIRefusjonskravVurderer.finnEndringstidslinje(periode, relevanteInntektsmeldinger, relevanteInntektsmeldingerForrigeVedtak);
    }


    public static LocalDateTimeline<Boolean> finnEndringstidslinje(DatoIntervallEntitet vilkårsperiode, Collection<Inntektsmelding> gjeldendeInntektsmeldinger, Collection<Inntektsmelding> inntektsmeldingerForrigeVedtak) {
        return gjeldendeInntektsmeldinger.stream()
            .map(finnEndringIRefusjonTidslinje(vilkårsperiode, inntektsmeldingerForrigeVedtak))
            .reduce(LocalDateTimeline.empty(), (t1, t2) -> t1.crossJoin(t2, StandardCombinators::alwaysTrueForMatch));

    }

    private static Function<Inntektsmelding, LocalDateTimeline<Boolean>> finnEndringIRefusjonTidslinje(DatoIntervallEntitet vilkårsperiode, Collection<Inntektsmelding> inntektsmeldingerForrigeVedtak) {
        return im -> {
            var matchendeInntektsmeldingerFraForrigeVedtak = inntektsmeldingerForrigeVedtak.stream()
                .filter(imForrige -> matcher(im, imForrige))
                .findFirst();

            var tidslinje = lagRefusjontidslinje(im, vilkårsperiode);
            var forrigeTidslinje = matchendeInntektsmeldingerFraForrigeVedtak.map(imForrigeVedtak -> lagRefusjontidslinje(imForrigeVedtak, vilkårsperiode)).orElse(
                new LocalDateTimeline<>(vilkårsperiode.toLocalDateInterval(), Beløp.ZERO));


            var erRefusjonEndretTidslinje = tidslinje.intersection(forrigeTidslinje, (di, lhs, rhs) -> new LocalDateSegment<>(di, lhs.getValue().compareTo(rhs.getValue()) != 0));

            return erRefusjonEndretTidslinje.filterValue(v -> v);


        };
    }

    private static LocalDateTimeline<Beløp> lagRefusjontidslinje(Inntektsmelding im, DatoIntervallEntitet vilkårsperiode) {
        var tidslinje = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(vilkårsperiode.toLocalDateInterval(), im.getRefusjonBeløpPerMnd())));

        if (im.getEndringerRefusjon() != null) {

            tidslinje = tidslinje.intersection(lagEndringTidslinje(im), StandardCombinators::coalesceRightHandSide);
        }

        if (im.getRefusjonOpphører() != null && im.getRefusjonOpphører().isBefore(vilkårsperiode.getTomDato())) {
            tidslinje = tidslinje.intersection(new LocalDateTimeline<>(List.of(new LocalDateSegment<>(im.getRefusjonOpphører().plusDays(1), vilkårsperiode.getTomDato(), Beløp.ZERO))));
        }
        return tidslinje;
    }

    private static LocalDateTimeline<Beløp> lagEndringTidslinje(Inntektsmelding im) {
        return new LocalDateTimeline<>(im.getEndringerRefusjon()
            .stream()
            .map(e -> new LocalDateSegment<>(e.getFom(), TIDENES_ENDE, e.getRefusjonsbeløp()))
            .sorted(Comparator.naturalOrder())
            .toList(), StandardCombinators::coalesceRightHandSide);
    }

    private static boolean matcher(Inntektsmelding im, Inntektsmelding imForrige) {
        return imForrige.getArbeidsgiver().equals(im.getArbeidsgiver()) && imForrige.getArbeidsforholdRef().equals(im.getArbeidsforholdRef());
    }

    private List<MottattDokument> finnMottatteInntektsmeldinger(BehandlingReferanse ref) {
        return mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(ref.getFagsakId())
            .stream()
            .filter(it -> Objects.equals(Brevkode.INNTEKTSMELDING, it.getType()))
            .toList();
    }


}
