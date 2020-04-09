package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

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

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

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
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetVurdering;
import no.nav.k9.sak.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Stillingsprosent;

@Dependent
public class OpptjeningsperioderUtenOverstyringTjeneste {

    private OpptjeningRepository opptjeningRepository;
    private MapYtelseperioderTjeneste mapYtelseperioderTjeneste;

    @Inject
    public OpptjeningsperioderUtenOverstyringTjeneste(OpptjeningRepository opptjeningRepository) {
        this.opptjeningRepository = opptjeningRepository;
        this.mapYtelseperioderTjeneste = new MapYtelseperioderTjeneste();
    }

    public List<OpptjeningsperiodeForSaksbehandling> mapPerioderForSaksbehandling(BehandlingReferanse behandlingReferanse,
                                                                                  InntektArbeidYtelseGrunnlag grunnlag,
                                                                                  OpptjeningAktivitetVurdering vurderOpptjening, DatoIntervallEntitet opptjeningPeriode) {
        AktørId aktørId = behandlingReferanse.getAktørId();
        List<OpptjeningsperiodeForSaksbehandling> perioder = new ArrayList<>();

        var mapArbeidOpptjening = OpptjeningAktivitetType.hentFraArbeidTypeRelasjoner();
        var skjæringstidspunkt = opptjeningPeriode.getTomDato().plusDays(1);
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(aktørId)).før(skjæringstidspunkt);
        for (var yrkesaktivitet : filter.getYrkesaktiviteterForBeregning()) {
            var opptjeningsperioder = MapYrkesaktivitetTilOpptjeningsperiodeTjeneste.mapYrkesaktivitet(
                behandlingReferanse, yrkesaktivitet, grunnlag, vurderOpptjening, mapArbeidOpptjening, null, opptjeningPeriode);
            perioder.addAll(opptjeningsperioder);
        }

        var oppgittOpptjening = grunnlag.getOppgittOpptjening();
        perioder.addAll(mapOppgittOpptjening(mapArbeidOpptjening, oppgittOpptjening));
        perioder.addAll(mapYtelseperioderTjeneste.mapYtelsePerioder(behandlingReferanse, grunnlag, vurderOpptjening, opptjeningPeriode));
        lagOpptjeningsperiodeForFrilansAktivitet(behandlingReferanse, oppgittOpptjening.orElse(null), grunnlag, perioder, opptjeningPeriode,
            mapArbeidOpptjening).ifPresent(perioder::add);

        return perioder.stream().sorted(Comparator.comparing(OpptjeningsperiodeForSaksbehandling::getPeriode)).collect(Collectors.toList());
    }

    public Optional<OpptjeningResultat> hentOpptjeningHvisFinnes(Long behandlingId) {
        return opptjeningRepository.finnOpptjening(behandlingId);
    }

    private List<OpptjeningsperiodeForSaksbehandling> mapOppgittOpptjening(Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening,
                                                                           Optional<OppgittOpptjening> oppgittOpptjening) {
        List<OpptjeningsperiodeForSaksbehandling> oppgittOpptjeningPerioder = new ArrayList<>();
        if (oppgittOpptjening.isPresent()) {
            // map
            final OppgittOpptjening opptjening = oppgittOpptjening.get();
            for (Map.Entry<ArbeidType, List<OppgittAnnenAktivitet>> annenAktivitet : opptjening.getAnnenAktivitet().stream()
                .collect(Collectors.groupingBy(OppgittAnnenAktivitet::getArbeidType)).entrySet()) {
                oppgittOpptjeningPerioder.addAll(mapAnnenAktivitet(annenAktivitet, mapArbeidOpptjening));
            }
            opptjening.getOppgittArbeidsforhold() // .filter(utenlandskArbforhold -> utenlandskArbforhold.getArbeidType().equals(ArbeidType.UDEFINERT))
                .forEach(oppgittArbeidsforhold -> oppgittOpptjeningPerioder.add(mapOppgittArbeidsforholdUtenOverstyring(oppgittArbeidsforhold,
                    mapArbeidOpptjening)));
            opptjening.getEgenNæring().forEach(egenNæring -> oppgittOpptjeningPerioder.add(mapEgenNæring(egenNæring)));
        }
        return oppgittOpptjeningPerioder;
    }

    private OpptjeningsperiodeForSaksbehandling mapOppgittArbeidsforholdUtenOverstyring(OppgittArbeidsforhold oppgittArbeidsforhold,
                                                                                        Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening) {
        final OpptjeningAktivitetType type = utledOpptjeningType(mapArbeidOpptjening, oppgittArbeidsforhold.getArbeidType());
        return mapOppgittArbeidsperiode(oppgittArbeidsforhold, type);
    }

    private OpptjeningsperiodeForSaksbehandling mapOppgittArbeidsperiode(OppgittArbeidsforhold oppgittArbeidsforhold, OpptjeningAktivitetType type) {
        final OpptjeningsperiodeForSaksbehandling.Builder builder = OpptjeningsperiodeForSaksbehandling.Builder.ny();
        DatoIntervallEntitet periode = oppgittArbeidsforhold.getPeriode();
        builder.medOpptjeningAktivitetType(type)
            .medPeriode(periode)
            .medArbeidsgiverUtlandNavn(oppgittArbeidsforhold.getUtenlandskVirksomhet().getNavn());
        return builder.build();
    }

    private List<OpptjeningsperiodeForSaksbehandling> mapAnnenAktivitet(Map.Entry<ArbeidType, List<OppgittAnnenAktivitet>> annenAktivitet,
                                                                        Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening) {
        OpptjeningAktivitetType opptjeningAktivitetType = utledOpptjeningType(mapArbeidOpptjening, annenAktivitet.getKey());
        List<OpptjeningsperiodeForSaksbehandling> annenAktivitetPerioder = new ArrayList<>();
        for (OppgittAnnenAktivitet aktivitet : annenAktivitet.getValue()) {
            OpptjeningsperiodeForSaksbehandling.Builder builder = OpptjeningsperiodeForSaksbehandling.Builder.ny();
            builder.medPeriode(aktivitet.getPeriode())
                .medOpptjeningAktivitetType(opptjeningAktivitetType);
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

    private OpptjeningsperiodeForSaksbehandling mapEgenNæring(OppgittEgenNæring egenNæring) {
        final OpptjeningsperiodeForSaksbehandling.Builder builder = OpptjeningsperiodeForSaksbehandling.Builder.ny()
            .medOpptjeningAktivitetType(NÆRING);
        builder.medPeriode(egenNæring.getPeriode());
        if (egenNæring.getOrgnr() != null) {
            builder.medOpptjeningsnøkkel(new Opptjeningsnøkkel(null, egenNæring.getOrgnr(), null))
                .medArbeidsgiver(Arbeidsgiver.virksomhet(egenNæring.getOrgnr()));
        }
        builder.medStillingsandel(new Stillingsprosent(BigDecimal.valueOf(100)));
        return builder.build();
    }

    private Optional<OpptjeningsperiodeForSaksbehandling> lagOpptjeningsperiodeForFrilansAktivitet(BehandlingReferanse behandlingReferanse,
                                                                                                   OppgittOpptjening oppgittOpptjening,
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
            OpptjeningAktivitetType brukType = utledOpptjeningType(mapArbeidOpptjening, ArbeidType.FRILANSER);
            return frilansMedInntekt.isEmpty() ? Optional.empty()
                : Optional.of(OpptjeningsperiodeForSaksbehandling.Builder.ny()
                .medOpptjeningAktivitetType(brukType)
                .medPeriode(periode)
                .build());
        }
        return Optional.empty();
    }

    private boolean harInntektFraVirksomhetForPeriode(Yrkesaktivitet frilans, InntektFilter inntektFilter, DatoIntervallEntitet opptjeningsPeriode) {
        return inntektFilter
            .filter(i -> frilans.getArbeidsgiver().equals(i.getArbeidsgiver()))
            .anyMatchFilter((i, ip) -> harInntektpostForPeriode(ip, opptjeningsPeriode));
    }

    private boolean harInntektpostForPeriode(Inntektspost ip, DatoIntervallEntitet opptjeningsPeriode) {
        return opptjeningsPeriode.overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(ip.getPeriode().getFomDato(), ip.getPeriode().getTomDato()));
    }
}
