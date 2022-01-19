package no.nav.k9.sak.domene.opptjening.aksjonspunkt.ytelse;

import static no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType.NÆRING;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningResultat;
import no.nav.k9.sak.domene.iay.modell.AktørArbeid;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.InntektFilter;
import no.nav.k9.sak.domene.iay.modell.Inntektspost;
import no.nav.k9.sak.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.k9.sak.domene.iay.modell.OppgittArbeidsforhold;
import no.nav.k9.sak.domene.iay.modell.OppgittEgenNæring;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilterProvider;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetVurdering;
import no.nav.k9.sak.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.k9.sak.domene.opptjening.VurderingsStatus;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.MapYrkesaktivitetTilOpptjeningsperiodeTjeneste;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.MapYtelseperioderTjeneste;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.VurderStatusInput;
import no.nav.k9.sak.domene.opptjening.ytelse.OpptjeningsperiodeForSaksbehandlingFRISINN;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Stillingsprosent;

@Dependent
public class OpptjeningsperioderTjenesteFRISINN {

    protected OpptjeningRepository opptjeningRepository;
    protected OppgittOpptjeningFilterProvider oppgittOpptjeningFilterProvider;
    private MapYtelseperioderTjeneste mapYtelseperioderTjeneste;

    public OpptjeningsperioderTjenesteFRISINN() {
        // CDI
    }

    @Inject
    public OpptjeningsperioderTjenesteFRISINN(OpptjeningRepository opptjeningRepository, OppgittOpptjeningFilterProvider oppgittOpptjeningFilterProvider) {
        this.opptjeningRepository = opptjeningRepository;
        this.oppgittOpptjeningFilterProvider = oppgittOpptjeningFilterProvider;
        this.mapYtelseperioderTjeneste = new MapYtelseperioderTjeneste();
    }

    public List<OpptjeningsperiodeForSaksbehandling> mapPerioderForSaksbehandling(BehandlingReferanse ref,
                                                                                  InntektArbeidYtelseGrunnlag grunnlag,
                                                                                  OppgittOpptjening oppgittOpptjening,
                                                                                  OpptjeningAktivitetVurdering vurderOpptjening,
                                                                                  DatoIntervallEntitet opptjeningPeriode) {
        List<OpptjeningsperiodeForSaksbehandling> perioder = new ArrayList<>();

        var aktørId = ref.getAktørId();
        var skjæringstidspunkt = opptjeningPeriode.getTomDato().plusDays(1);

        var mapArbeidOpptjening = OpptjeningAktivitetType.hentFraArbeidTypeRelasjoner();
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(aktørId)).før(skjæringstidspunkt);
        for (var yrkesaktivitet : filter.getYrkesaktiviteter()) {
            var opptjeningsperioder = MapYrkesaktivitetTilOpptjeningsperiodeTjeneste.mapYrkesaktivitet(ref, yrkesaktivitet, grunnlag, vurderOpptjening, mapArbeidOpptjening, opptjeningPeriode);
            perioder.addAll(opptjeningsperioder);
        }

        perioder.addAll(mapOppgittOpptjening(mapArbeidOpptjening, oppgittOpptjening, vurderOpptjening, opptjeningPeriode, ref));
        perioder.addAll(mapYtelseperioderTjeneste.mapYtelsePerioder(ref, grunnlag, vurderOpptjening, opptjeningPeriode));
        lagOpptjeningsperiodeForFrilansAktivitet(ref, oppgittOpptjening, vurderOpptjening, grunnlag, perioder, opptjeningPeriode,
            mapArbeidOpptjening).ifPresent(perioder::add);

        return perioder.stream().sorted(Comparator.comparing(OpptjeningsperiodeForSaksbehandling::getPeriode)).collect(Collectors.toList());
    }

    public Optional<OpptjeningResultat> hentOpptjeningHvisFinnes(Long behandlingId) {
        return opptjeningRepository.finnOpptjening(behandlingId);
    }

    private List<OpptjeningsperiodeForSaksbehandling> mapOppgittOpptjening(Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening, OppgittOpptjening oppgittOpptjening, OpptjeningAktivitetVurdering vurderOpptjening, DatoIntervallEntitet opptjeningPeriode, BehandlingReferanse ref) {
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
            oppgittOpptjening.getFrilans().ifPresent(fl -> oppgittOpptjeningPerioder.add(mapFl(ref, vurderOpptjening, opptjeningPeriode, mapArbeidOpptjening)));
        }
        return oppgittOpptjeningPerioder;
    }

    private OpptjeningsperiodeForSaksbehandling mapOppgittArbeidsforholdUtenOverstyring(OppgittArbeidsforhold oppgittArbeidsforhold,
                                                                                        Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening, OpptjeningAktivitetVurdering vurderOpptjening, BehandlingReferanse ref) {
        final OpptjeningAktivitetType type = utledOpptjeningType(mapArbeidOpptjening, oppgittArbeidsforhold.getArbeidType());
        VurderingsStatus status = vurderOpptjening.vurderStatus(new VurderStatusInput(type, ref));
        return mapOppgittArbeidsperiode(oppgittArbeidsforhold, type, status);
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

        var inntektFilter = new InntektFilter(grunnlag.getAktørInntektFraRegister(aktørId)).før(periode.getTomDato()).filterPensjonsgivende();

        Collection<Yrkesaktivitet> frilansOppdrag = filter.getFrilansOppdrag();

        if (aktørArbeidFraRegister.isPresent() && !inntektFilter.getFiltrertInntektsposter().isEmpty() && !frilansOppdrag.isEmpty()) {
            List<Yrkesaktivitet> frilansMedInntekt = frilansOppdrag.stream()
                .filter(frilans -> harInntektFraVirksomhetForPeriode(frilans, inntektFilter, periode))
                .collect(Collectors.toList());
            if (frilansMedInntekt.isEmpty()) {
                return Optional.empty();
            }
            OpptjeningsperiodeForSaksbehandling fl = mapFl(behandlingReferanse, vurderOpptjening, periode, mapArbeidOpptjening);
            return Optional.of(fl);
        }
        return Optional.empty();
    }

    private OpptjeningsperiodeForSaksbehandling mapFl(BehandlingReferanse behandlingReferanse, OpptjeningAktivitetVurdering vurderOpptjening, DatoIntervallEntitet periode, Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening) {
        OpptjeningAktivitetType brukType = utledOpptjeningType(mapArbeidOpptjening, ArbeidType.FRILANSER);
        VurderingsStatus status = vurderOpptjening.vurderStatus(new VurderStatusInput(brukType, behandlingReferanse));
        return OpptjeningsperiodeForSaksbehandling.Builder.ny()
            .medOpptjeningAktivitetType(brukType)
            .medVurderingsStatus(status)
            .medPeriode(periode)
            .build();
    }

    private boolean harInntektFraVirksomhetForPeriode(Yrkesaktivitet frilans, InntektFilter inntektFilter, DatoIntervallEntitet opptjeningsPeriode) {
        return inntektFilter
            .filter(i -> frilans.getArbeidsgiver().equals(i.getArbeidsgiver()))
            .anyMatchFilter((i, ip) -> harInntektpostForPeriode(ip, opptjeningsPeriode));
    }

    private boolean harInntektpostForPeriode(Inntektspost ip, DatoIntervallEntitet opptjeningsPeriode) {
        return opptjeningsPeriode.overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(ip.getPeriode().getFomDato(), ip.getPeriode().getTomDato()));
    }

    private OpptjeningsperiodeForSaksbehandling mapOppgittArbeidsperiode(OppgittArbeidsforhold oppgittArbeidsforhold, OpptjeningAktivitetType type, VurderingsStatus status) {
        final OpptjeningsperiodeForSaksbehandlingFRISINN.Builder builder = new OpptjeningsperiodeForSaksbehandlingFRISINN.Builder();
        DatoIntervallEntitet periode = oppgittArbeidsforhold.getPeriode();
        builder.medOpptjeningAktivitetType(type)
            .medVurderingsStatus(status)
            .medPeriode(periode);
        return builder.build();
    }
}
