package no.nav.k9.sak.web.app.tjenester.behandling.arbeidsforhold;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
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
import no.nav.k9.sak.kontrakt.arbeidsforhold.PeriodeDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.SøknadsperiodeOgOppgittOpptjeningDto;

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
        HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder = historikkAdapter.tekstBuilder();
        var oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny();
        var søknadsperiodeOgOppgittOpptjening = dto.getSøknadsperiodeOgOppgittOpptjeningDto();
        leggerTilEgenæring(søknadsperiodeOgOppgittOpptjening, historikkInnslagTekstBuilder).ifPresent(oppgittOpptjeningBuilder::leggTilEgneNæringer);
        leggerTilFrilans(søknadsperiodeOgOppgittOpptjening, historikkInnslagTekstBuilder).ifPresent(oppgittOpptjeningBuilder::leggTilFrilansOpplysninger);

        ArrayList<UttakAktivitetPeriode> perioderSomSkalMed = utledePerioder(dto);
        uttakRepository.lagreOgFlushFastsattUttak(param.getBehandlingId(), new UttakAktivitet(perioderSomSkalMed));

        inntektArbeidYtelseTjeneste.lagreOverstyrtOppgittOpptjening(param.getBehandlingId(), oppgittOpptjeningBuilder);

        historikkAdapter.opprettHistorikkInnslag(param.getBehandlingId(), HistorikkinnslagType.FAKTA_ENDRET);
        return OppdateringResultat.utenOveropp();
    }

    private ArrayList<UttakAktivitetPeriode> utledePerioder(BekreftOverstyrOppgittOpptjeningDto dto) {
        PeriodeDto periodeFraSøknad = dto.getSøknadsperiodeOgOppgittOpptjeningDto().getPeriodeFraSøknad();


        var perioderSomSkalMed = new ArrayList<UttakAktivitetPeriode>();
        if (dto.getSøknadsperiodeOgOppgittOpptjeningDto().getSøkerYtelseForFrilans()) {
            var tidligsteDato = finnFraOgMedDatoFL(dto.getSøknadsperiodeOgOppgittOpptjeningDto());
            perioderSomSkalMed.add(new UttakAktivitetPeriode(UttakArbeidType.FRILANSER, tidligsteDato, periodeFraSøknad.getTom()));
        }
        if (dto.getSøknadsperiodeOgOppgittOpptjeningDto().getSøkerYtelseForNæring()) {
            var tidligsteDato = finnFraOgMedDatoSN(dto.getSøknadsperiodeOgOppgittOpptjeningDto());
            perioderSomSkalMed.add(new UttakAktivitetPeriode(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, tidligsteDato, periodeFraSøknad.getTom()));
        }
        return perioderSomSkalMed;
    }

    LocalDate finnFraOgMedDatoSN(SøknadsperiodeOgOppgittOpptjeningDto dto) {
        OppgittOpptjeningDto iSøkerPerioden = dto.getISøkerPerioden();
        return iSøkerPerioden.getOppgittEgenNæring().stream().map(egenNæringDto -> egenNæringDto.getPeriode().getFom()).min(LocalDate::compareTo).orElseThrow(() -> new IllegalStateException("Fant ikke startdato for SN-perioden."));
    }

    LocalDate finnFraOgMedDatoFL(SøknadsperiodeOgOppgittOpptjeningDto dto) {
        OppgittOpptjeningDto iSøkerPerioden = dto.getISøkerPerioden();
        OppgittFrilansDto oppgittFrilans = iSøkerPerioden.getOppgittFrilans();

        return Optional.ofNullable(oppgittFrilans)
                .orElseThrow(() -> new IllegalStateException("Fant ikke startdato for FL-perioden."))
                .getOppgittFrilansoppdrag().stream().map(frilans -> frilans.getPeriode().getFom()).min(LocalDate::compareTo)
                .orElseThrow(() -> new IllegalStateException("Fant ikke startdato for FL-perioden."));

    }

    private Optional<OppgittFrilans> leggerTilFrilans(SøknadsperiodeOgOppgittOpptjeningDto søknadsperiodeOgOppgittOpptjening, HistorikkInnslagTekstBuilder builder) {
        var frilansI = søknadsperiodeOgOppgittOpptjening.getISøkerPerioden().getOppgittFrilans();
        var frilansFør = søknadsperiodeOgOppgittOpptjening.getFørSøkerPerioden().getOppgittFrilans();

        if (frilansI != null || frilansFør != null) {
            var oppdrag = Optional.ofNullable(frilansI).map(OppgittFrilansDto::getOppgittFrilansoppdrag).orElse(Collections.emptyList());
            oppdrag.addAll(Optional.ofNullable(frilansFør).map(OppgittFrilansDto::getOppgittFrilansoppdrag).orElse(Collections.emptyList()));

            var oppgittFrilansoppdrag = oppdrag.stream().map(oppgittFrilansoppdragDto -> {
                var frilansOppdragBuilder = OppgittFrilansOppdragBuilder.ny();
                BigDecimal verdi = oppgittFrilansoppdragDto.getBruttoInntekt().getVerdi();
                frilansOppdragBuilder.medInntekt(verdi);
                frilansOppdragBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(oppgittFrilansoppdragDto.getPeriode().getFom(), oppgittFrilansoppdragDto.getPeriode().getTom()));
                builder.medEndretFelt(HistorikkEndretFeltType.FRILANS_INNTEKT, null, verdi);
                return frilansOppdragBuilder.build();
            }).collect(Collectors.toList());

            var frilansBuilder = OppgittFrilansBuilder.ny();
            frilansBuilder.medErNyoppstartet(utled(oppgittFrilansoppdrag));
            frilansBuilder.leggTilOppgittOppdrag(oppgittFrilansoppdrag);
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

    private Optional<List<EgenNæringBuilder>> leggerTilEgenæring(SøknadsperiodeOgOppgittOpptjeningDto søknadsperiodeOgOppgittOpptjening, HistorikkInnslagTekstBuilder builder) {
        List<OppgittEgenNæringDto> egenNæring = new ArrayList<>();
        Optional.ofNullable(søknadsperiodeOgOppgittOpptjening.getFørSøkerPerioden().getOppgittEgenNæring()).ifPresent(egenNæring::addAll);
        Optional.ofNullable(søknadsperiodeOgOppgittOpptjening.getISøkerPerioden().getOppgittEgenNæring()).ifPresent(egenNæring::addAll);

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
