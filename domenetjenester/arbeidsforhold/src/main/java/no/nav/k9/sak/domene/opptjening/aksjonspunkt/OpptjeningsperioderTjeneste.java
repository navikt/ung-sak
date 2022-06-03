package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import static no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType.NÆRING;
import static no.nav.k9.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningResultat;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtale;
import no.nav.k9.sak.domene.iay.modell.AktørArbeid;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.k9.sak.domene.iay.modell.OppgittArbeidsforhold;
import no.nav.k9.sak.domene.iay.modell.OppgittEgenNæring;
import no.nav.k9.sak.domene.iay.modell.OppgittFrilansoppdrag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.domene.iay.modell.YtelseFilter;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilter;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilterProvider;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetVurdering;
import no.nav.k9.sak.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.k9.sak.domene.opptjening.VurderingsStatus;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Stillingsprosent;

@Dependent
public class OpptjeningsperioderTjeneste {

    protected OpptjeningRepository opptjeningRepository;
    protected OppgittOpptjeningFilterProvider oppgittOpptjeningFilterProvider;
    private final MapYtelseperioderTjeneste mapYtelseperioderTjeneste;

    @Inject
    public OpptjeningsperioderTjeneste(OpptjeningRepository opptjeningRepository,
                                       OppgittOpptjeningFilterProvider oppgittOpptjeningFilterProvider) {
        this.opptjeningRepository = opptjeningRepository;
        this.oppgittOpptjeningFilterProvider = oppgittOpptjeningFilterProvider;
        this.mapYtelseperioderTjeneste = new MapYtelseperioderTjeneste();
    }

    public List<OpptjeningsperiodeForSaksbehandling> mapPerioderForSaksbehandling(BehandlingReferanse ref,
                                                                                  InntektArbeidYtelseGrunnlag grunnlag,
                                                                                  OpptjeningAktivitetVurdering vurderOpptjening,
                                                                                  DatoIntervallEntitet opptjeningPeriode,
                                                                                  DatoIntervallEntitet vilkårsPeriode,
                                                                                  YrkesaktivitetFilter yrkesaktivitetFilter) {
        List<OpptjeningsperiodeForSaksbehandling> perioder = new ArrayList<>();

        var aktørId = ref.getAktørId();
        var skjæringstidspunkt = opptjeningPeriode.getTomDato().plusDays(1);
        var oppgittOpptjeningFilter = oppgittOpptjeningFilterProvider.finnOpptjeningFilter(ref.getBehandlingId());
        var oppgittOpptjening = oppgittOpptjeningFilter.hentOppgittOpptjening(ref.getBehandlingId(), grunnlag, skjæringstidspunkt).orElse(null);

        var ytelseFilter = new YtelseFilter(grunnlag.getAktørYtelseFraRegister(aktørId)).før(opptjeningPeriode.getTomDato());
        var ytelsesperioder = mapYtelseperioderTjeneste.mapYtelsePerioder(ref, vurderOpptjening, false, ytelseFilter);
        var mapArbeidOpptjening = OpptjeningAktivitetType.hentFraArbeidTypeRelasjoner();
        var tidslinjePerYtelse = utledYtelsesTidslinjer(ytelsesperioder);
        for (var yrkesaktivitet : yrkesaktivitetFilter.getYrkesaktiviteter()) {
            var opptjeningsperioder = MapYrkesaktivitetTilOpptjeningsperiodeTjeneste.mapYrkesaktivitet(ref, yrkesaktivitet, grunnlag, vurderOpptjening, mapArbeidOpptjening, opptjeningPeriode, vilkårsPeriode, tidslinjePerYtelse);
            perioder.addAll(opptjeningsperioder);
        }

        perioder.addAll(mapOppgittOpptjening(mapArbeidOpptjening, oppgittOpptjening, vurderOpptjening, ref));
        perioder.addAll(mapYtelseperioderTjeneste.mapYtelsePerioder(ref, vurderOpptjening, true, ytelseFilter));
        lagOpptjeningsperiodeForFrilansAktivitet(ref, oppgittOpptjening, vurderOpptjening, grunnlag, perioder, opptjeningPeriode,
            mapArbeidOpptjening).ifPresent(perioder::add);

        return perioder.stream().sorted(Comparator.comparing(OpptjeningsperiodeForSaksbehandling::getPeriode)).collect(Collectors.toList());
    }

    private Map<OpptjeningAktivitetType, LocalDateTimeline<Boolean>> utledYtelsesTidslinjer(List<OpptjeningsperiodeForSaksbehandling> ytelsesperioder) {
        var gruppertPåYtelse = ytelsesperioder.stream()
            .collect(Collectors.groupingBy(OpptjeningsperiodeForSaksbehandling::getOpptjeningAktivitetType));
        var timelinePerYtelse = new HashMap<OpptjeningAktivitetType, LocalDateTimeline<Boolean>>();

        for (Map.Entry<OpptjeningAktivitetType, List<OpptjeningsperiodeForSaksbehandling>> entry : gruppertPåYtelse.entrySet()) {
            var segmenter = entry.getValue().stream().map(it -> new LocalDateSegment<>(it.getPeriode().toLocalDateInterval(), true)).collect(Collectors.toSet());
            var timeline = new LocalDateTimeline<Boolean>(List.of());

            for (LocalDateSegment<Boolean> segment : segmenter) {
                timeline = timeline.combine(segment, StandardCombinators::alwaysTrueForMatch, LocalDateTimeline.JoinStyle.CROSS_JOIN);
            }
            timelinePerYtelse.put(entry.getKey(), timeline.compress());
        }

        return timelinePerYtelse;
    }

    public Optional<OpptjeningResultat> hentOpptjeningHvisFinnes(Long behandlingId) {
        return opptjeningRepository.finnOpptjening(behandlingId);
    }

    private List<OpptjeningsperiodeForSaksbehandling> mapOppgittOpptjening(Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening, OppgittOpptjening oppgittOpptjening, OpptjeningAktivitetVurdering vurderOpptjening, BehandlingReferanse ref) {
        List<OpptjeningsperiodeForSaksbehandling> oppgittOpptjeningPerioder = new ArrayList<>();
        if (oppgittOpptjening != null) {
            for (Map.Entry<ArbeidType, List<OppgittAnnenAktivitet>> annenAktivitet : oppgittOpptjening.getAnnenAktivitet().stream()
                .collect(Collectors.groupingBy(OppgittAnnenAktivitet::getArbeidType)).entrySet()) {
                oppgittOpptjeningPerioder.addAll(mapAnnenAktivitet(annenAktivitet, mapArbeidOpptjening, vurderOpptjening, ref));
            }
            oppgittOpptjening.getOppgittArbeidsforhold()
                .forEach(oppgittArbeidsforhold -> oppgittOpptjeningPerioder.add(mapOppgittArbeidsforholdUtenOverstyring(oppgittArbeidsforhold,
                    mapArbeidOpptjening, vurderOpptjening, ref)));
            oppgittOpptjening.getEgenNæring().forEach(egenNæring -> oppgittOpptjeningPerioder.add(mapEgenNæring(egenNæring, vurderOpptjening, ref)));
        }
        return oppgittOpptjeningPerioder;
    }

    private OpptjeningsperiodeForSaksbehandling mapOppgittArbeidsforholdUtenOverstyring(OppgittArbeidsforhold oppgittArbeidsforhold,
                                                                                        Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening, OpptjeningAktivitetVurdering vurderOpptjening, BehandlingReferanse ref) {
        final OpptjeningAktivitetType type = utledOpptjeningType(mapArbeidOpptjening, oppgittArbeidsforhold.getArbeidType());
        VurderingsStatus status = vurderOpptjening.vurderStatus(new VurderStatusInput(type, ref));
        return mapOppgittArbeidsperiode(oppgittArbeidsforhold, type, status);
    }

    private OpptjeningsperiodeForSaksbehandling mapOppgittArbeidsperiode(OppgittArbeidsforhold oppgittArbeidsforhold, OpptjeningAktivitetType type, VurderingsStatus status) {
        final OpptjeningsperiodeForSaksbehandling.Builder builder = OpptjeningsperiodeForSaksbehandling.Builder.ny();
        DatoIntervallEntitet periode = oppgittArbeidsforhold.getPeriode();
        builder.medOpptjeningAktivitetType(type)
            .medPeriode(periode)
            .medArbeidsgiverUtlandNavn(oppgittArbeidsforhold.getUtenlandskVirksomhet().getNavn())
            .medVurderingsStatus(status);
        return builder.build();
    }

    private List<OpptjeningsperiodeForSaksbehandling> mapAnnenAktivitet(Map.Entry<ArbeidType, List<OppgittAnnenAktivitet>> annenAktivitet,
                                                                        Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening, OpptjeningAktivitetVurdering vurderOpptjening,
                                                                        BehandlingReferanse ref) {
        OpptjeningAktivitetType opptjeningAktivitetType = utledOpptjeningType(mapArbeidOpptjening, annenAktivitet.getKey());
        List<OpptjeningsperiodeForSaksbehandling> annenAktivitetPerioder = new ArrayList<>();
        for (OppgittAnnenAktivitet aktivitet : annenAktivitet.getValue()) {
            OpptjeningsperiodeForSaksbehandling.Builder builder = OpptjeningsperiodeForSaksbehandling.Builder.ny();
            VurderingsStatus status = vurderOpptjening.vurderStatus(new VurderStatusInput(opptjeningAktivitetType, ref));
            builder.medPeriode(aktivitet.getPeriode())
                .medOpptjeningAktivitetType(opptjeningAktivitetType)
                .medVurderingsStatus(status);
            annenAktivitetPerioder.add(builder.build());
        }
        return annenAktivitetPerioder;
    }

    private OpptjeningAktivitetType utledOpptjeningType(Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening, ArbeidType arbeidType) {
        return mapArbeidOpptjening.get(arbeidType)
            .stream()
            .findFirst()
            .orElse(OpptjeningAktivitetType.UDEFINERT);
    }

    private OpptjeningsperiodeForSaksbehandling mapEgenNæring(OppgittEgenNæring egenNæring, OpptjeningAktivitetVurdering vurderOpptjening, BehandlingReferanse ref) {
        VurderingsStatus status = vurderOpptjening.vurderStatus(new VurderStatusInput(NÆRING, ref));
        OpptjeningsperiodeForSaksbehandling.Builder builder = OpptjeningsperiodeForSaksbehandling.Builder.ny()
            .medOpptjeningAktivitetType(NÆRING)
            .medVurderingsStatus(status)
            .medPeriode(egenNæring.getPeriode());
        if (egenNæring.getOrgnr() != null) {
            builder.medOpptjeningsnøkkel(new Opptjeningsnøkkel(null, egenNæring.getOrgnr(), null))
                .medArbeidsgiver(Arbeidsgiver.virksomhet(egenNæring.getOrgnr()));
        }
        builder.medStillingsandel(new Stillingsprosent(BigDecimal.valueOf(100)));
        return builder.build();
    }

    private Optional<OpptjeningsperiodeForSaksbehandling> lagOpptjeningsperiodeForFrilansAktivitet(BehandlingReferanse behandlingReferanse,
                                                                                                   OppgittOpptjening oppgittOpptjening,
                                                                                                   OpptjeningAktivitetVurdering vurderOpptjening,
                                                                                                   InntektArbeidYtelseGrunnlag grunnlag,
                                                                                                   List<OpptjeningsperiodeForSaksbehandling> perioder,
                                                                                                   DatoIntervallEntitet periode,
                                                                                                   Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening) {
        // Hvis oppgitt frilansaktivitet brukes perioden derfra og det er allerede laget en OFS.
        if (oppgittOpptjening != null && oppgittOpptjening.getAnnenAktivitet().stream().anyMatch(oaa -> ArbeidType.FRILANSER.equals(oaa.getArbeidType())) ||
            perioder.stream().anyMatch(oaa -> OpptjeningAktivitetType.FRILANS.equals(oaa.getOpptjeningAktivitetType()))) {
            return Optional.empty();
        }

        AktørId aktørId = behandlingReferanse.getAktørId();
        Optional<AktørArbeid> aktørArbeidFraRegister = grunnlag.getAktørArbeidFraRegister(aktørId);
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), aktørArbeidFraRegister).før(periode.getTomDato());

        Collection<Yrkesaktivitet> frilansOppdrag = filter.getFrilansOppdrag();

        if (aktørArbeidFraRegister.isPresent() && !frilansOppdrag.isEmpty()) {

            DatoIntervallEntitet frilansPeriode = finnFrilansPeriode(oppgittOpptjening, periode, frilansOppdrag);

            OpptjeningAktivitetType brukType = utledOpptjeningType(mapArbeidOpptjening, ArbeidType.FRILANSER);
            VurderingsStatus status = vurderOpptjening.vurderStatus(new VurderStatusInput(brukType, behandlingReferanse));
            return Optional.of(OpptjeningsperiodeForSaksbehandling.Builder.ny()
                .medOpptjeningAktivitetType(brukType)
                .medVurderingsStatus(status)
                .medPeriode(frilansPeriode)
                .build());
        }
        return Optional.empty();
    }

    protected DatoIntervallEntitet finnFrilansPeriode(OppgittOpptjening oppgittOpptjening, DatoIntervallEntitet periode, Collection<Yrkesaktivitet> frilansOppdrag) {
        Optional<DatoIntervallEntitet> frilansPeriodeFraOppdrag = finnPeriodeFraOppdrag(frilansOppdrag);
        Optional<DatoIntervallEntitet> frilansperiodeFraSøknad = finnOppgittFrilansperiode(oppgittOpptjening);
        var frilansPeriode = frilansperiodeFraSøknad.orElse(frilansPeriodeFraOppdrag.orElse(periode));
        return frilansPeriode;
    }

    private Optional<DatoIntervallEntitet> finnPeriodeFraOppdrag(Collection<Yrkesaktivitet> frilansOppdrag) {
        List<DatoIntervallEntitet> oppdragsperioderFrilans = frilansOppdrag.stream().flatMap(y -> y.getAnsettelsesPeriode().stream())
            .map(AktivitetsAvtale::getPeriode)
            .collect(Collectors.toList());
        return finnMinMaxAvAllePerioder(oppdragsperioderFrilans);
    }

    private Optional<DatoIntervallEntitet> finnOppgittFrilansperiode(OppgittOpptjening oppgittOpptjening) {
        if (oppgittOpptjening != null) {
            List<DatoIntervallEntitet> oppgittFrilansOppdrag = oppgittOpptjening.getFrilans().stream()
                .flatMap(oo -> oo.getFrilansoppdrag().stream())
                .map(OppgittFrilansoppdrag::getPeriode)
                .collect(Collectors.toList());

            return finnMinMaxAvAllePerioder(oppgittFrilansOppdrag);


        }
        return Optional.empty();
    }

    private Optional<DatoIntervallEntitet> finnMinMaxAvAllePerioder(List<DatoIntervallEntitet> perioder) {
        Optional<LocalDate> startDatoFrilans = perioder.stream()
            .map(DatoIntervallEntitet::getFomDato)
            .min(Comparator.naturalOrder());

        var sluttDatoFrilans = perioder.stream()
            .map(DatoIntervallEntitet::getTomDato)
            .max(Comparator.naturalOrder())
            .orElse(TIDENES_ENDE);


        return startDatoFrilans.map(startDato -> DatoIntervallEntitet.fraOgMedTilOgMed(startDato, sluttDatoFrilans));
    }

}
