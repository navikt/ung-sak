package no.nav.folketrygdloven.beregningsgrunnlag.adapter.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSats;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningSatsType;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.kodeverk.beregningsgrunnlag.PeriodeÅrsak;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.tid.ÅpenDatoIntervallEntitet;

@ApplicationScoped
public class BeregningsgrunnlagTestUtil {

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    BeregningsgrunnlagTestUtil() {
        // Inject
    }

    @Inject
    public BeregningsgrunnlagTestUtil(BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                      InntektArbeidYtelseTjeneste iayTjeneste) {
        Objects.requireNonNull(beregningsgrunnlagRepository);
        this.inntektArbeidYtelseTjeneste = iayTjeneste;
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
    }

    public void leggTilFLTilknyttetOrganisasjon(BehandlingReferanse behandlingReferanse, String orgnr, InternArbeidsforholdRef arbId) {
        BeregningsgrunnlagEntitet bg = beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId()).dypKopi();
        bg.getBeregningsgrunnlagPerioder().forEach(periode -> {
            BGAndelArbeidsforhold.Builder bga = BGAndelArbeidsforhold
                .builder()
                .medArbeidsforholdRef(arbId)
                .medArbeidsgiver(lagArbeidsgiver(orgnr));
            BeregningsgrunnlagPrStatusOgAndel.builder()
                .medBGAndelArbeidsforhold(bga)
                .medAktivitetStatus(AktivitetStatus.FRILANSER)
                .build(periode);
        });
        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), bg, BeregningsgrunnlagTilstand.OPPRETTET);
    }

    public BeregningsgrunnlagEntitet lagGjeldendeBeregningsgrunnlag(BehandlingReferanse ref, LocalDate skjæringstidspunktOpptjening, AktivitetStatus... statuser) {
        HashMap<String, Integer> avkortet = new HashMap<>();
        HashMap<String, Integer> bruttoPrÅr = new HashMap<>();
        List<LocalDateInterval> perioder = Collections.singletonList(new LocalDateInterval(skjæringstidspunktOpptjening, null));
        return lagGjeldendeBeregningsgrunnlag(ref, skjæringstidspunktOpptjening, avkortet,
            bruttoPrÅr, Collections.emptyMap(), perioder, Collections.singletonList(Collections.emptyList()), Collections.emptyMap(), statuser);
    }

    public BeregningsgrunnlagEntitet lagGjeldendeBeregningsgrunnlag(BehandlingReferanse ref, LocalDate skjæringstidspunktOpptjening, List<LocalDateInterval> berPerioder, AktivitetStatus... statuser) {
        return lagGjeldendeBeregningsgrunnlag(ref, skjæringstidspunktOpptjening, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), berPerioder, Collections.emptyList(), Collections.emptyMap(), statuser);
    }

    public BeregningsgrunnlagEntitet lagGjeldendeBeregningsgrunnlag(BehandlingReferanse ref, LocalDate skjæringstidspunktOpptjening, List<LocalDateInterval> berPerioder, List<List<PeriodeÅrsak>> opprinneligePeriodeÅrsaker, AktivitetStatus... statuser) {
        return lagGjeldendeBeregningsgrunnlag(ref, skjæringstidspunktOpptjening, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), berPerioder, opprinneligePeriodeÅrsaker, Collections.emptyMap(), statuser);
    }

    private BeregningsgrunnlagEntitet lagGjeldendeBeregningsgrunnlag(BehandlingReferanse ref, // NOSONAR - brukes bare til test
                                                             LocalDate skjæringstidspunktOpptjening,
                                                             Map<String, Integer> andelAvkortet,
                                                             Map<String, Integer> bruttoPrÅr,
                                                             Map<String, List<Boolean>> lagtTilAvSaksbehandler,
                                                             List<LocalDateInterval> perioder,
                                                             List<List<PeriodeÅrsak>> periodePeriodeÅrsaker,
                                                             Map<String, List<Inntektskategori>> inntektskategoriPrAndelIArbeidsforhold,
                                                             AktivitetStatus... statuser) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(skjæringstidspunktOpptjening)
            .medGrunnbeløp(getGrunnbeløp(skjæringstidspunktOpptjening))
            .build();

        if (statuser.length > 0) {
            byggBGForSpesifikkeAktivitetstatuser(ref, skjæringstidspunktOpptjening, beregningsgrunnlag, statuser);
        } else {
            lagPerioder(ref, andelAvkortet, bruttoPrÅr, lagtTilAvSaksbehandler, Collections.nCopies(perioder.size(), 1000),
                Collections.nCopies(perioder.size(), 1000), perioder, beregningsgrunnlag, periodePeriodeÅrsaker, inntektskategoriPrAndelIArbeidsforhold, Collections.emptyMap());
        }
        beregningsgrunnlagRepository.lagre(ref.getBehandlingId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.FASTSATT);
        return beregningsgrunnlag;
    }

    private void byggBGForSpesifikkeAktivitetstatuser(BehandlingReferanse ref, LocalDate skjæringstidspunktOpptjening, BeregningsgrunnlagEntitet beregningsgrunnlag, AktivitetStatus[] statuser) {
        BeregningsgrunnlagAktivitetStatus.Builder bgAktivitetStatusbuilder = BeregningsgrunnlagAktivitetStatus.builder();
        for (int i = 1; i < statuser.length; i++) {
            bgAktivitetStatusbuilder.medAktivitetStatus(statuser[i]);
        }
        bgAktivitetStatusbuilder.medAktivitetStatus(statuser[0]).build(beregningsgrunnlag);
        List<AktivitetStatus> enkeltstatuser = oversettTilEnkeltstatuser(statuser);

        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(skjæringstidspunktOpptjening, null)
            .build(beregningsgrunnlag);
        for (AktivitetStatus status : enkeltstatuser) {
            if (status.equals(AktivitetStatus.ARBEIDSTAKER)) {
                continue;
            }
            BeregningsgrunnlagPrStatusOgAndel.Builder andelBuilder = BeregningsgrunnlagPrStatusOgAndel.builder()
                .medAktivitetStatus(status);
            if (status.equals(AktivitetStatus.FRILANSER)) {
                andelBuilder.medBeregningsperiode(skjæringstidspunktOpptjening.minusMonths(3).withDayOfMonth(1), skjæringstidspunktOpptjening.withDayOfMonth(1).minusDays(1));
            }
            andelBuilder.build(periode);
        }
        Long behandlingId = ref.getBehandlingId();
        InntektArbeidYtelseGrunnlag agg = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        var aktørArbeid = agg.getAktørArbeidFraRegister(ref.getAktørId());

        var filter = new YrkesaktivitetFilter(agg.getArbeidsforholdInformasjon(), aktørArbeid).før(skjæringstidspunktOpptjening);
        Collection<Yrkesaktivitet> aktiviteterOpt = filter.getYrkesaktiviteterForBeregning();

        List<Yrkesaktivitet> aktiviteter = aktiviteterOpt.stream().filter(a -> a.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold()).collect(Collectors.toList());

        for (int i = 0; i < aktiviteter.size(); i++) {
            String arbId = aktiviteter.get(i).getArbeidsforholdRef().getReferanse();
            String orgNr = aktiviteter.get(i).getArbeidsgiver().getIdentifikator();
            DatoIntervallEntitet arbeidsperiode = finnArbeidsperiode(filter, aktiviteter, i);
            BGAndelArbeidsforhold.Builder bga = BGAndelArbeidsforhold.builder()
                .medArbeidsgiver(lagArbeidsgiver(orgNr))
                .medArbeidsforholdRef(arbId)
                .medArbeidsperiodeFom(arbeidsperiode.getFomDato())
                .medArbeidsperiodeTom(arbeidsperiode.getTomDato());

            BeregningsgrunnlagPrStatusOgAndel.builder()
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .medBeregningsperiode(skjæringstidspunktOpptjening.minusMonths(3).withDayOfMonth(1), skjæringstidspunktOpptjening.withDayOfMonth(1).minusDays(1))
                .medBGAndelArbeidsforhold(bga)
                .build(periode);
        }
    }

    private DatoIntervallEntitet finnArbeidsperiode(YrkesaktivitetFilter filter, List<Yrkesaktivitet> aktiviteter, int i) {
        return filter.getAnsettelsesPerioder(aktiviteter.get(i)).stream()
            .map(AktivitetsAvtale::getPeriode)
            .reduce((p1, p2) -> {
                LocalDate fom = p1.getFomDato().isBefore(p2.getFomDato()) ? p1.getFomDato() : p2.getFomDato();
                LocalDate tom = p1.getTomDato().isAfter(p2.getTomDato()) ? p1.getTomDato() : p2.getTomDato();
                return tom == null ? DatoIntervallEntitet.fraOgMed(fom) : DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        }).orElse(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusYears(1), LocalDate.now().plusYears(2)));
    }

    private List<AktivitetStatus> oversettTilEnkeltstatuser(AktivitetStatus... statuser) {
        List<AktivitetStatus> enkeltstatuser = new ArrayList<>();
        if (statuser.length == 0) {
            enkeltstatuser.add(AktivitetStatus.ARBEIDSTAKER);
        } else {
            Map<AktivitetStatus, List<AktivitetStatus>> kombinasjonsstatuser = new HashMap<>();
            kombinasjonsstatuser.put(AktivitetStatus.KOMBINERT_AT_FL, Arrays.asList(AktivitetStatus.ARBEIDSTAKER, AktivitetStatus.FRILANSER));
            kombinasjonsstatuser.put(AktivitetStatus.KOMBINERT_AT_FL_SN, Arrays.asList(AktivitetStatus.ARBEIDSTAKER, AktivitetStatus.FRILANSER, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE));
            kombinasjonsstatuser.put(AktivitetStatus.KOMBINERT_AT_SN, Arrays.asList(AktivitetStatus.ARBEIDSTAKER, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE));
            kombinasjonsstatuser.put(AktivitetStatus.KOMBINERT_FL_SN, Arrays.asList(AktivitetStatus.FRILANSER, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE));
            for (AktivitetStatus status : statuser) {
                if (kombinasjonsstatuser.containsKey(status)) {
                    enkeltstatuser.addAll(kombinasjonsstatuser.get(status));
                } else {
                    enkeltstatuser.add(status);
                }
            }
        }
        return enkeltstatuser;
    }


    public BeregningsgrunnlagEntitet lagForrigeBeregningsgrunnlag(BehandlingReferanse ref, // NOSONAR - brukes bare til test
                                                           LocalDate skjæringstidspunktOpptjening,
                                                           Map<String, Integer> andelAvkortet,
                                                           Map<String, Integer> bruttoPrÅr,
                                                           Map<String, List<Boolean>> lagtTilAvSaksbehandler,
                                                           List<LocalDateInterval> perioder,
                                                           List<List<PeriodeÅrsak>> periodePeriodeÅrsaker,
                                                           Map<String, List<Inntektskategori>> inntektskategoriPrAndelIArbeidsforhold) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(skjæringstidspunktOpptjening)
            .medGrunnbeløp(getGrunnbeløp(skjæringstidspunktOpptjening))
            .build();
        lagPerioder(ref, andelAvkortet, bruttoPrÅr, lagtTilAvSaksbehandler, Collections.nCopies(perioder.size(), 1000),
            Collections.nCopies(perioder.size(), 1000), perioder, beregningsgrunnlag, periodePeriodeÅrsaker, inntektskategoriPrAndelIArbeidsforhold, Collections.emptyMap());
        beregningsgrunnlagRepository.lagre(ref.getBehandlingId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.FASTSATT_INN);
        return beregningsgrunnlag;
    }

    public BeregningsgrunnlagEntitet lagBeregningsgrunnlagForEndring(BehandlingReferanse ref, LocalDate skjæringstidspunktOpptjening, List<List<PeriodeÅrsak>> periodePeriodeÅrsaker, List<LocalDateInterval> perioder) {
        HashMap<String, Integer> avkortet = new HashMap<>();
        HashMap<String, Integer> bruttoPrÅr = new HashMap<>();
        return lagBeregningsgrunnlagForEndring(ref, skjæringstidspunktOpptjening,
            avkortet, bruttoPrÅr, periodePeriodeÅrsaker, perioder, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }


    public BeregningsgrunnlagEntitet lagBeregningsgrunnlagForEndring(BehandlingReferanse ref, LocalDate skjæringstidspunktOpptjening, Map<String, Integer> andelAvkortet, // NOSONAR - brukes bare til test
                                                              Map<String, Integer> bruttoPrÅr,
                                                              List<List<PeriodeÅrsak>> periodePeriodeÅrsaker,
                                                              List<LocalDateInterval> perioder,
                                                              Map<String, List<Boolean>> lagtTilAvSaksbehandlerPrAndelIArbeidsforhold,
                                                              Map<String, List<Inntektskategori>> inntektskategoriPrAndelIArbeidsforhold, Map<String, Integer> refusjonPrÅr) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(skjæringstidspunktOpptjening)
            .medGrunnbeløp(getGrunnbeløp(skjæringstidspunktOpptjening))
            .build();
        lagPerioder(ref, andelAvkortet, bruttoPrÅr, lagtTilAvSaksbehandlerPrAndelIArbeidsforhold, Collections.nCopies(perioder.size(), null), Collections.nCopies(perioder.size(), null),
            perioder, beregningsgrunnlag, periodePeriodeÅrsaker, inntektskategoriPrAndelIArbeidsforhold, refusjonPrÅr);
        Optional<BeregningsgrunnlagEntitet> gjeldendeBg = beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(
            ref.getBehandlingId(), ref.getOriginalBehandlingId(), BeregningsgrunnlagTilstand.FASTSATT)
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);

        Long behandlingId = ref.getBehandlingId();
        gjeldendeBg.ifPresent(bg -> {
            beregningsgrunnlagRepository.lagre(behandlingId, bg.dypKopi(), BeregningsgrunnlagTilstand.KOFAKBER_UT);
        });
        beregningsgrunnlagRepository.lagre(behandlingId, beregningsgrunnlag, BeregningsgrunnlagTilstand.OPPRETTET);
        return beregningsgrunnlag;
    }

    private void lagPerioder(BehandlingReferanse ref, Map<String, Integer> avkortetAndel, // NOSONAR - brukes bare til test, men denne bør reskrives // TODO (Safir)
                             Map<String, Integer> bruttoPrÅr,
                             Map<String, List<Boolean>> lagtTilAvSaksbehandlerPrAndelIArbeidsforhold,
                             List<Integer> redusert,
                             List<Integer> avkortet,
                             List<LocalDateInterval> perioder,
                             BeregningsgrunnlagEntitet beregningsgrunnlag,
                             List<List<PeriodeÅrsak>> periodePeriodeÅrsaker,
                             Map<String, List<Inntektskategori>> inntektskategoriPrAndelIArbeidsforhold,
                             Map<String, Integer> refusjonPrÅr) {
        BeregningsgrunnlagAktivitetStatus.Builder bgAktivitetStatusbuilder = BeregningsgrunnlagAktivitetStatus.builder();
        bgAktivitetStatusbuilder.medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER).build(beregningsgrunnlag);
        for (int j = 0; j < perioder.size(); j++) {
            BeregningsgrunnlagPeriode.Builder periodeBuilder = BeregningsgrunnlagPeriode.builder()
                .medBeregningsgrunnlagPeriode(perioder.get(j).getFomDato(), perioder.get(j).getTomDato())
                .medAvkortetPrÅr(avkortet.get(j) != null ? BigDecimal.valueOf(avkortet.get(j)) : null)
                .medRedusertPrÅr(redusert.get(j) != null ? BigDecimal.valueOf(redusert.get(j)) : null);
            if (!periodePeriodeÅrsaker.isEmpty()) {
                periodeBuilder.leggTilPeriodeÅrsaker(periodePeriodeÅrsaker.get(j));
            }
            BeregningsgrunnlagPeriode beregningsgrunnlagPeriode = periodeBuilder.build(beregningsgrunnlag);
            lagAndelerPrArbeidsforhold(ref, beregningsgrunnlag, avkortetAndel, bruttoPrÅr, lagtTilAvSaksbehandlerPrAndelIArbeidsforhold,
                inntektskategoriPrAndelIArbeidsforhold, refusjonPrÅr, beregningsgrunnlagPeriode);
        }
    }

    private void lagAndelerPrArbeidsforhold(BehandlingReferanse ref,//NOSONAR
                                            BeregningsgrunnlagEntitet beregningsgrunnlag,
                                            Map<String, Integer> avkortetAndel,
                                            Map<String, Integer> bruttoPrÅr,
                                            Map<String, List<Boolean>> lagtTilAvSaksbehandlerPrAndelIArbeidsforhold,
                                            Map<String, List<Inntektskategori>> inntektskategoriPrAndelIArbeidsforhold,
                                            Map<String, Integer> refusjonPrÅr,
                                            BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {

        Long behandlingId = ref.getId();
        AktørId aktørId = ref.getAktørId();
        InntektArbeidYtelseGrunnlag agg = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        var aktørArbeid = agg.getAktørArbeidFraRegister(aktørId);
        var filter = new YrkesaktivitetFilter(agg.getArbeidsforholdInformasjon(), aktørArbeid);

        List<Yrkesaktivitet> aktiviteter = finnAlleYrkesaktiviteter(filter, beregningsgrunnlag);
        List<Arbeidsgiver> arbeidsgivere = aktiviteter.stream()
            .map(Yrkesaktivitet::getArbeidsgiver).collect(Collectors.toList());
        for (int i = 0; i < aktiviteter.size(); i++) {
            String arbId = aktiviteter.get(i).getArbeidsforholdRef().getReferanse();
            Arbeidsgiver arbeidsgiver = arbeidsgivere.get(i);
            String identifikator = arbeidsgiver.getIdentifikator();
            List<Yrkesaktivitet> aktivitetIPeriode = finnAktivitetForAndelIPeriode(filter, aktiviteter, beregningsgrunnlagPeriode.getPeriode(), identifikator);
            DatoIntervallEntitet arbeidsperiode = finnArbeidsperiode(filter, aktiviteter, i);
            BGAndelArbeidsforhold.Builder bga = byggArbeidsforhold(refusjonPrÅr, arbId, arbeidsgiver, arbeidsperiode);
            if (!aktivitetIPeriode.isEmpty()) {
                if (lagtTilAvSaksbehandlerPrAndelIArbeidsforhold.get(identifikator) != null && !lagtTilAvSaksbehandlerPrAndelIArbeidsforhold.get(identifikator).isEmpty()) {
                    for (int k = 0; k < lagtTilAvSaksbehandlerPrAndelIArbeidsforhold.get(identifikator).size(); k++) {
                        Inntektskategori inntektskategori = finnInntektskategori(inntektskategoriPrAndelIArbeidsforhold, identifikator, k);
                        byggAndel(avkortetAndel.get(identifikator), bruttoPrÅr.get(identifikator),
                            lagtTilAvSaksbehandlerPrAndelIArbeidsforhold.get(identifikator).get(k),
                            inntektskategori, beregningsgrunnlagPeriode, bga);
                    }
                } else {
                    byggAndel(avkortetAndel.get(identifikator), bruttoPrÅr.get(identifikator), false,
                        Inntektskategori.UDEFINERT, beregningsgrunnlagPeriode, bga);
                }
            }
        }
    }

    private List<Yrkesaktivitet> finnAlleYrkesaktiviteter(YrkesaktivitetFilter filter, BeregningsgrunnlagEntitet beregningsgrunnlag) {

        var filterFør = filter.før(beregningsgrunnlag.getSkjæringstidspunkt());
        Collection<Yrkesaktivitet> aktiviteterFørStpOpt = filterFør.getYrkesaktiviteterForBeregning();
        Stream<Yrkesaktivitet> aktiviteterFørStp = aktiviteterFørStpOpt.stream().filter(a -> a.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold());

        var filterEtter = filter.etter(beregningsgrunnlag.getSkjæringstidspunkt());
        Collection<Yrkesaktivitet> aktiviteterEtterStpOpt = filterEtter.getYrkesaktiviteterForBeregning();
        Stream<Yrkesaktivitet> aktiviteterEtterStp = aktiviteterEtterStpOpt.stream().filter(a -> a.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold());

        return Stream.concat(aktiviteterFørStp, aktiviteterEtterStp).distinct().collect(Collectors.toList());
    }

    private List<Yrkesaktivitet> finnAktivitetForAndelIPeriode(YrkesaktivitetFilter filter, List<Yrkesaktivitet> aktiviteter, ÅpenDatoIntervallEntitet periode, String orgNr) {
        return aktiviteter.stream()
            .filter(a -> a.getArbeidsgiver().getIdentifikator().equals(orgNr))
            .filter(a -> filter.getAnsettelsesPerioder(a).stream().anyMatch(ansettelsePeriode ->
                ansettelsePeriode.getPeriode().overlapper(periode) ||
                    ansettelsePeriode.getPeriode().getTomDato().isBefore(periode.getFomDato())))
            .collect(Collectors.toList());
    }

    private Inntektskategori finnInntektskategori(Map<String, List<Inntektskategori>> inntektskategoriPrAndelIArbeidsforhold, String orgNr, int k) {
        return inntektskategoriPrAndelIArbeidsforhold.get(orgNr) != null && k < inntektskategoriPrAndelIArbeidsforhold.get(orgNr).size() ?
            inntektskategoriPrAndelIArbeidsforhold.get(orgNr).get(k) : Inntektskategori.ARBEIDSTAKER;
    }

    private BGAndelArbeidsforhold.Builder byggArbeidsforhold(Map<String, Integer> refusjonPrÅr, String arbId, Arbeidsgiver arbeidsgiver, DatoIntervallEntitet arbeidsperiode) {
        String identifikator = arbeidsgiver.getIdentifikator();
        return BGAndelArbeidsforhold.builder()
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdRef(arbId)
            .medRefusjonskravPrÅr(refusjonPrÅr.get(identifikator) != null ?
                BigDecimal.valueOf(refusjonPrÅr.get(identifikator)) : null)
            .medArbeidsperiodeFom(arbeidsperiode.getFomDato())
            .medArbeidsperiodeTom(arbeidsperiode.getTomDato());
    }

    private void byggAndel(Integer avkortetPrÅr, Integer bruttoPrÅr, Boolean lagtTilAvSaksbehandler, Inntektskategori inntektskategori, BeregningsgrunnlagPeriode beregningsgrunnlagPeriode, BGAndelArbeidsforhold.Builder bga) { //NOSONAR
        LocalDate skjæringstidspunkt = beregningsgrunnlagPeriode.getBeregningsgrunnlag().getSkjæringstidspunkt();
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBeregningsperiode(skjæringstidspunkt.minusMonths(3).withDayOfMonth(1), skjæringstidspunkt.withDayOfMonth(1).minusDays(1))
            .medBGAndelArbeidsforhold(bga)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medAvkortetPrÅr(avkortetPrÅr != null ? BigDecimal.valueOf(avkortetPrÅr) : null)
            .medBeregnetPrÅr(bruttoPrÅr != null ? BigDecimal.valueOf(bruttoPrÅr) : null)
            .medLagtTilAvSaksbehandler(lagtTilAvSaksbehandler)
            .medInntektskategori(inntektskategori)
            .build(beregningsgrunnlagPeriode);
    }

    public BigDecimal getGrunnbeløp(LocalDate skjæringstidspunktOpptjening) {
        BeregningSats sats = beregningsgrunnlagRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, skjæringstidspunktOpptjening);
        return BigDecimal.valueOf(sats.getVerdi());
    }

    private Arbeidsgiver lagArbeidsgiver(String orgNr) {
        return Arbeidsgiver.virksomhet(orgNr);
    }
}
