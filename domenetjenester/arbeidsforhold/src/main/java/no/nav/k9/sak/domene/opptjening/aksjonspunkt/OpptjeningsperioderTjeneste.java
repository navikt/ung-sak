package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import static no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType.NÆRING;
import static no.nav.k9.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import no.nav.k9.sak.domene.opptjening.MellomliggendeHelgUtleder;
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
    private final MapYtelsesstidslinjerForPermisjonvalidering mapYtelsesstidslinjerForPermisjonvalidering = new MapYtelsesstidslinjerForPermisjonvalidering();

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

        var mapArbeidOpptjening = OpptjeningAktivitetType.hentFraArbeidTypeRelasjoner();
        var tidslinjePerYtelse = mapYtelsesstidslinjerForPermisjonvalidering.utledYtelsesTidslinjerForValideringAvPermisjoner(new YtelseFilter(grunnlag.getAktørYtelseFraRegister(aktørId)));
        for (var yrkesaktivitet : yrkesaktivitetFilter.getYrkesaktiviteter()) {
            var opptjeningsperioder = MapYrkesaktivitetTilOpptjeningsperiodeTjeneste.mapYrkesaktivitet(ref, yrkesaktivitet, grunnlag, vurderOpptjening, mapArbeidOpptjening, opptjeningPeriode, vilkårsPeriode, tidslinjePerYtelse);
            perioder.addAll(opptjeningsperioder);
        }

        var oppgittOpptjeningFilter = oppgittOpptjeningFilterProvider.finnOpptjeningFilter(ref.getBehandlingId());
        var oppgittOpptjening = oppgittOpptjeningFilter.hentOppgittOpptjening(ref.getBehandlingId(), grunnlag, skjæringstidspunkt).orElse(null);
        perioder.addAll(mapOppgittOpptjening(mapArbeidOpptjening, oppgittOpptjening, vurderOpptjening, ref, vilkårsPeriode));
        var ytelseFilterVenstreSide = new YtelseFilter(grunnlag.getAktørYtelseFraRegister(aktørId)).før(opptjeningPeriode.getTomDato());
        perioder.addAll(mapYtelseperioderTjeneste.mapYtelsePerioder(ref, vilkårsPeriode, vurderOpptjening, true, ytelseFilterVenstreSide));
        lagOpptjeningsperiodeForFrilansAktivitet(ref, oppgittOpptjening, vurderOpptjening, grunnlag, perioder, opptjeningPeriode,
            mapArbeidOpptjening, vilkårsPeriode).ifPresent(perioder::add);

        return perioder.stream().sorted(Comparator.comparing(OpptjeningsperiodeForSaksbehandling::getPeriode)).collect(Collectors.toList());
    }

    public Optional<OpptjeningResultat> hentOpptjeningHvisFinnes(Long behandlingId) {
        return opptjeningRepository.finnOpptjening(behandlingId);
    }

    private List<OpptjeningsperiodeForSaksbehandling> mapOppgittOpptjening(Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening, OppgittOpptjening oppgittOpptjening, OpptjeningAktivitetVurdering vurderOpptjening, BehandlingReferanse ref, DatoIntervallEntitet vilkårsPeriode) {
        List<OpptjeningsperiodeForSaksbehandling> oppgittOpptjeningPerioder = new ArrayList<>();
        if (oppgittOpptjening != null) {
            for (Map.Entry<ArbeidType, List<OppgittAnnenAktivitet>> annenAktivitet : oppgittOpptjening.getAnnenAktivitet().stream()
                .collect(Collectors.groupingBy(OppgittAnnenAktivitet::getArbeidType)).entrySet()) {
                oppgittOpptjeningPerioder.addAll(mapAnnenAktivitet(annenAktivitet, mapArbeidOpptjening, vurderOpptjening, ref, vilkårsPeriode));
            }
            oppgittOpptjening.getOppgittArbeidsforhold()
                .forEach(oppgittArbeidsforhold -> oppgittOpptjeningPerioder.add(mapOppgittArbeidsforholdUtenOverstyring(oppgittArbeidsforhold,
                    mapArbeidOpptjening, vurderOpptjening, ref, vilkårsPeriode)));
            oppgittOpptjening.getEgenNæring().forEach(egenNæring -> oppgittOpptjeningPerioder.add(mapEgenNæring(egenNæring, vurderOpptjening, ref, vilkårsPeriode)));
        }
        return oppgittOpptjeningPerioder;
    }

    private OpptjeningsperiodeForSaksbehandling mapOppgittArbeidsforholdUtenOverstyring(OppgittArbeidsforhold oppgittArbeidsforhold,
                                                                                        Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening,
                                                                                        OpptjeningAktivitetVurdering vurderOpptjening,
                                                                                        BehandlingReferanse ref,
                                                                                        DatoIntervallEntitet vilkårsPeriode) {
        final OpptjeningAktivitetType type = utledOpptjeningType(mapArbeidOpptjening, oppgittArbeidsforhold.getArbeidType());
        VurderingsStatus status = vurderOpptjening.vurderStatus(lagInput(ref, type, vilkårsPeriode, oppgittArbeidsforhold.getPeriode()));
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
                                                                        BehandlingReferanse ref, DatoIntervallEntitet vilkårsPeriode) {
        OpptjeningAktivitetType opptjeningAktivitetType = utledOpptjeningType(mapArbeidOpptjening, annenAktivitet.getKey());
        List<OpptjeningsperiodeForSaksbehandling> annenAktivitetPerioder = new ArrayList<>();
        for (OppgittAnnenAktivitet aktivitet : annenAktivitet.getValue()) {
            OpptjeningsperiodeForSaksbehandling.Builder builder = OpptjeningsperiodeForSaksbehandling.Builder.ny();
            var input = lagInput(ref, opptjeningAktivitetType, vilkårsPeriode, aktivitet.getPeriode());
            VurderingsStatus status = vurderOpptjening.vurderStatus(input);
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

    private OpptjeningsperiodeForSaksbehandling mapEgenNæring(OppgittEgenNæring egenNæring, OpptjeningAktivitetVurdering vurderOpptjening, BehandlingReferanse ref, DatoIntervallEntitet vilkårsPeriode) {
        VurderingsStatus status = vurderOpptjening.vurderStatus(lagInput(ref, NÆRING, vilkårsPeriode, egenNæring.getPeriode()));
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

    private VurderStatusInput lagInput(BehandlingReferanse ref, OpptjeningAktivitetType type, DatoIntervallEntitet vilkårsPeriode, DatoIntervallEntitet aktivitetperiode) {
        var input = new VurderStatusInput(type, ref);
        input.setVilkårsperiode(vilkårsPeriode);
        input.setAktivitetPeriode(aktivitetperiode);
        return input;
    }

    private Optional<OpptjeningsperiodeForSaksbehandling> lagOpptjeningsperiodeForFrilansAktivitet(BehandlingReferanse behandlingReferanse,
                                                                                                   OppgittOpptjening oppgittOpptjening,
                                                                                                   OpptjeningAktivitetVurdering vurderOpptjening,
                                                                                                   InntektArbeidYtelseGrunnlag grunnlag,
                                                                                                   List<OpptjeningsperiodeForSaksbehandling> perioder,
                                                                                                   DatoIntervallEntitet periode,
                                                                                                   Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening,
                                                                                                   DatoIntervallEntitet vilkårsPeriode) {
        // Hvis oppgitt frilansaktivitet brukes perioden derfra og det er allerede laget en OFS.
        if (oppgittOpptjening != null && oppgittOpptjening.getAnnenAktivitet().stream().anyMatch(oaa -> ArbeidType.FRILANSER.equals(oaa.getArbeidType())) ||
            perioder.stream().anyMatch(oaa -> OpptjeningAktivitetType.FRILANS.equals(oaa.getOpptjeningAktivitetType()))) {
            return Optional.empty();
        }

        AktørId aktørId = behandlingReferanse.getAktørId();
        Optional<AktørArbeid> aktørArbeidFraRegister = grunnlag.getAktørArbeidFraRegister(aktørId);
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), aktørArbeidFraRegister).før(periode.getTomDato());

        Collection<Yrkesaktivitet> frilansOppdrag = filter.getFrilansOppdrag();

        if (aktørArbeidFraRegister.isPresent() && !frilansOppdrag.isEmpty() || (oppgittOpptjening != null && oppgittOpptjening.getFrilans().isPresent())) {

            DatoIntervallEntitet frilansPeriode = finnFrilansPeriode(oppgittOpptjening, periode, frilansOppdrag);

            OpptjeningAktivitetType brukType = utledOpptjeningType(mapArbeidOpptjening, ArbeidType.FRILANSER);
            VurderingsStatus status = vurderOpptjening.vurderStatus(lagInput(behandlingReferanse, brukType, vilkårsPeriode, frilansPeriode));
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
