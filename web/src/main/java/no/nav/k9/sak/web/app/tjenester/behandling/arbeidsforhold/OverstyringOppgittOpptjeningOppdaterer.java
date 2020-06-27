package no.nav.k9.sak.web.app.tjenester.behandling.arbeidsforhold;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.abakus.iaygrunnlag.kodeverk.VirksomhetType;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.k9.sak.domene.iay.modell.OppgittFrilans;
import no.nav.k9.sak.domene.iay.modell.OppgittFrilansoppdrag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.EgenNæringBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittFrilansBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittFrilansOppdragBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.arbeidsforhold.BekreftOverstyrOppgittOpptjeningDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.OppgittArbeidsforholdDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.OppgittEgenNæringDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.OppgittFrilansDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.OppgittOpptjeningDto;
import no.nav.k9.sak.kontrakt.frisinn.PeriodeMedSNOgFLDto;
import no.nav.k9.sak.kontrakt.frisinn.SøknadsperiodeOgOppgittOpptjeningV2Dto;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.frisinn.vilkår.UtledPerioderMedEndringTjeneste;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftOverstyrOppgittOpptjeningDto.class, adapter = AksjonspunktOppdaterer.class)
public class OverstyringOppgittOpptjeningOppdaterer implements AksjonspunktOppdaterer<BekreftOverstyrOppgittOpptjeningDto> {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private UttakRepository uttakRepository;
    private HistorikkTjenesteAdapter historikkAdapter;
    private VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private boolean toggletVilkårsperioder;

    OverstyringOppgittOpptjeningOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public OverstyringOppgittOpptjeningOppdaterer(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste, UttakRepository uttakRepository,
                                                  HistorikkTjenesteAdapter historikkAdapter,
                                                  BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                                  @FagsakYtelseTypeRef("FRISINN") VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste,
                                                  @KonfigVerdi(value = "FRISINN_VILKARSPERIODER", defaultVerdi = "true") Boolean toggletVilkårsperioder) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.uttakRepository = uttakRepository;
        this.historikkAdapter = historikkAdapter;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.toggletVilkårsperioder = toggletVilkårsperioder;
    }

    @Override
    public OppdateringResultat oppdater(BekreftOverstyrOppgittOpptjeningDto dto, AksjonspunktOppdaterParameter param) {
        var historikkInnslagTekstBuilder = historikkAdapter.tekstBuilder();
        var oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny();
        var søknadsperiodeOgOppgittOpptjening = dto.getSøknadsperiodeOgOppgittOpptjeningDto();
        leggerTilEgenæring(søknadsperiodeOgOppgittOpptjening, historikkInnslagTekstBuilder).ifPresent(oppgittOpptjeningBuilder::leggTilEgneNæringer);
        leggerTilFrilans(søknadsperiodeOgOppgittOpptjening, historikkInnslagTekstBuilder).ifPresent(oppgittOpptjeningBuilder::leggTilFrilansOpplysninger);
        leggerTilArbeidsforhold(søknadsperiodeOgOppgittOpptjening, historikkInnslagTekstBuilder).ifPresent(oppgittOpptjeningBuilder::leggTilOppgittArbeidsforhold);

        var perioderSomSkalMed = utledePerioder(dto);
        uttakRepository.lagreOgFlushFastsattUttak(param.getBehandlingId(), new UttakAktivitet(perioderSomSkalMed));

        inntektArbeidYtelseTjeneste.lagreOverstyrtOppgittOpptjening(param.getBehandlingId(), oppgittOpptjeningBuilder);

        if (toggletVilkårsperioder) {
            var vilkårsperioder = vilkårsPerioderTilVurderingTjeneste.utled(param.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);

            // Validerer at vi har perioder som skal vurderes. Vi må hindre dette for å unngå situasjoner der vi ikkje har aktive beregningsgrunnlag i kalkulus
            if (vilkårsperioder.isEmpty()) {
                throw new IllegalStateException("Ingen perioder er endret");
            }
            for (DatoIntervallEntitet vilkårsperiode : vilkårsperioder) {
                beregningsgrunnlagVilkårTjeneste.settVilkårutfallTilIkkeVurdert(param.getBehandlingId(), vilkårsperiode);
            }
        }

        historikkAdapter.opprettHistorikkInnslag(param.getBehandlingId(), HistorikkinnslagType.FAKTA_ENDRET);
        return OppdateringResultat.utenOveropp();
    }

    private Optional<List<OppgittArbeidsforholdBuilder>>leggerTilArbeidsforhold(SøknadsperiodeOgOppgittOpptjeningV2Dto dto, HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder) {
        List<OppgittArbeidsforholdBuilder> builders = new ArrayList<>();

        List<PeriodeMedSNOgFLDto> måneder = dto.getMåneder();
        for (PeriodeMedSNOgFLDto periodeMedSNOgFLDto : måneder) {
            builders.addAll(mapArbeidsforhold(periodeMedSNOgFLDto, historikkInnslagTekstBuilder));
        }
        return Optional.of(builders);
    }

    private Optional<List<EgenNæringBuilder>> leggerTilEgenæring(SøknadsperiodeOgOppgittOpptjeningV2Dto søknadsperiodeOgOppgittOpptjening, HistorikkInnslagTekstBuilder builder) {
        List<OppgittEgenNæringDto> egenNæring = new ArrayList<>();
        Optional.ofNullable(søknadsperiodeOgOppgittOpptjening.getFørSøkerPerioden().getOppgittEgenNæring()).ifPresent(egenNæring::addAll);
        egenNæring.addAll(søknadsperiodeOgOppgittOpptjening.getMåneder()
            .stream().map(o -> o.getOppgittIMåned().getOppgittEgenNæring())
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .collect(Collectors.toList()));

        if (!egenNæring.isEmpty()) {
            return mapTilEgenNæring(builder, egenNæring);
        }
        return Optional.empty();
    }

    private Optional<OppgittFrilans> leggerTilFrilans(SøknadsperiodeOgOppgittOpptjeningV2Dto søknadsperiodeOgOppgittOpptjening, HistorikkInnslagTekstBuilder builder) {
        var oppdragI = søknadsperiodeOgOppgittOpptjening.getMåneder()
            .stream().map(m -> m.getOppgittIMåned().getOppgittFrilans())
            .filter(Objects::nonNull)
            .map(OppgittFrilansDto::getOppgittFrilansoppdrag)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        if (!oppdragI.isEmpty()) {
            return mapTilFrilans(builder, oppdragI);
        }
        return Optional.empty();
    }

    private List<OppgittArbeidsforholdBuilder> mapArbeidsforhold(PeriodeMedSNOgFLDto periodeMedSNOgFLDto, HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder) {
        OppgittOpptjeningDto oppgittIMåned = periodeMedSNOgFLDto.getOppgittIMåned();
        List<OppgittArbeidsforholdDto> oppgittArbeidsforhold = oppgittIMåned.getOppgittArbeidsforhold();
        List<OppgittArbeidsforholdBuilder> builders = new ArrayList<>();
        if (oppgittArbeidsforhold != null) {
            for (OppgittArbeidsforholdDto dto : oppgittArbeidsforhold) {
                OppgittArbeidsforholdBuilder builder = OppgittArbeidsforholdBuilder.ny();
                BigDecimal verdi = dto.getInntekt().getVerdi();
                builder.medInntekt(verdi);
                LocalDate tidligste = finnTidligste(finnFraOgMedDatoSN(oppgittIMåned).orElse(null), finnFraOgMedDatoFL(oppgittIMåned).orElse(null));
                builder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(tidligste, periodeMedSNOgFLDto.getMåned().getTom()));
                builder.medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
                builders.add(builder);
                historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.ARBEIDSFORHOLD, null, verdi);
            }
        }
        return builders;
    }

    private LocalDate finnTidligste(LocalDate sn, LocalDate fl) {
        if (sn == null && fl == null) {
            return null;
        }
        if (fl == null) {
            return sn;
        } else if (sn == null) {
            return fl;
        }
        return sn.isBefore(fl) ? sn : fl;
    }

    private ArrayList<UttakAktivitetPeriode> utledePerioder(BekreftOverstyrOppgittOpptjeningDto dto) {
        var periodeFraSøknad = dto.getSøknadsperiodeOgOppgittOpptjeningDto().getMåneder();
        var perioderSomSkalMed = new ArrayList<UttakAktivitetPeriode>();

        for (PeriodeMedSNOgFLDto periode : periodeFraSøknad) {
            if (periode.getSøkerFL()) {
                var tidligsteDato = finnFraOgMedDatoFL(periode.getOppgittIMåned());
                perioderSomSkalMed.add(new UttakAktivitetPeriode(UttakArbeidType.FRILANSER, tidligsteDato.orElseThrow(() -> new IllegalStateException("Fant ikke startdato for FL-perioden.")), periode.getMåned().getTom()));
            }
            if (periode.getSøkerSN()) {
                var tidligsteDato = finnFraOgMedDatoSN(periode.getOppgittIMåned());
                perioderSomSkalMed.add(new UttakAktivitetPeriode(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, tidligsteDato.orElseThrow(() -> new IllegalStateException("Fant ikke startdato for SN-perioden.")), periode.getMåned().getTom()));
            }
        }
        return perioderSomSkalMed;
    }

    Optional<LocalDate> finnFraOgMedDatoSN(OppgittOpptjeningDto oppgittIMåned) {
        return oppgittIMåned.getOppgittEgenNæring() != null ?
            oppgittIMåned.getOppgittEgenNæring().stream()
                .map(p -> p.getPeriode().getFom()).findFirst() : Optional.empty();
    }

    Optional<LocalDate> finnFraOgMedDatoFL(OppgittOpptjeningDto oppgittIMåned) {
        return Optional.ofNullable(oppgittIMåned.getOppgittFrilans())
            .map(OppgittFrilansDto::getOppgittFrilansoppdrag)
            .flatMap(f -> f.stream().map(oppgittFrilansoppdragDto -> oppgittFrilansoppdragDto.getPeriode().getFom()).findFirst());
    }

    private boolean utled(List<OppgittFrilansoppdrag> oppgittFrilansoppdrag) {
        Optional<LocalDate> førstFomOpt = oppgittFrilansoppdrag.stream().map(oppdrag -> oppdrag.getPeriode().getFomDato()).min(LocalDate::compareTo);
        if (førstFomOpt.isPresent()) {
            LocalDate føsteFom = førstFomOpt.get();
            // regner ting etter 01.01.2019 som nyoppstartet
            return føsteFom.isAfter(LocalDate.of(2019, 1, 1));
        }
        return false;
    }

    private Optional<List<EgenNæringBuilder>> mapTilEgenNæring(HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder, List<OppgittEgenNæringDto> egenNæring) {
        return Optional.of(egenNæring.stream().map(oppgittEgenNæringDto -> {
            var egenNæringBuilder = EgenNæringBuilder.ny();
            BigDecimal verdi = oppgittEgenNæringDto.getBruttoInntekt().getVerdi();
            egenNæringBuilder.medBruttoInntekt(verdi);
            egenNæringBuilder.medVirksomhetType(VirksomhetType.ANNEN);
            egenNæringBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(oppgittEgenNæringDto.getPeriode().getFom(), oppgittEgenNæringDto.getPeriode().getTom()));
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.SELVSTENDIG_NÆRINGSDRIVENDE, null, verdi);
            return egenNæringBuilder;
        }).collect(Collectors.toList()));
    }

    private Optional<OppgittFrilans> mapTilFrilans(HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder, List<no.nav.k9.sak.kontrakt.arbeidsforhold.OppgittFrilansoppdragDto> oppdragI) {
        var oppgittFrilansoppdrag = oppdragI.stream().map(oppgittFrilansoppdragDto -> {
            var frilansOppdragBuilder = OppgittFrilansOppdragBuilder.ny();
            BigDecimal verdi = oppgittFrilansoppdragDto.getBruttoInntekt().getVerdi();
            frilansOppdragBuilder.medInntekt(verdi);
            frilansOppdragBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(oppgittFrilansoppdragDto.getPeriode().getFom(), oppgittFrilansoppdragDto.getPeriode().getTom()));
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.FRILANS_INNTEKT, null, verdi);
            return frilansOppdragBuilder.build();
        }).collect(Collectors.toList());

        var frilansBuilder = OppgittFrilansBuilder.ny();
        frilansBuilder.medErNyoppstartet(utled(oppgittFrilansoppdrag));
        frilansBuilder.medFrilansOppdrag(oppgittFrilansoppdrag);
        return Optional.of(frilansBuilder.build());
    }
}
