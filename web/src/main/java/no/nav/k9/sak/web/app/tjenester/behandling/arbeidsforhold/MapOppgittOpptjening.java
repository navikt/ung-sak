package no.nav.k9.sak.web.app.tjenester.behandling.arbeidsforhold;

import static no.nav.k9.felles.konfigurasjon.konfig.Tid.TIDENES_ENDE;
import static no.nav.k9.sak.ytelse.frisinn.mapper.FrisinnMapper.SISTE_DAG_I_MARS;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningForBeregningTjeneste;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef.Lookup;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.kontrakt.arbeidsforhold.OppgittArbeidsforholdDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.OppgittEgenNæringDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.OppgittFrilansDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.OppgittFrilansoppdragDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.OppgittOpptjeningDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.PeriodeDto;
import no.nav.k9.sak.kontrakt.frisinn.PeriodeMedSNOgFLDto;
import no.nav.k9.sak.kontrakt.frisinn.SøknadsperiodeOgOppgittOpptjeningV2Dto;
import no.nav.k9.sak.typer.Beløp;
import no.nav.k9.sak.ytelse.frisinn.mapper.FrisinnMapper;

@Dependent
class MapOppgittOpptjening {

    public static final LocalDate FØRSTE_MULIGE_SØKNADPERIODE_START = SISTE_DAG_I_MARS;

    private UttakRepository uttakRepository;
    private Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjeneste;

    @Inject
    MapOppgittOpptjening(UttakRepository uttakRepository,
                         @Any Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjeneste) {
        this.uttakRepository = uttakRepository;
        this.opptjeningForBeregningTjeneste = opptjeningForBeregningTjeneste;
    }

    SøknadsperiodeOgOppgittOpptjeningV2Dto mapOppgittOpptjening(Behandling behandling, InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        var dto = new SøknadsperiodeOgOppgittOpptjeningV2Dto();

        OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste = Lookup.find(this.opptjeningForBeregningTjeneste, behandling.getFagsakYtelseType()).orElseThrow();
        // TODO ESPEN: BRUKES BARE FOR FRISINN, BØR FLYTTES TIL YTELSESSPESIFIKK MODUL
        LocalDate stp = null;
        var ref = BehandlingReferanse.fra(behandling);
        Optional<OppgittOpptjening> oppgittOpptjeningOpt = opptjeningForBeregningTjeneste.finnOppgittOpptjening(ref, inntektArbeidYtelseGrunnlag, stp);

        if (oppgittOpptjeningOpt.isEmpty()) {
            return dto;
        }
        OppgittOpptjening oppgittOpptjening = oppgittOpptjeningOpt.get();

        OppgittOpptjeningDto oppgittOpptjeningDto = new OppgittOpptjeningDto();

        if (oppgittOpptjening.getFrilans().isPresent()) {
            OppgittFrilansDto oppgittFrilansDto = new OppgittFrilansDto();
            oppgittFrilansDto.setOppgittFrilansoppdrag(oppgittOpptjening.getFrilans().get().getFrilansoppdrag().stream().map(frilansoppdrag -> {
                OppgittFrilansoppdragDto oppgittFrilansoppdragDto = new OppgittFrilansoppdragDto();
                oppgittFrilansoppdragDto.setBruttoInntekt(new Beløp(frilansoppdrag.getInntekt()));
                oppgittFrilansoppdragDto.setPeriode(new PeriodeDto(frilansoppdrag.getPeriode().getFomDato(), frilansoppdrag.getPeriode().getTomDato()));
                return oppgittFrilansoppdragDto;
            }).collect(Collectors.toList()));
            oppgittOpptjeningDto.setOppgittFrilans(oppgittFrilansDto);
        }
        List<OppgittEgenNæringDto> egenNæringList = oppgittOpptjening.getEgenNæring().stream().map(egen -> {
            OppgittEgenNæringDto oppgittEgenNæringDto = new OppgittEgenNæringDto();
            oppgittEgenNæringDto.setBruttoInntekt(new Beløp(egen.getBruttoInntekt()));
            oppgittEgenNæringDto.setPeriode(new PeriodeDto(egen.getPeriode().getFomDato(), egen.getPeriode().getTomDato()));
            return oppgittEgenNæringDto;
        }).collect(Collectors.toList());
        oppgittOpptjeningDto.setOppgittEgenNæring(egenNæringList);

        List<OppgittArbeidsforholdDto> oppgittArbeidsforhold = oppgittOpptjening.getOppgittArbeidsforhold().stream().map(arb -> {
            var arbeidsforholdDto = new OppgittArbeidsforholdDto();
            arbeidsforholdDto.setInntekt(new Beløp(arb.getInntekt()));
            arbeidsforholdDto.setPeriode(new PeriodeDto(arb.getPeriode().getFomDato(), arb.getPeriode().getTomDato()));
            return arbeidsforholdDto;
        }).collect(Collectors.toList());
        oppgittOpptjeningDto.setOppgittArbeidsforhold(oppgittArbeidsforhold);

        var fastsattUttak = uttakRepository.hentFastsattUttak(behandling.getId());
        var oppgittUttak = uttakRepository.hentOppgittUttak(behandling.getId());
        var perioder = FrisinnMapper.finnMåneder(oppgittUttak);
        List<PeriodeMedSNOgFLDto> iSøknad = perioder
            .stream()
            .map(periode -> map(periode, mapTilPeriode(oppgittOpptjeningDto, periode), fastsattUttak))
            .collect(Collectors.toList());

        if (!fastsattUttak.getPerioder().isEmpty()) {
            OppgittOpptjeningDto førSøknad = mapUtenomPeriode(oppgittOpptjeningDto,
                new PeriodeDto(fastsattUttak.getMaksPeriode().getFomDato(), fastsattUttak.getMaksPeriode().getTomDato()));
            dto.setFørSøkerPerioden(førSøknad);
        } else {
            OppgittOpptjeningDto førSøknad = mapUtenomPeriode(oppgittOpptjeningDto, new PeriodeDto(FØRSTE_MULIGE_SØKNADPERIODE_START, TIDENES_ENDE));
            dto.setFørSøkerPerioden(førSøknad);
        }
        dto.setMåneder(iSøknad);
        return dto;
    }

    private OppgittOpptjeningDto mapTilPeriode(OppgittOpptjeningDto oppgittOpptjeningDto, PeriodeDto periodeFraSøknad) {
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(periodeFraSøknad.getFom(), periodeFraSøknad.getTom());
        OppgittOpptjeningDto dto = new OppgittOpptjeningDto();

        dto.setOppgittEgenNæring(oppgittOpptjeningDto.getOppgittEgenNæring()
            .stream()
            .filter(oppgittEgenNæringDto -> periode.overlapper(oppgittEgenNæringDto.getPeriode().getFom(), oppgittEgenNæringDto.getPeriode().getFom()))
            .collect(Collectors.toList()));

        dto.setOppgittArbeidsforhold(oppgittOpptjeningDto.getOppgittArbeidsforhold()
            .stream()
            .filter(oppgittArbeidsforholdDto -> periode.overlapper(oppgittArbeidsforholdDto.getPeriode().getFom(), oppgittArbeidsforholdDto.getPeriode().getFom()))
            .collect(Collectors.toList()));

        if (oppgittOpptjeningDto.getOppgittFrilans() != null) {
            OppgittFrilansDto oppgittFrilansDto = new OppgittFrilansDto();
            oppgittFrilansDto.setOppgittFrilansoppdrag(oppgittOpptjeningDto.getOppgittFrilans().getOppgittFrilansoppdrag().stream()
                .filter(oppgittFrilansoppdragDto -> periode.overlapper(oppgittFrilansoppdragDto.getPeriode().getFom(), oppgittFrilansoppdragDto.getPeriode().getFom())).collect(Collectors.toList()));
            dto.setOppgittFrilans(oppgittFrilansDto);

        }
        return dto;
    }

    private OppgittOpptjeningDto mapUtenomPeriode(OppgittOpptjeningDto oppgittOpptjeningDto, PeriodeDto periodeFraSøknad) {
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(periodeFraSøknad.getFom(), periodeFraSøknad.getTom());
        OppgittOpptjeningDto dto = new OppgittOpptjeningDto();

        dto.setOppgittEgenNæring(oppgittOpptjeningDto.getOppgittEgenNæring().stream()
            .filter(oppgittEgenNæringDto -> !periode.overlapper(oppgittEgenNæringDto.getPeriode().getFom(), oppgittEgenNæringDto.getPeriode().getFom()))
            .sorted(Comparator.comparing(o -> o.getPeriode().getFom()))
            .collect(Collectors.toList()));

        if (oppgittOpptjeningDto.getOppgittFrilans() != null) {
            OppgittFrilansDto oppgittFrilansDto = new OppgittFrilansDto();
            oppgittFrilansDto.setOppgittFrilansoppdrag(oppgittOpptjeningDto.getOppgittFrilans().getOppgittFrilansoppdrag().stream()
                .filter(oppgittFrilansoppdragDto -> !periode.overlapper(oppgittFrilansoppdragDto.getPeriode().getFom(), oppgittFrilansoppdragDto.getPeriode().getFom()))
                .sorted(Comparator.comparing(f -> f.getPeriode().getFom()))
                .collect(Collectors.toList()));
            dto.setOppgittFrilans(oppgittFrilansDto);
        }
        return dto;
    }

    private PeriodeMedSNOgFLDto map(PeriodeDto måned, OppgittOpptjeningDto dto, UttakAktivitet fastsattUttak) {
        var resultatDto = new PeriodeMedSNOgFLDto();
        resultatDto.setMåned(måned);
        resultatDto.setOppgittIMåned(dto);
        resultatDto.setSøkerFL(fastsattUttak.getPerioder().stream().anyMatch(p -> måned.getTom().equals(p.getPeriode().getTomDato()) && p.getAktivitetType() == UttakArbeidType.FRILANSER));
        resultatDto.setSøkerSN(fastsattUttak.getPerioder().stream().anyMatch(p -> måned.getTom().equals(p.getPeriode().getTomDato()) && p.getAktivitetType() == UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE));
        return resultatDto;
    }
}
