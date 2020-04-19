package no.nav.k9.sak.domene.opptjening;

import static no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType.NÆRING;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningResultat;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtale;
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
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.AksjonspunktutlederForVurderBekreftetOpptjening;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.AksjonspunktutlederForVurderOppgittOpptjening;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.MapYrkesaktivitetTilOpptjeningsperiodeTjeneste;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.MapYtelseperioderTjeneste;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.OpptjeningAktivitetVurderingAksjonspunkt;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.OpptjeningAktivitetVurderingVilkår;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Stillingsprosent;

@Dependent
public class OpptjeningsperioderTjeneste {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private OpptjeningRepository opptjeningRepository;
    private OpptjeningAktivitetVurdering vurderForSaksbehandling;
    private OpptjeningAktivitetVurdering vurderForVilkår;
    private MapYtelseperioderTjeneste mapYtelseperioderTjeneste;

    OpptjeningsperioderTjeneste() {
        // CDI
    }

    @Inject
    public OpptjeningsperioderTjeneste(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                       OpptjeningRepository opptjeningRepository,
                                       AksjonspunktutlederForVurderOppgittOpptjening vurderOppgittOpptjening,
                                       AksjonspunktutlederForVurderBekreftetOpptjening vurderBekreftetOpptjening) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.opptjeningRepository = opptjeningRepository;
        this.vurderForSaksbehandling = new OpptjeningAktivitetVurderingAksjonspunkt(vurderOppgittOpptjening, vurderBekreftetOpptjening);
        this.vurderForVilkår = new OpptjeningAktivitetVurderingVilkår(vurderOppgittOpptjening, vurderBekreftetOpptjening);
        this.mapYtelseperioderTjeneste = new MapYtelseperioderTjeneste();
    }

    /**
     * Hent alle opptjeningsaktiv fra et gitt grunnlag og utleder om noen perioder trenger vurdering av saksbehandler
     */
    public List<OpptjeningsperiodeForSaksbehandling> hentRelevanteOpptjeningAktiveterForSaksbehandling(BehandlingReferanse behandlingReferanse, UUID iayGrunnlagUuid, LocalDate skjæringstidspunkt) {

        var grunnlag = Optional.ofNullable(iayGrunnlagUuid).map(uuid -> inntektArbeidYtelseTjeneste.hentGrunnlagForGrunnlagId(behandlingReferanse.getBehandlingId(), uuid))
            .orElseGet(() -> inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingReferanse.getBehandlingId()).orElse(null));
        if (grunnlag == null) {
            return Collections.emptyList();
        }
        return mapOpptjeningsperiodeForSaksbehandling(behandlingReferanse, grunnlag, vurderForSaksbehandling, skjæringstidspunkt);
    }

    public List<OpptjeningsperiodeForSaksbehandling> hentRelevanteOpptjeningAktiveterForVilkårVurdering(BehandlingReferanse behandlingReferanse, LocalDate skjæringstidspunkt) {
        Optional<InntektArbeidYtelseGrunnlag> grunnlagOpt = inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingReferanse.getBehandlingId());
        if (grunnlagOpt.isPresent()) {
            return mapOpptjeningsperiodeForSaksbehandling(behandlingReferanse, grunnlagOpt.get(), vurderForVilkår, skjæringstidspunkt);
        }
        return Collections.emptyList();
    }

    private List<OpptjeningsperiodeForSaksbehandling> mapOpptjeningsperiodeForSaksbehandling(BehandlingReferanse behandlingReferanse,
                                                                                             InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag,
                                                                                             OpptjeningAktivitetVurdering vurderForSaksbehandling, LocalDate skjæringstidspunkt) {
        return mapPerioderForSaksbehandling(behandlingReferanse, inntektArbeidYtelseGrunnlag, vurderForSaksbehandling, skjæringstidspunkt);
    }

    private List<OpptjeningsperiodeForSaksbehandling> mapPerioderForSaksbehandling(BehandlingReferanse behandlingReferanse,
                                                                                   InntektArbeidYtelseGrunnlag grunnlag,
                                                                                   OpptjeningAktivitetVurdering vurderOpptjening,
                                                                                   LocalDate skjæringstidspunkt) {

        var opptjeningResultat = opptjeningRepository.finnOpptjening(behandlingReferanse.getBehandlingId());
        var opptjening = opptjeningResultat.flatMap(it -> it.finnOpptjening(skjæringstidspunkt)).orElseThrow();

        AktørId aktørId = behandlingReferanse.getAktørId();
        List<OpptjeningsperiodeForSaksbehandling> perioder = new ArrayList<>();

        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(aktørId)).før(opptjening.getTom());

        Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening = OpptjeningAktivitetType.hentFraArbeidTypeRelasjoner();
        for (Yrkesaktivitet yrkesaktivitet : filter.getYrkesaktiviteter()) {
            mapYrkesaktivitet(behandlingReferanse, perioder, yrkesaktivitet, grunnlag, vurderOpptjening, mapArbeidOpptjening, opptjening.getOpptjeningPeriode());
        }

        final Optional<OppgittOpptjening> optOppgittOpptjening = grunnlag.getOppgittOpptjening();
        if (optOppgittOpptjening.isPresent()) {
            // map
            final OppgittOpptjening oppgittOpptjening = optOppgittOpptjening.get();
            for (Map.Entry<ArbeidType, List<OppgittAnnenAktivitet>> annenAktivitet : oppgittOpptjening.getAnnenAktivitet().stream()
                .collect(Collectors.groupingBy(OppgittAnnenAktivitet::getArbeidType)).entrySet()) {
                mapAnnenAktivitet(perioder, annenAktivitet, grunnlag, behandlingReferanse, vurderOpptjening, mapArbeidOpptjening, opptjening.getOpptjeningPeriode());
            }
            oppgittOpptjening.getOppgittArbeidsforhold() // .filter(utenlandskArbforhold -> utenlandskArbforhold.getArbeidType().equals(ArbeidType.UDEFINERT))
                .forEach(oppgittArbeidsforhold -> perioder.add(mapOppgittArbeidsforhold(oppgittArbeidsforhold, grunnlag,
                    behandlingReferanse, vurderOpptjening, mapArbeidOpptjening, opptjening.getOpptjeningPeriode())));

            oppgittOpptjening.getEgenNæring().forEach(egenNæring -> {
                OpptjeningsperiodeForSaksbehandling periode = mapEgenNæring(egenNæring, grunnlag, behandlingReferanse, vurderOpptjening, opptjening.getOpptjeningPeriode());
                perioder.add(periode);
            });
        }
        perioder.addAll(mapYtelseperioderTjeneste.mapYtelsePerioder(behandlingReferanse, grunnlag, vurderOpptjening, opptjening.getOpptjeningPeriode()));

        var filterSaksbehandlet = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getBekreftetAnnenOpptjening(aktørId));

        håndterManueltLagtTilAktiviteter(behandlingReferanse, grunnlag, vurderOpptjening, perioder, filterSaksbehandlet, mapArbeidOpptjening, opptjening.getOpptjeningPeriode());

        lagOpptjeningsperiodeForFrilansAktivitet(behandlingReferanse, optOppgittOpptjening.orElse(null), grunnlag, perioder, opptjening.getOpptjeningPeriode(),
            mapArbeidOpptjening, vurderOpptjening).ifPresent(perioder::add);

        return perioder.stream().sorted(Comparator.comparing(OpptjeningsperiodeForSaksbehandling::getPeriode)).collect(Collectors.toList());
    }

    private void håndterManueltLagtTilAktiviteter(BehandlingReferanse behandlingReferanse, InntektArbeidYtelseGrunnlag grunnlag,
                                                  OpptjeningAktivitetVurdering vurderOpptjening,
                                                  List<OpptjeningsperiodeForSaksbehandling> perioder, YrkesaktivitetFilter filter,
                                                  Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening, DatoIntervallEntitet opptjeningPeriode) {
        filter.getYrkesaktiviteter()
            .stream()
            .filter(Yrkesaktivitet::erArbeidsforhold)
            .forEach(yr -> filter.getAnsettelsesPerioder(yr).forEach(avtale -> {
                if (perioder.stream().noneMatch(
                    p -> p.getOpptjeningAktivitetType().equals(utledOpptjeningType(mapArbeidOpptjening, yr.getArbeidType()))
                        && p.getPeriode().equals(avtale.getPeriode()))) {
                    leggTilManuelleAktiviteter(yr, avtale, perioder, behandlingReferanse, grunnlag, vurderOpptjening, mapArbeidOpptjening, opptjeningPeriode);
                }
            }));
        filter.getYrkesaktiviteter()
            .stream()
            .filter(yr -> !yr.erArbeidsforhold())
            .forEach(yr -> filter.getAktivitetsAvtalerForArbeid(yr).stream().filter(av -> perioder.stream()
                .noneMatch(p -> p.getOpptjeningAktivitetType().equals(utledOpptjeningType(mapArbeidOpptjening, yr.getArbeidType())) &&
                    p.getPeriode().equals(av.getPeriode())))
                .forEach(avtale -> leggTilManuelleAktiviteter(yr, avtale, perioder, behandlingReferanse, grunnlag, vurderOpptjening, mapArbeidOpptjening, opptjeningPeriode)));
    }

    private void leggTilManuelleAktiviteter(Yrkesaktivitet yr, AktivitetsAvtale avtale, List<OpptjeningsperiodeForSaksbehandling> perioder,
                                            BehandlingReferanse behandlingReferanse, InntektArbeidYtelseGrunnlag grunnlag,
                                            OpptjeningAktivitetVurdering vurderOpptjening, Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening, DatoIntervallEntitet opptjeningPeriode) {
        final OpptjeningAktivitetType type = utledOpptjeningType(mapArbeidOpptjening, yr.getArbeidType());
        OpptjeningsperiodeForSaksbehandling.Builder builder = OpptjeningsperiodeForSaksbehandling.Builder.ny();
        builder.medPeriode(avtale.getPeriode())
            .medOpptjeningAktivitetType(type)
            .medBegrunnelse(avtale.getBeskrivelse())
            .medVurderingsStatus(vurderOpptjening.vurderStatus(type, behandlingReferanse, yr, grunnlag, grunnlag.harBlittSaksbehandlet(), opptjeningPeriode));
        yr.getStillingsprosentFor(opptjeningPeriode.getTomDato()).ifPresent(builder::medStillingsandel);
        MapYrkesaktivitetTilOpptjeningsperiodeTjeneste.settArbeidsgiverInformasjon(yr, builder);
        harSaksbehandlerVurdert(builder, type, behandlingReferanse, null, vurderOpptjening, grunnlag, opptjeningPeriode);
        builder.medErManueltRegistrert();
        perioder.add(builder.build());
    }

    private OpptjeningsperiodeForSaksbehandling mapOppgittArbeidsforhold(OppgittArbeidsforhold oppgittArbeidsforhold, InntektArbeidYtelseGrunnlag grunnlag,
                                                                         BehandlingReferanse behandlingReferanse, OpptjeningAktivitetVurdering vurderOpptjening,
                                                                         Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening, DatoIntervallEntitet opptjeningPeriode) {
        final OpptjeningAktivitetType type = utledOpptjeningType(mapArbeidOpptjening, oppgittArbeidsforhold.getArbeidType());

        AktørId aktørId = behandlingReferanse.getAktørId();
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getBekreftetAnnenOpptjening(aktørId));

        final Yrkesaktivitet overstyrt = finnTilsvarende(filter, oppgittArbeidsforhold.getArbeidType(), oppgittArbeidsforhold.getPeriode()).orElse(null);
        return mapOppgittArbeidsperiode(oppgittArbeidsforhold, grunnlag, behandlingReferanse, vurderOpptjening, type, overstyrt, opptjeningPeriode);
    }

    private OpptjeningsperiodeForSaksbehandling mapOppgittArbeidsperiode(OppgittArbeidsforhold oppgittArbeidsforhold, InntektArbeidYtelseGrunnlag grunnlag,
                                                                         BehandlingReferanse behandlingReferanse, OpptjeningAktivitetVurdering vurderOpptjening,
                                                                         OpptjeningAktivitetType type, Yrkesaktivitet overstyrt, DatoIntervallEntitet opptjeningPeriode) {
        final OpptjeningsperiodeForSaksbehandling.Builder builder = OpptjeningsperiodeForSaksbehandling.Builder.ny();
        DatoIntervallEntitet periode = utledPeriode(oppgittArbeidsforhold.getPeriode(), overstyrt);
        builder.medOpptjeningAktivitetType(type)
            .medPeriode(periode)
            .medArbeidsgiverUtlandNavn(oppgittArbeidsforhold.getUtenlandskVirksomhet().getNavn())
            .medVurderingsStatus(vurderOpptjening.vurderStatus(type, behandlingReferanse, overstyrt, grunnlag, grunnlag.harBlittSaksbehandlet(), opptjeningPeriode));

        if (harEndretPåPeriode(oppgittArbeidsforhold.getPeriode(), overstyrt)) {
            builder.medErPeriodenEndret();
        }

        if (overstyrt != null) {
            new YrkesaktivitetFilter(null, List.of(overstyrt)).getAktivitetsAvtalerForArbeid()
                .stream()
                .filter(aa -> aa.getPeriode().equals(periode))
                .findFirst()
                .ifPresent(aa -> builder.medBegrunnelse(aa.getBeskrivelse()));
        }
        return builder.build();
    }

    private Optional<Yrkesaktivitet> finnTilsvarende(YrkesaktivitetFilter filter, Yrkesaktivitet registerAktivitet) {
        if (filter.getYrkesaktiviteter().isEmpty()) {
            return Optional.empty();
        }
        return filter.getYrkesaktiviteter().stream().filter(yr -> matcher(yr, registerAktivitet)).findFirst();
    }

    private Optional<Yrkesaktivitet> finnTilsvarende(YrkesaktivitetFilter filter, ArbeidType arbeidType, DatoIntervallEntitet periode) {
        return filter.getYrkesaktiviteter().stream()
            .filter(yr -> matcher(yr, arbeidType)
                && inneholderPeriode(yr, periode))
            .findFirst();
    }

    private boolean inneholderPeriode(Yrkesaktivitet yr, DatoIntervallEntitet periode) {
        return new YrkesaktivitetFilter(null, List.of(yr)).getAktivitetsAvtalerForArbeid().stream().anyMatch(aa -> aa.getPeriode().overlapper(periode));
    }

    private boolean matcher(Yrkesaktivitet saksbehandlet, Yrkesaktivitet registerAktivitet) {
        if (!saksbehandlet.getArbeidType().equals(registerAktivitet.getArbeidType())) {
            return false;
        }
        return saksbehandlet.gjelderFor(registerAktivitet.getArbeidsgiver(), registerAktivitet.getArbeidsforholdRef());
    }

    private boolean matcher(Yrkesaktivitet yr, ArbeidType type) {
        return yr.getArbeidType().equals(type);
    }

    private void mapYrkesaktivitet(BehandlingReferanse behandlingReferanse,
                                   List<OpptjeningsperiodeForSaksbehandling> perioder,
                                   Yrkesaktivitet registerAktivitet,
                                   InntektArbeidYtelseGrunnlag grunnlag,
                                   OpptjeningAktivitetVurdering vurderForSaksbehandling,
                                   Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening,
                                   DatoIntervallEntitet opptjeningPeriode) {
        AktørId aktørId = behandlingReferanse.getAktørId();
        var filter = new YrkesaktivitetFilter(null, grunnlag.getBekreftetAnnenOpptjening(aktørId));

        var overstyrtAktivitet = finnTilsvarende(filter, registerAktivitet).orElse(null);
        var opptjeningsperioderForSaksbehandling = MapYrkesaktivitetTilOpptjeningsperiodeTjeneste.mapYrkesaktivitet(behandlingReferanse,
            registerAktivitet, grunnlag, vurderForSaksbehandling, mapArbeidOpptjening, overstyrtAktivitet, opptjeningPeriode);
        perioder.addAll(opptjeningsperioderForSaksbehandling);
    }

    private void harSaksbehandlerVurdert(OpptjeningsperiodeForSaksbehandling.Builder builder, OpptjeningAktivitetType type,
                                         BehandlingReferanse behandlingReferanse, Yrkesaktivitet registerAktivitet,
                                         OpptjeningAktivitetVurdering vurderForSaksbehandling, InntektArbeidYtelseGrunnlag grunnlag, DatoIntervallEntitet opptjeningPeriode) {
        if (vurderForSaksbehandling.vurderStatus(type, behandlingReferanse, registerAktivitet, grunnlag, false, opptjeningPeriode).equals(VurderingsStatus.TIL_VURDERING)) {
            builder.medErManueltBehandlet();
        }
    }

    public Optional<OpptjeningResultat> hentOpptjeningHvisFinnes(Long behandlingId) {
        return opptjeningRepository.finnOpptjening(behandlingId);
    }

    private void mapAnnenAktivitet(List<OpptjeningsperiodeForSaksbehandling> perioder, Map.Entry<ArbeidType, List<OppgittAnnenAktivitet>> annenAktivitet,
                                   InntektArbeidYtelseGrunnlag grunnlag, BehandlingReferanse behandlingReferanse,
                                   OpptjeningAktivitetVurdering vurderForSaksbehandling, Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening, DatoIntervallEntitet opptjeningPeriode) {
        var opptjeningAktivitetType = utledOpptjeningType(mapArbeidOpptjening, annenAktivitet.getKey());

        AktørId aktørId = behandlingReferanse.getAktørId();
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getBekreftetAnnenOpptjening(aktørId));

        for (OppgittAnnenAktivitet aktivitet : annenAktivitet.getValue()) {
            var overstyrtAktivitet = finnTilsvarende(filter, aktivitet.getArbeidType(), aktivitet.getPeriode()).orElse(null);
            var builder = OpptjeningsperiodeForSaksbehandling.Builder.ny();
            var status = vurderForSaksbehandling.vurderStatus(opptjeningAktivitetType, behandlingReferanse, overstyrtAktivitet, grunnlag, grunnlag.harBlittSaksbehandlet(), opptjeningPeriode);
            builder.medPeriode(utledPeriode(aktivitet.getPeriode(), overstyrtAktivitet))
                .medOpptjeningAktivitetType(opptjeningAktivitetType)
                .medVurderingsStatus(status);

            if (overstyrtAktivitet != null) {
                var aktivitetsAvtale = utledAktivitetAvtale(aktivitet.getPeriode(), overstyrtAktivitet);
                aktivitetsAvtale.ifPresent(aktivitetsAvtale1 -> builder.medBegrunnelse(aktivitetsAvtale1.getBeskrivelse()));
                builder.medErManueltBehandlet();
            }
            if (grunnlag.harBlittSaksbehandlet() && VurderingsStatus.UNDERKJENT.equals(status)) {
                builder.medErManueltBehandlet();
            }
            if (harEndretPåPeriode(aktivitet.getPeriode(), overstyrtAktivitet)) {
                builder.medErPeriodenEndret();
            }
            perioder.add(builder.build());
        }
    }

    private DatoIntervallEntitet utledPeriode(DatoIntervallEntitet periode, Yrkesaktivitet overstyrtAktivitet) {
        if (overstyrtAktivitet == null) {
            return periode;
        }
        return utledAktivitetAvtale(periode, overstyrtAktivitet).map(AktivitetsAvtale::getPeriode).orElse(periode);
    }

    private Optional<AktivitetsAvtale> utledAktivitetAvtale(DatoIntervallEntitet periode, Yrkesaktivitet overstyrtAktivitet) {
        return new YrkesaktivitetFilter(null, List.of(overstyrtAktivitet)).getAktivitetsAvtalerForArbeid()
            .stream()
            .filter(it -> it.getPeriode().overlapper(periode))
            .findFirst();
    }

    private OpptjeningAktivitetType utledOpptjeningType(Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening, ArbeidType arbeidType) {
        return mapArbeidOpptjening.get(arbeidType)
            .stream()
            .findFirst()
            .orElse(OpptjeningAktivitetType.UDEFINERT);
    }

    private OpptjeningsperiodeForSaksbehandling mapEgenNæring(OppgittEgenNæring egenNæring, InntektArbeidYtelseGrunnlag grunnlag,
                                                              BehandlingReferanse behandlingReferanse,
                                                              OpptjeningAktivitetVurdering vurderForSaksbehandling, DatoIntervallEntitet opptjeningPeriode) {
        final OpptjeningsperiodeForSaksbehandling.Builder builder = OpptjeningsperiodeForSaksbehandling.Builder.ny()
            .medOpptjeningAktivitetType(NÆRING);

        AktørId aktørId = behandlingReferanse.getAktørId();
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getBekreftetAnnenOpptjening(aktørId));

        final Yrkesaktivitet overstyrt = finnTilsvarende(filter, ArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, egenNæring.getPeriode()).orElse(null);
        builder.medPeriode(utledPeriode(egenNæring.getPeriode(), overstyrt));
        if (egenNæring.getOrgnr() != null) {
            builder.medOpptjeningsnøkkel(new Opptjeningsnøkkel(null, egenNæring.getOrgnr(), null))
                .medArbeidsgiver(Arbeidsgiver.virksomhet(egenNæring.getOrgnr()));
        }

        builder.medVurderingsStatus(vurderForSaksbehandling.vurderStatus(NÆRING, behandlingReferanse, overstyrt, grunnlag, grunnlag.harBlittSaksbehandlet(), opptjeningPeriode));
        if (grunnlag.harBlittSaksbehandlet()) {
            builder.medErManueltBehandlet();
        }
        builder.medStillingsandel(new Stillingsprosent(BigDecimal.valueOf(100)));
        return builder.build();
    }

    private boolean harEndretPåPeriode(DatoIntervallEntitet periode, Yrkesaktivitet overstyrtAktivitet) {
        if (overstyrtAktivitet == null) {
            return false;
        }

        return new YrkesaktivitetFilter(null, List.of(overstyrtAktivitet)).getAktivitetsAvtalerForArbeid().stream().map(AktivitetsAvtale::getPeriode).noneMatch(p -> p.equals(periode));
    }

    private Optional<OpptjeningsperiodeForSaksbehandling> lagOpptjeningsperiodeForFrilansAktivitet(BehandlingReferanse behandlingReferanse,
                                                                                                   OppgittOpptjening oppgittOpptjening,
                                                                                                   InntektArbeidYtelseGrunnlag grunnlag,
                                                                                                   List<OpptjeningsperiodeForSaksbehandling> perioder,
                                                                                                   DatoIntervallEntitet opptjeningsperiode,
                                                                                                   Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening,
                                                                                                   OpptjeningAktivitetVurdering vurderOpptjening) {
        // Hvis oppgitt frilansaktivitet brukes perioden derfra og det er allerede laget en OFS.
        if (oppgittOpptjening != null && oppgittOpptjening.getAnnenAktivitet().stream().anyMatch(oaa -> ArbeidType.FRILANSER.equals(oaa.getArbeidType())) ||
            perioder.stream().anyMatch(oaa -> OpptjeningAktivitetType.FRILANS.equals(oaa.getOpptjeningAktivitetType()))) {
            return Optional.empty();
        }

        AktørId aktørId = behandlingReferanse.getAktørId();
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(aktørId));

        var filterRegisterFør = filter.før(opptjeningsperiode.getTomDato());
        var inntektFilter = new InntektFilter(grunnlag.getAktørInntektFraRegister(aktørId)).før(opptjeningsperiode.getTomDato()).filterPensjonsgivende();

        if (!inntektFilter.getFiltrertInntektsposter().isEmpty() && !filterRegisterFør.getFrilansOppdrag().isEmpty()) {

            var frilansMedInntekt = filterRegisterFør.getFrilansOppdrag().stream()
                .filter(frilans -> harInntektFraVirksomhetForPeriode(frilans, inntektFilter, opptjeningsperiode))
                .collect(Collectors.toList());
            var brukType = utledOpptjeningType(mapArbeidOpptjening, ArbeidType.FRILANSER);

            var filterSaksbehandlet = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getBekreftetAnnenOpptjening(aktørId));
            var overstyrtAktivitet = finnTilsvarende(filterSaksbehandlet, ArbeidType.FRILANSER, opptjeningsperiode).orElse(null);

            return frilansMedInntekt.isEmpty() ? Optional.empty()
                : Optional.of(OpptjeningsperiodeForSaksbehandling.Builder.ny()
                .medOpptjeningAktivitetType(brukType)
                .medPeriode(opptjeningsperiode)
                .medVurderingsStatus(
                    vurderOpptjening.vurderStatus(brukType, behandlingReferanse, overstyrtAktivitet, grunnlag, grunnlag.harBlittSaksbehandlet(), opptjeningsperiode))
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
