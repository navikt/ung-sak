package no.nav.k9.sak.web.app.tjenester.behandling.arbeidsforhold;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.abakus.iaygrunnlag.kodeverk.VirksomhetType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.OppgittFrilans;
import no.nav.k9.sak.domene.iay.modell.OppgittFrilansoppdrag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.EgenNæringBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittFrilansBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittFrilansOppdragBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.arbeidsforhold.BekreftOverstyrOppgittOpptjeningDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.OppgittEgenNæringDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.OppgittFrilansDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.OppgittOpptjeningDto;
import no.nav.k9.sak.kontrakt.frisinn.PeriodeMedSNOgFLDto;
import no.nav.k9.sak.kontrakt.frisinn.SøknadsperiodeOgOppgittOpptjeningV2Dto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftOverstyrOppgittOpptjeningDto.class, adapter = AksjonspunktOppdaterer.class)
public class OverstyringOppgittOpptjeningOppdaterer implements AksjonspunktOppdaterer<BekreftOverstyrOppgittOpptjeningDto> {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private UttakRepository uttakRepository;
    private HistorikkTjenesteAdapter historikkAdapter;

    OverstyringOppgittOpptjeningOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public OverstyringOppgittOpptjeningOppdaterer(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste, UttakRepository uttakRepository, HistorikkTjenesteAdapter historikkAdapter) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.uttakRepository = uttakRepository;
        this.historikkAdapter = historikkAdapter;
    }

    @Override
    public OppdateringResultat oppdater(BekreftOverstyrOppgittOpptjeningDto dto, AksjonspunktOppdaterParameter param) {
        var historikkInnslagTekstBuilder = historikkAdapter.tekstBuilder();
        var oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny();
        var søknadsperiodeOgOppgittOpptjening = dto.getSøknadsperiodeOgOppgittOpptjeningDto();
        leggerTilEgenæring(søknadsperiodeOgOppgittOpptjening, historikkInnslagTekstBuilder).ifPresent(oppgittOpptjeningBuilder::leggTilEgneNæringer);
        leggerTilFrilans(søknadsperiodeOgOppgittOpptjening, historikkInnslagTekstBuilder).ifPresent(oppgittOpptjeningBuilder::leggTilFrilansOpplysninger);

        var perioderSomSkalMed = utledePerioder(dto);
        uttakRepository.lagreOgFlushFastsattUttak(param.getBehandlingId(), new UttakAktivitet(perioderSomSkalMed));

        inntektArbeidYtelseTjeneste.lagreOverstyrtOppgittOpptjening(param.getBehandlingId(), oppgittOpptjeningBuilder);

        historikkAdapter.opprettHistorikkInnslag(param.getBehandlingId(), HistorikkinnslagType.FAKTA_ENDRET);
        return OppdateringResultat.utenOveropp();
    }

    private ArrayList<UttakAktivitetPeriode> utledePerioder(BekreftOverstyrOppgittOpptjeningDto dto) {
        var periodeFraSøknad = dto.getSøknadsperiodeOgOppgittOpptjeningDto().getMåneder();
        var perioderSomSkalMed = new ArrayList<UttakAktivitetPeriode>();

        for (PeriodeMedSNOgFLDto periode : periodeFraSøknad) {
            if (periode.getSøkerFL()) {
                var tidligsteDato = finnFraOgMedDatoFL(periode.getOppgittIMåned());
                perioderSomSkalMed.add(new UttakAktivitetPeriode(UttakArbeidType.FRILANSER, tidligsteDato, periode.getMåned().getTom()));
            }
            if (periode.getSøkerSN()) {
                var tidligsteDato = finnFraOgMedDatoSN(periode.getOppgittIMåned());
                perioderSomSkalMed.add(new UttakAktivitetPeriode(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, tidligsteDato, periode.getMåned().getTom()));
            }
        }
        return perioderSomSkalMed;
    }

    LocalDate finnFraOgMedDatoSN(OppgittOpptjeningDto oppgittIMåned) {
        return oppgittIMåned.getOppgittEgenNæring()
            .stream()
            .map(egenNæringDto -> egenNæringDto.getPeriode().getFom())
            .min(LocalDate::compareTo)
            .orElseThrow(() -> new IllegalStateException("Fant ikke startdato for SN-perioden."));
    }

    LocalDate finnFraOgMedDatoFL(OppgittOpptjeningDto oppgittIMåned) {
        var oppgittFrilans = oppgittIMåned.getOppgittFrilans();
        return Optional.ofNullable(oppgittFrilans)
                .orElseThrow(() -> new IllegalStateException("Fant ikke startdato for FL-perioden."))
                .getOppgittFrilansoppdrag().stream().map(frilans -> frilans.getPeriode().getFom()).min(LocalDate::compareTo)
                .orElseThrow(() -> new IllegalStateException("Fant ikke startdato for FL-perioden."));

    }

    private Optional<OppgittFrilans> leggerTilFrilans(SøknadsperiodeOgOppgittOpptjeningV2Dto søknadsperiodeOgOppgittOpptjening, HistorikkInnslagTekstBuilder builder) {
        var oppdragI = søknadsperiodeOgOppgittOpptjening.getMåneder()
            .stream().map(m -> m.getOppgittIMåned().getOppgittFrilans())
            .map(OppgittFrilansDto::getOppgittFrilansoppdrag)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        if (!oppdragI.isEmpty()) {
            var oppgittFrilansoppdrag = oppdragI.stream().map(oppgittFrilansoppdragDto -> {
                var frilansOppdragBuilder = OppgittFrilansOppdragBuilder.ny();
                BigDecimal verdi = oppgittFrilansoppdragDto.getBruttoInntekt().getVerdi();
                frilansOppdragBuilder.medInntekt(verdi);
                frilansOppdragBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(oppgittFrilansoppdragDto.getPeriode().getFom(), oppgittFrilansoppdragDto.getPeriode().getTom()));
                builder.medEndretFelt(HistorikkEndretFeltType.FRILANS_INNTEKT, null, verdi);
                return frilansOppdragBuilder.build();
            }).collect(Collectors.toList());

            var frilansBuilder = OppgittFrilansBuilder.ny();
            frilansBuilder.medErNyoppstartet(utled(oppgittFrilansoppdrag));
            frilansBuilder.medFrilansOppdrag(oppgittFrilansoppdrag);
            return Optional.of(frilansBuilder.build());
        }
        return Optional.empty();
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

    private Optional<List<EgenNæringBuilder>> leggerTilEgenæring(SøknadsperiodeOgOppgittOpptjeningV2Dto søknadsperiodeOgOppgittOpptjening, HistorikkInnslagTekstBuilder builder) {
        List<OppgittEgenNæringDto> egenNæring = new ArrayList<>();
        Optional.ofNullable(søknadsperiodeOgOppgittOpptjening.getFørSøkerPerioden().getOppgittEgenNæring()).ifPresent(egenNæring::addAll);
        egenNæring.addAll(søknadsperiodeOgOppgittOpptjening.getMåneder()
            .stream().map(o -> o.getOppgittIMåned().getOppgittEgenNæring())
            .flatMap(Collection::stream)
            .collect(Collectors.toList()));

        if (!egenNæring.isEmpty()) {
            return Optional.of(egenNæring.stream().map(oppgittEgenNæringDto -> {
                var egenNæringBuilder = EgenNæringBuilder.ny();
                BigDecimal verdi = oppgittEgenNæringDto.getBruttoInntekt().getVerdi();
                egenNæringBuilder.medBruttoInntekt(verdi);
                egenNæringBuilder.medVirksomhetType(VirksomhetType.ANNEN);
                egenNæringBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(oppgittEgenNæringDto.getPeriode().getFom(), oppgittEgenNæringDto.getPeriode().getTom()));
                builder.medEndretFelt(HistorikkEndretFeltType.SELVSTENDIG_NÆRINGSDRIVENDE, null, verdi);
                return egenNæringBuilder;
            }).collect(Collectors.toList()));
        }
        return Optional.empty();
    }
}
