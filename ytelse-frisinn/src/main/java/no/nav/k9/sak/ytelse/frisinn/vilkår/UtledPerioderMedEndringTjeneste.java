package no.nav.k9.sak.ytelse.frisinn.vilkår;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.frisinn.filter.OppgittOpptjeningFilter;

@ApplicationScoped
public class UtledPerioderMedEndringTjeneste {


    private final DatoIntervallEntitet iFjor = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31));
    private final DatoIntervallEntitet startenAvÅret = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 3, 29));
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private BehandlingRepository behandlingRepository;

    public UtledPerioderMedEndringTjeneste() {
        //CDI
    }

    @Inject
    public UtledPerioderMedEndringTjeneste(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste, BehandlingRepository behandlingRepository) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    private static LocalDateSegment<Verdi> slåSammen(LocalDateInterval interval,
                                                     LocalDateSegment<Verdi> opprinneligVerdi,
                                                     LocalDateSegment<Verdi> tilkommetVerdi) {

        if (opprinneligVerdi != null && tilkommetVerdi != null) {
            Verdi opprinneligVerdiValue = opprinneligVerdi.getValue();
            Verdi tilkommetVerdiValue = tilkommetVerdi.getValue();
            return new LocalDateSegment<>(interval, new Verdi(DatoIntervallEntitet.fraOgMedTilOgMed(interval.getFomDato(), interval.getTomDato()), opprinneligVerdiValue.getVerdiSN(), opprinneligVerdiValue.getVerdiFL() == null ? tilkommetVerdiValue.getVerdiFL() : opprinneligVerdiValue.getVerdiFL(), opprinneligVerdiValue.getVerdiArb() == null ? tilkommetVerdiValue.getVerdiArb() : opprinneligVerdiValue.getVerdiArb()));
        } else if (opprinneligVerdi == null) {
            Verdi tilkommetVerdiValue = tilkommetVerdi.getValue();
            return new LocalDateSegment<>(interval, new Verdi(DatoIntervallEntitet.fraOgMedTilOgMed(interval.getFomDato(), interval.getTomDato()), tilkommetVerdiValue.getVerdiSN(), tilkommetVerdiValue.getVerdiFL(), tilkommetVerdiValue.getVerdiArb()));
        }
        Verdi opprinneligVerdiValue = opprinneligVerdi.getValue();
        return new LocalDateSegment<>(interval, new Verdi(DatoIntervallEntitet.fraOgMedTilOgMed(interval.getFomDato(), interval.getTomDato()), opprinneligVerdiValue.getVerdiSN(), opprinneligVerdiValue.getVerdiFL(), opprinneligVerdiValue.getVerdiArb()));
    }

    private static LocalDateSegment<Verdi> utLeddDiff(LocalDateInterval interval,
                                                      LocalDateSegment<Verdi> opprinneligVerdi,
                                                      LocalDateSegment<Verdi> tilkommetVerdi) {

        if (opprinneligVerdi != null && tilkommetVerdi != null) {
            Verdi opprinneligVerdiValue = opprinneligVerdi.getValue();
            Verdi tilkommetVerdiValue = tilkommetVerdi.getValue();

            if (opprinneligVerdiValue.equals(tilkommetVerdiValue)) {
                return null;
            }

            return new LocalDateSegment<>(interval, new Verdi(DatoIntervallEntitet.fraOgMedTilOgMed(interval.getFomDato(), interval.getTomDato()), opprinneligVerdiValue.getVerdiSN(), opprinneligVerdiValue.getVerdiFL() == null ? tilkommetVerdiValue.getVerdiFL() : opprinneligVerdiValue.getVerdiFL(), opprinneligVerdiValue.getVerdiArb() == null ? tilkommetVerdiValue.getVerdiArb() : opprinneligVerdiValue.getVerdiArb()));
        } else if (opprinneligVerdi == null) {
            Verdi tilkommetVerdiValue = tilkommetVerdi.getValue();
            return new LocalDateSegment<>(interval, new Verdi(DatoIntervallEntitet.fraOgMedTilOgMed(interval.getFomDato(), interval.getTomDato()), tilkommetVerdiValue.getVerdiSN(), tilkommetVerdiValue.getVerdiFL(), tilkommetVerdiValue.getVerdiArb()));
        }
        Verdi opprinneligVerdiValue = opprinneligVerdi.getValue();
        return new LocalDateSegment<>(interval, new Verdi(DatoIntervallEntitet.fraOgMedTilOgMed(interval.getFomDato(), interval.getTomDato()), opprinneligVerdiValue.getVerdiSN(), opprinneligVerdiValue.getVerdiFL(), opprinneligVerdiValue.getVerdiArb()));
    }

    /**
     * Sjekker behandlig mot tidligere behandling og utleder hvilke perioder som har fått endret opplysningene sine.
     * Intervallene trenger ikke være fulle måndeder, så man kan matche mot måned hvis man ønsker.
     *
     * @param behandlingId idene til behandlingen som man tar utgangspunkt i
     * @return DatoIntervallEntitet
     */
    public List<DatoIntervallEntitet> finnPeriodeMedEndring(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Optional<Behandling> originalBehandlingOpt = behandling.getOriginalBehandling();
        if (originalBehandlingOpt.isEmpty()) {
            return Collections.emptyList();
        }

        Behandling forrigeBehandling = originalBehandlingOpt.get();
        InntektArbeidYtelseGrunnlag forrigeGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(forrigeBehandling.getId()).orElseThrow(() -> new IllegalStateException("Forventet å finne et grunnlag for behandling:" + forrigeBehandling.getId()));
        InntektArbeidYtelseGrunnlag nyttGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId).orElseThrow(() -> new IllegalStateException("Forventet å finne et grunnlag for behandling:" + behandlingId));

        return utledDiffIperioder(forrigeGrunnlag, nyttGrunnlag);
    }

    //slik at den er testbar
    List<DatoIntervallEntitet> utledDiffIperioder(InntektArbeidYtelseGrunnlag forrigeGrunnlag, InntektArbeidYtelseGrunnlag nyttGrunnlag) {
        LocalDateTimeline<Verdi> forrige = byggTidslinje(forrigeGrunnlag);
        LocalDateTimeline<Verdi> ny = byggTidslinje(nyttGrunnlag);


        LocalDateTimeline<Verdi> tidslinjeMedDiff = forrige.combine(ny, UtledPerioderMedEndringTjeneste::utLeddDiff, JoinStyle.CROSS_JOIN);

        List<DatoIntervallEntitet> perioderMedDiff = tidslinjeMedDiff.getDatoIntervaller().stream().map(intervall -> {
            LocalDateSegment<Verdi> segment = tidslinjeMedDiff.getSegment(intervall);
            return segment.getValue().getPeriode();
        }).collect(Collectors.toList());

        //sjekker om det har blitt endring i felles opplysninger (opplysninger som gjelder 2019 eller starten av 2020 før covid-19)
        boolean harEndringIregister = perioderMedDiff.stream().anyMatch(p -> iFjor.inkluderer(p.getFomDato()) || startenAvÅret.inkluderer(p.getFomDato()));

        if (harEndringIregister) {
            //skal returnere alle perioder
            return forrige.getDatoIntervaller().stream()
                .filter(p -> (!iFjor.inkluderer(p.getFomDato()) || !startenAvÅret.inkluderer(p.getFomDato())))
                .map(intervall -> {
                    LocalDateSegment<Verdi> segment = forrige.getSegment(intervall);
                    return segment.getValue().getPeriode();
                }).collect(Collectors.toList());
        }

        return perioderMedDiff;
    }

    private LocalDateTimeline<Verdi> byggTidslinje(InntektArbeidYtelseGrunnlag grunnlag) {
        OppgittOpptjeningFilter filter = new OppgittOpptjeningFilter(grunnlag.getOppgittOpptjening(), grunnlag.getOverstyrtOppgittOpptjening());
        OppgittOpptjening oppgittOpptjeningFrisinn = filter.getOppgittOpptjeningFrisinn();

        List<LocalDateSegment<Verdi>> egenNæring = oppgittOpptjeningFrisinn.getEgenNæring().stream().map(egen -> new LocalDateSegment<>(egen.getFraOgMed(), egen.getTilOgMed(), new Verdi(egen.getPeriode(), egen.getBruttoInntekt(), null, null))).collect(Collectors.toList());
        List<LocalDateSegment<Verdi>> frilans = oppgittOpptjeningFrisinn.getFrilans().map(fl -> fl.getFrilansoppdrag().stream().map(floppdrag -> new LocalDateSegment<>(floppdrag.getPeriode().getFomDato(), floppdrag.getPeriode().getTomDato(), new Verdi(floppdrag.getPeriode(), null, floppdrag.getInntekt(), null))).collect(Collectors.toList())).stream().flatMap(Collection::stream).collect(Collectors.toList());
        List<LocalDateSegment<Verdi>> arb = oppgittOpptjeningFrisinn.getOppgittArbeidsforhold().stream().map(arbeidsforhold -> new LocalDateSegment<>(arbeidsforhold.getFraOgMed(), arbeidsforhold.getTilOgMed(), new Verdi(arbeidsforhold.getPeriode(), null, null, arbeidsforhold.getInntekt()))).collect(Collectors.toList());

        LocalDateTimeline<Verdi> egenTid = new LocalDateTimeline<>(egenNæring);
        LocalDateTimeline<Verdi> flTid = new LocalDateTimeline<>(frilans);
        LocalDateTimeline<Verdi> arbTid = new LocalDateTimeline<>(arb);

        return egenTid.combine(flTid, UtledPerioderMedEndringTjeneste::slåSammen, JoinStyle.CROSS_JOIN)
            .combine(arbTid, UtledPerioderMedEndringTjeneste::slåSammen, JoinStyle.CROSS_JOIN);
    }

    private static class Verdi {
        private final DatoIntervallEntitet periode;
        private final BigDecimal verdiSN;
        private final BigDecimal verdiFL;
        private final BigDecimal verdiArb;

        public Verdi(DatoIntervallEntitet periode, BigDecimal verdiSN, BigDecimal verdiFL, BigDecimal verdiArb) {
            this.periode = periode;
            this.verdiSN = verdiSN;
            this.verdiFL = verdiFL;
            this.verdiArb = verdiArb;
        }

        public DatoIntervallEntitet getPeriode() {
            return periode;
        }

        public BigDecimal getVerdiSN() {
            return verdiSN;
        }

        public BigDecimal getVerdiFL() {
            return verdiFL;
        }

        public BigDecimal getVerdiArb() {
            return verdiArb;
        }

        @Override
        public int hashCode() {
            return Objects.hash(periode, verdiSN, verdiFL, verdiArb);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Verdi verdi = (Verdi) o;
            return Objects.equals(periode, verdi.periode) &&
                Objects.equals(verdiSN, verdi.verdiSN) &&
                Objects.equals(verdiFL, verdi.verdiFL) &&
                Objects.equals(verdiArb, verdi.verdiArb);
        }
    }
}
