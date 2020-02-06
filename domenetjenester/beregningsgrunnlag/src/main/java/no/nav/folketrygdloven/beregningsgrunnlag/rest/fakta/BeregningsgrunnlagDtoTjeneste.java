package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.GraderingUtenBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Sammenligningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.SammenligningsgrunnlagPrStatus;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.kodeverk.beregningsgrunnlag.SammenligningsgrunnlagType;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.BeregningsgrunnlagDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.BeregningsgrunnlagPeriodeDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.BeregningsgrunnlagPrStatusOgAndelDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.SammenligningsgrunnlagDto;
import no.nav.k9.sak.typer.Beløp;

@ApplicationScoped
public class BeregningsgrunnlagDtoTjeneste {

    private static final int SEKS = 6;
    private final Map<FagsakYtelseType, BiConsumer<BeregningsgrunnlagInput, BeregningsgrunnlagDto>> ytelsespesifikkMapper = Map.of(/* FIXME K9 ytelsesppesifikt grunnlag */);
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private FaktaOmBeregningDtoTjeneste faktaOmBeregningDtoTjeneste;
    private BeregningsgrunnlagPrStatusOgAndelDtoTjeneste andelDtoTjeneste;

    BeregningsgrunnlagDtoTjeneste() {
        // Hibernate
    }

    @Inject
    public BeregningsgrunnlagDtoTjeneste(BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                         FaktaOmBeregningDtoTjeneste faktaOmBeregningDtoTjeneste,
                                         BeregningsgrunnlagPrStatusOgAndelDtoTjeneste andelDtoTjeneste) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.faktaOmBeregningDtoTjeneste = faktaOmBeregningDtoTjeneste;
        this.andelDtoTjeneste = andelDtoTjeneste;
    }

    public Optional<BeregningsgrunnlagDto> lagBeregningsgrunnlagDto(BeregningsgrunnlagInput input) {
        var newInput = leggBeregningsgrunnlagTilInput(input);
        return newInput.map(this::lagDto);
    }

    private Optional<BeregningsgrunnlagInput> leggBeregningsgrunnlagTilInput(BeregningsgrunnlagInput input) {
        var ref = input.getBehandlingReferanse();
        Long behandlingId = ref.getBehandlingId();
        Optional<BeregningsgrunnlagGrunnlagEntitet> grunnlagOpt = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(behandlingId);
        Optional<BeregningsgrunnlagEntitet> beregningsgrunnlagOpt = grunnlagOpt.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);
        if (beregningsgrunnlagOpt.isEmpty() || input.getOpptjeningAktiviteterForBeregning().isEmpty()) {
            return Optional.empty();
        }

        var grunnlag = grunnlagOpt.get();

        return Optional.of(input.medBeregningsgrunnlagGrunnlag(grunnlag));
    }

    private BeregningsgrunnlagDto lagDto(BeregningsgrunnlagInput input) {
        BeregningsgrunnlagDto dto = new BeregningsgrunnlagDto();

        mapSkjæringstidspunkt(input, dto);

        mapFaktaOmBeregning(input, dto);
        mapSammenligningsgrunnlag(input, dto);
        mapSammenlingingsgrunnlagPrStatus(input, dto);

        mapBeregningsgrunnlagAktivitetStatus(input, dto);
        mapBeregningsgrunnlagPerioder(input, dto);
        mapBeløp(input, dto);

        mapAktivitetGradering(input, dto);

        mapDtoYtelsespesifikk(input, dto);

        return dto;
    }

    private void mapFaktaOmBeregning(BeregningsgrunnlagInput input, BeregningsgrunnlagDto dto) {
        faktaOmBeregningDtoTjeneste.lagDto(input).ifPresent(dto::setFaktaOmBeregning);
    }

    private void mapSammenligningsgrunnlag(BeregningsgrunnlagInput input, BeregningsgrunnlagDto dto) {
        var beregningsgrunnlag = input.getBeregningsgrunnlag();
        var sammenligningsgrunnlagDto = lagSammenligningsgrunnlagDto(beregningsgrunnlag);
        sammenligningsgrunnlagDto.ifPresent(dto::setSammenligningsgrunnlag);
    }

    private void mapSammenlingingsgrunnlagPrStatus(BeregningsgrunnlagInput input, BeregningsgrunnlagDto dto) {
        var beregningsgrunnlag = input.getBeregningsgrunnlag();
        List<SammenligningsgrunnlagDto> sammenligningsgrunnlagDto = lagSammenligningsgrunnlagDtoPrStatus(beregningsgrunnlag);
        if (sammenligningsgrunnlagDto.isEmpty() && dto.getSammenligningsgrunnlag() != null) {
            dto.getSammenligningsgrunnlag().setSammenligningsgrunnlagType(SammenligningsgrunnlagType.SAMMENLIGNING_ATFL_SN);
            dto.getSammenligningsgrunnlag().setDifferanseBeregnet(finnDifferanseBeregnetGammeltSammenliningsgrunnlag(beregningsgrunnlag, dto.getSammenligningsgrunnlag().getRapportertPrAar()));
            dto.setSammenligningsgrunnlagPrStatus(List.of(dto.getSammenligningsgrunnlag()));
        } else {
            dto.setSammenligningsgrunnlagPrStatus(sammenligningsgrunnlagDto);
        }
    }

    private BigDecimal finnDifferanseBeregnetGammeltSammenliningsgrunnlag(BeregningsgrunnlagEntitet beregningsgrunnlag, BigDecimal rapportertPrÅr) {
        BigDecimal beregnet;
        if (finnesAndelMedSN(beregningsgrunnlag)) {
            beregnet = hentBeregnetSelvstendigNæringsdrivende(beregningsgrunnlag);
        } else {
            beregnet = hentBeregnetSamletArbeidstakerOgFrilanser(beregningsgrunnlag);
        }
        return beregnet.subtract(rapportertPrÅr);
    }

    private boolean finnesAndelMedSN(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .anyMatch(b -> b.getAktivitetStatus().equals(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE));
    }

    private void mapBeregningsgrunnlagAktivitetStatus(BeregningsgrunnlagInput input, BeregningsgrunnlagDto dto) {
        var beregningsgrunnlag = input.getBeregningsgrunnlag();
        dto.setAktivitetStatus(lagAktivitetStatusListe(beregningsgrunnlag));
    }

    private void mapBeregningsgrunnlagPerioder(BeregningsgrunnlagInput input, BeregningsgrunnlagDto dto) {
        var ref = input.getBehandlingReferanse();
        var overstyrt = finnBgForTilstand(ref, BeregningsgrunnlagTilstand.OPPDATERT_MED_REFUSJON_OG_GRADERING);
        var faktaOmBeregning = finnBgForTilstand(ref, BeregningsgrunnlagTilstand.KOFAKBER_UT);
        var beregningsgrunnlagPerioder = lagBeregningsgrunnlagPeriodeDto(input, faktaOmBeregning, overstyrt);
        dto.setBeregningsgrunnlagPeriode(beregningsgrunnlagPerioder);
    }

    private void mapSkjæringstidspunkt(BeregningsgrunnlagInput input, BeregningsgrunnlagDto dto) {
        var skjæringstidspunktForBeregning = input.getSkjæringstidspunktForBeregning();
        dto.setSkjaeringstidspunktBeregning(skjæringstidspunktForBeregning);
        dto.setSkjæringstidspunkt(skjæringstidspunktForBeregning);
        dto.setLedetekstBrutto("Brutto beregningsgrunnlag");
    }

    private void mapBeløp(BeregningsgrunnlagInput input, BeregningsgrunnlagDto dto) {
        var beregningsgrunnlag = input.getBeregningsgrunnlag();
        Beløp grunnbeløp = Optional.ofNullable(beregningsgrunnlag.getGrunnbeløp()).orElse(Beløp.ZERO);
        long seksG = Math.round(grunnbeløp.getVerdi().multiply(BigDecimal.valueOf(6)).doubleValue());
        BigDecimal halvG = grunnbeløp.getVerdi().divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP);
        dto.setHalvG(halvG);
        dto.setGrunnbeløp(grunnbeløp.getVerdi());
        dto.setHjemmel(beregningsgrunnlag.getHjemmel());
        dto.setLedetekstAvkortet("Avkortet beregningsgrunnlag (6G=" + seksG + ")");

        // Det skal vises et tall som "oppsumering" av årsinntekt i GUI, innebærer nok logikk til at det bør utledes backend
        FinnÅrsinntektvisningstall.finn(beregningsgrunnlag).ifPresent(dto::setÅrsinntektVisningstall);
    }

    private void mapAktivitetGradering(BeregningsgrunnlagInput input, BeregningsgrunnlagDto dto) {
        var beregningsgrunnlag = input.getBeregningsgrunnlag();
        var aktivitetGradering = input.getAktivitetGradering();
        var andelerMedGraderingUtenBG = GraderingUtenBeregningsgrunnlagTjeneste.finnAndelerMedGraderingUtenBG(beregningsgrunnlag,
            aktivitetGradering);
        if (!andelerMedGraderingUtenBG.isEmpty()) {
            dto.setAndelerMedGraderingUtenBG(andelDtoTjeneste
                .lagBeregningsgrunnlagPrStatusOgAndelDto(input, andelerMedGraderingUtenBG, Collections.emptyList(), Collections.emptyList()));
        }
    }

    private void mapDtoYtelsespesifikk(BeregningsgrunnlagInput input, BeregningsgrunnlagDto dto) {
        this.ytelsespesifikkMapper.getOrDefault(input.getFagsakYtelseType(), (in, dt) -> {
        }).accept(input, dto);
    }

    private Optional<BeregningsgrunnlagEntitet> finnBgForTilstand(BehandlingReferanse ref,
                                                                  BeregningsgrunnlagTilstand tilstand) {
        return beregningsgrunnlagRepository
            .hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(ref.getBehandlingId(), ref.getOriginalBehandlingId(),
                tilstand)
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);
    }

    private Optional<SammenligningsgrunnlagDto> lagSammenligningsgrunnlagDto(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        if (beregningsgrunnlag.getSammenligningsgrunnlag() == null) {
            return Optional.empty();
        }
        Sammenligningsgrunnlag sammenligningsgrunnlag = beregningsgrunnlag.getSammenligningsgrunnlag();
        SammenligningsgrunnlagDto dto = new SammenligningsgrunnlagDto();
        dto.setSammenligningsgrunnlagFom(sammenligningsgrunnlag.getSammenligningsperiodeFom());
        dto.setSammenligningsgrunnlagTom(sammenligningsgrunnlag.getSammenligningsperiodeTom());
        dto.setRapportertPrAar(sammenligningsgrunnlag.getRapportertPrÅr());
        dto.setAvvikPromille(sammenligningsgrunnlag.getAvvikPromille());
        BigDecimal avvikProsent = sammenligningsgrunnlag.getAvvikPromille() == null ? null
            : BigDecimal.valueOf(sammenligningsgrunnlag.getAvvikPromille()).divide(BigDecimal.TEN, 1, RoundingMode.HALF_EVEN);
        dto.setAvvikProsent(avvikProsent);
        return Optional.of(dto);
    }

    private List<SammenligningsgrunnlagDto> lagSammenligningsgrunnlagDtoPrStatus(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        List<SammenligningsgrunnlagDto> sammenligningsgrunnlagDtos = new ArrayList<>();
        beregningsgrunnlag.getSammenligningsgrunnlagPrStatusListe().forEach(s -> {
            SammenligningsgrunnlagDto dto = new SammenligningsgrunnlagDto();
            dto.setSammenligningsgrunnlagFom(s.getSammenligningsperiodeFom());
            dto.setSammenligningsgrunnlagTom(s.getSammenligningsperiodeTom());
            dto.setRapportertPrAar(s.getRapportertPrÅr());
            dto.setAvvikPromille(s.getAvvikPromille());
            BigDecimal avvikProsent = s.getAvvikPromille() == null ? null : BigDecimal.valueOf(s.getAvvikPromille()).divide(BigDecimal.TEN, 1, RoundingMode.HALF_EVEN);
            dto.setAvvikProsent(avvikProsent);
            dto.setSammenligningsgrunnlagType(s.getSammenligningsgrunnlagType());
            dto.setDifferanseBeregnet(finnDifferanseBeregnet(beregningsgrunnlag, s));
            sammenligningsgrunnlagDtos.add(dto);
        });
        return sammenligningsgrunnlagDtos;

    }

    private BigDecimal finnDifferanseBeregnet(BeregningsgrunnlagEntitet beregningsgrunnlag, SammenligningsgrunnlagPrStatus sammenligningsgrunnlagPrStatus) {
        BigDecimal beregnet;
        if (sammenligningsgrunnlagPrStatus.getSammenligningsgrunnlagType().equals(SammenligningsgrunnlagType.SAMMENLIGNING_AT)) {
            beregnet = hentBeregnetArbeidstaker(beregningsgrunnlag);
        } else if (sammenligningsgrunnlagPrStatus.getSammenligningsgrunnlagType().equals(SammenligningsgrunnlagType.SAMMENLIGNING_FL)) {
            beregnet = hentBeregnetFrilanser(beregningsgrunnlag);
        } else if (sammenligningsgrunnlagPrStatus.getSammenligningsgrunnlagType().equals(SammenligningsgrunnlagType.SAMMENLIGNING_SN)) {
            beregnet = hentBeregnetSelvstendigNæringsdrivende(beregningsgrunnlag);
        } else {
            beregnet = hentBeregnetSamletArbeidstakerOgFrilanser(beregningsgrunnlag);
        }
        return beregnet.subtract(sammenligningsgrunnlagPrStatus.getRapportertPrÅr());
    }

    private BigDecimal hentBeregnetArbeidstaker(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(b -> b.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSTAKER))
            .map(BeregningsgrunnlagPrStatusOgAndel::getBruttoInkludertNaturalYtelser)
            .filter(Objects::nonNull)
            .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
    }

    private BigDecimal hentBeregnetFrilanser(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(b -> b.getAktivitetStatus().equals(AktivitetStatus.FRILANSER))
            .map(BeregningsgrunnlagPrStatusOgAndel::getBruttoPrÅr)
            .filter(Objects::nonNull)
            .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
    }

    private BigDecimal hentBeregnetSelvstendigNæringsdrivende(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(b -> b.getAktivitetStatus().equals(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE))
            .map(BeregningsgrunnlagPrStatusOgAndel::getPgiSnitt)
            .filter(Objects::nonNull)
            .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
    }

    private BigDecimal hentBeregnetSamletArbeidstakerOgFrilanser(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(b -> b.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSTAKER) || b.getAktivitetStatus().equals(AktivitetStatus.FRILANSER))
            .map(BeregningsgrunnlagPrStatusOgAndel::getBruttoInkludertNaturalYtelser)
            .filter(Objects::nonNull)
            .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
    }

    private List<BeregningsgrunnlagPeriodeDto> lagBeregningsgrunnlagPeriodeDto(BeregningsgrunnlagInput input,
                                                                               Optional<BeregningsgrunnlagEntitet> fakta,
                                                                               Optional<BeregningsgrunnlagEntitet> overstyrt) {
        List<BeregningsgrunnlagPeriodeDto> dtoList = new ArrayList<>();
        var beregningsgrunnlag = input.getBeregningsgrunnlag();
        List<BeregningsgrunnlagPeriode> beregningsgrunnlagPerioder = beregningsgrunnlag.getBeregningsgrunnlagPerioder();
        for (BeregningsgrunnlagPeriode periode : beregningsgrunnlagPerioder) {
            BeregningsgrunnlagPeriodeDto dto = lagBeregningsgrunnlagPeriode(input, fakta, overstyrt, periode);
            dtoList.add(dto);
        }
        return dtoList;
    }

    private BeregningsgrunnlagPeriodeDto lagBeregningsgrunnlagPeriode(BeregningsgrunnlagInput input,
                                                                      Optional<BeregningsgrunnlagEntitet> fakta,
                                                                      Optional<BeregningsgrunnlagEntitet> overstyrt,
                                                                      BeregningsgrunnlagPeriode periode) {
        BeregningsgrunnlagPeriodeDto dto = new BeregningsgrunnlagPeriodeDto();
        dto.setBeregningsgrunnlagPeriodeFom(periode.getBeregningsgrunnlagPeriodeFom());
        dto.setBeregningsgrunnlagPeriodeTom(periode.getBeregningsgrunnlagPeriodeTom());
        dto.setBeregnetPrAar(periode.getBeregnetPrÅr());
        dto.setBruttoPrAar(periode.getBruttoPrÅr());
        BigDecimal bruttoInkludertBortfaltNaturalytelsePrAar = periode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .map(BeregningsgrunnlagPrStatusOgAndel::getBruttoInkludertNaturalYtelser)
            .filter(Objects::nonNull)
            .reduce(BigDecimal::add)
            .orElse(null);
        dto.setBruttoInkludertBortfaltNaturalytelsePrAar(bruttoInkludertBortfaltNaturalytelsePrAar);
        dto.setAvkortetPrAar(finnAvkortetUtenGraderingPrÅr(bruttoInkludertBortfaltNaturalytelsePrAar, periode));
        dto.setRedusertPrAar(periode.getRedusertPrÅr());
        dto.setDagsats(periode.getDagsats());
        dto.leggTilPeriodeAarsaker(periode.getPeriodeÅrsaker());
        List<BeregningsgrunnlagPrStatusOgAndel> faktaAndelListe = finnAndelerIBgPeriodeFraGrunnlag(fakta, periode);
        List<BeregningsgrunnlagPrStatusOgAndel> fastsattAndelListe = finnAndelerIBgPeriodeFraGrunnlag(overstyrt, periode);
        dto.setAndeler(
            andelDtoTjeneste.lagBeregningsgrunnlagPrStatusOgAndelDto(input, periode.getBeregningsgrunnlagPrStatusOgAndelList(), faktaAndelListe,
                fastsattAndelListe));
        leggTilAndelerLagtTilAvSaksbehandlerIForrige(input, fakta, periode, dto);
        return dto;
    }

    private BigDecimal finnAvkortetUtenGraderingPrÅr(BigDecimal bruttoInkludertBortfaltNaturalytelsePrAar, BeregningsgrunnlagPeriode periode) {
        if (bruttoInkludertBortfaltNaturalytelsePrAar == null) {
            return null;
        }
        BigDecimal seksG = periode.getBeregningsgrunnlag().getGrunnbeløp().multipliser(SEKS).getVerdi();
        return bruttoInkludertBortfaltNaturalytelsePrAar.compareTo(seksG) > 0 ? seksG : bruttoInkludertBortfaltNaturalytelsePrAar;
    }

    private void leggTilAndelerLagtTilAvSaksbehandlerIForrige(BeregningsgrunnlagInput input,
                                                              Optional<BeregningsgrunnlagEntitet> fakta,
                                                              BeregningsgrunnlagPeriode periode,
                                                              BeregningsgrunnlagPeriodeDto dto) {
        if (dto.getBeregningsgrunnlagPrStatusOgAndel().stream().noneMatch(BeregningsgrunnlagPrStatusOgAndelDto::getLagtTilAvSaksbehandler)) {
            Optional<BeregningsgrunnlagPeriode> fastsattPeriode = fakta.stream()
                .flatMap(bg -> bg.getBeregningsgrunnlagPerioder().stream())
                .filter(periode1 -> periode1.getPeriode().overlapper(periode.getPeriode()))
                .findFirst();
            List<BeregningsgrunnlagPrStatusOgAndel> andelerLagtTilAvSaksbehandler = fastsattPeriode.stream()
                .flatMap(periode1 -> periode1.getBeregningsgrunnlagPrStatusOgAndelList().stream())
                .filter(BeregningsgrunnlagPrStatusOgAndel::getLagtTilAvSaksbehandler).collect(Collectors.toList());
            dto.setAndelerLagtTilManueltIForrige(andelDtoTjeneste
                .lagBeregningsgrunnlagPrStatusOgAndelDto(input, andelerLagtTilAvSaksbehandler, Collections.emptyList(), Collections.emptyList()));
        }
    }

    private List<BeregningsgrunnlagPrStatusOgAndel> finnAndelerIBgPeriodeFraGrunnlag(Optional<BeregningsgrunnlagEntitet> tidligereGrunnlag,
                                                                                     BeregningsgrunnlagPeriode periode) {
        Optional<BeregningsgrunnlagPeriode> periodeITidligereGrunnlag = finnPeriodeIGrunnlag(tidligereGrunnlag, periode);
        return periodeITidligereGrunnlag.stream()
            .flatMap(p -> p.getBeregningsgrunnlagPrStatusOgAndelList().stream()).collect(Collectors.toList());
    }

    private Optional<BeregningsgrunnlagPeriode> finnPeriodeIGrunnlag(Optional<BeregningsgrunnlagEntitet> grunnlag, BeregningsgrunnlagPeriode periode) {
        return grunnlag.stream()
            .flatMap(bg -> bg.getBeregningsgrunnlagPerioder().stream())
            .filter(p -> periode.getPeriode().inkluderer(p.getPeriode().getFomDato()))
            .findFirst();
    }

    private List<AktivitetStatus> lagAktivitetStatusListe(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        ArrayList<AktivitetStatus> statusListe = new ArrayList<>();
        for (BeregningsgrunnlagAktivitetStatus status : beregningsgrunnlag.getAktivitetStatuser()) {
            statusListe.add(status.getAktivitetStatus());
        }
        return statusListe;
    }

}
