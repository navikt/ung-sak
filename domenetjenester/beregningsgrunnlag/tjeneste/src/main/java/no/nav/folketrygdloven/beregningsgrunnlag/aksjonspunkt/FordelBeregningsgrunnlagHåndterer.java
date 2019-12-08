package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsatteVerdierDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsettBeregningsgrunnlagAndelDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsettBeregningsgrunnlagPeriodeDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FordelBeregningsgrunnlagDto;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.behandlingslager.Kopimaskin;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

@ApplicationScoped
public class FordelBeregningsgrunnlagHåndterer {


    private FordelRefusjonTjeneste fordelRefusjonTjeneste;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;


    FordelBeregningsgrunnlagHåndterer() {
        // for CDI proxy
    }

    @Inject
    public FordelBeregningsgrunnlagHåndterer(BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                              FordelRefusjonTjeneste fordelRefusjonTjeneste) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.fordelRefusjonTjeneste = fordelRefusjonTjeneste;
    }

    public void håndter(FordelBeregningsgrunnlagDto dto, Long behandlingId) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingId);
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = beregningsgrunnlag.dypKopi();

        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();

        for (FastsettBeregningsgrunnlagPeriodeDto endretPeriode : dto.getEndretBeregningsgrunnlagPerioder()) {
            fastsettVerdierForPeriode(behandlingId, perioder, endretPeriode);
        }

        beregningsgrunnlagRepository.lagre(behandlingId, nyttBeregningsgrunnlag, BeregningsgrunnlagTilstand.FASTSATT_INN);
    }


    private void fastsettVerdierForPeriode(Long behandlingId,
                                           List<BeregningsgrunnlagPeriode> perioder, FastsettBeregningsgrunnlagPeriodeDto endretPeriode) {
        BeregningsgrunnlagPeriode korrektPeriode = getKorrektPeriode(behandlingId, perioder, endretPeriode);
        Map<FastsettBeregningsgrunnlagAndelDto, BigDecimal> refusjonMap = fordelRefusjonTjeneste.getRefusjonPrÅrMap(behandlingId, endretPeriode, korrektPeriode);
        BeregningsgrunnlagPeriode uendretPeriode = Kopimaskin.deepCopy(korrektPeriode);
        BeregningsgrunnlagPeriode.Builder perioderBuilder = BeregningsgrunnlagPeriode.builder(korrektPeriode)
            .fjernAlleBeregningsgrunnlagPrStatusOgAndeler();
        // Må sortere med eksisterende først for å sette andelsnr på disse først
        List<FastsettBeregningsgrunnlagAndelDto> sorted = sorterMedNyesteSist(endretPeriode);
        for (FastsettBeregningsgrunnlagAndelDto endretAndel : sorted) {
            perioderBuilder.leggTilBeregningsgrunnlagPrStatusOgAndel(fastsettVerdierForAndel(uendretPeriode, refusjonMap, endretAndel));
        }
    }

    private BeregningsgrunnlagPrStatusOgAndel.Builder fastsettVerdierForAndel(BeregningsgrunnlagPeriode korrektPeriode,
                                                                              Map<FastsettBeregningsgrunnlagAndelDto, BigDecimal> refusjonMap,
                                                                              FastsettBeregningsgrunnlagAndelDto endretAndel) {
        FastsatteVerdierDto fastsatteVerdier = endretAndel.getFastsatteVerdier();
        FastsatteVerdierDto verdierMedJustertRefusjon = lagVerdierMedFordeltRefusjon(refusjonMap, endretAndel, fastsatteVerdier);
        return byggAndel(korrektPeriode, endretAndel, verdierMedJustertRefusjon);
    }

    private BeregningsgrunnlagPrStatusOgAndel.Builder byggAndel(BeregningsgrunnlagPeriode korrektPeriode, FastsettBeregningsgrunnlagAndelDto endretAndel, FastsatteVerdierDto verdierMedJustertRefusjon) {
        BeregningsgrunnlagPrStatusOgAndel.Builder andelBuilder = lagBuilderMedInntektOgInntektskategori(endretAndel);
        if (gjelderArbeidsforhold(endretAndel)) {
            byggArbeidsforhold(korrektPeriode, endretAndel, verdierMedJustertRefusjon, andelBuilder);
        }
        Optional<BeregningsgrunnlagPrStatusOgAndel> korrektAndel = finnAndelMedMatchendeAndelsnr(korrektPeriode, endretAndel);
        if (korrektAndel.isPresent()) {
            if (gjelderAAPEllerDagpenger(endretAndel)) {
                mapFelterForYtelse(korrektAndel.get(), andelBuilder);
            }
            if (!endretAndel.getNyAndel()) {
                mapBeregnetOgOverstyrt(korrektAndel.get(), andelBuilder);
            }
        }
        return andelBuilder;
    }

    private void mapBeregnetOgOverstyrt(BeregningsgrunnlagPrStatusOgAndel korrektAndel, BeregningsgrunnlagPrStatusOgAndel.Builder andelBuilder) {
        andelBuilder.medBeregnetPrÅr(korrektAndel.getBeregnetPrÅr());
        andelBuilder.medOverstyrtPrÅr(korrektAndel.getOverstyrtPrÅr());
    }

    private Optional<BeregningsgrunnlagPrStatusOgAndel> finnAndelMedMatchendeAndelsnr(BeregningsgrunnlagPeriode korrektPeriode, FastsettBeregningsgrunnlagAndelDto endretAndel) {
        return korrektPeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(a -> a.getAndelsnr().equals(endretAndel.getAndelsnr())).findFirst();
    }

    private void mapFelterForYtelse(BeregningsgrunnlagPrStatusOgAndel korrektAndel, BeregningsgrunnlagPrStatusOgAndel.Builder andelBuilder) {
        andelBuilder.medOrginalDagsatsFraTilstøtendeYtelse(korrektAndel.getOrginalDagsatsFraTilstøtendeYtelse());
        andelBuilder.medÅrsbeløpFraTilstøtendeYtelse(korrektAndel.getÅrsbeløpFraTilstøtendeYtelseVerdi());
    }

    private boolean gjelderAAPEllerDagpenger(FastsettBeregningsgrunnlagAndelDto endretAndel) {
        return endretAndel.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSAVKLARINGSPENGER) || endretAndel.getAktivitetStatus().equals(AktivitetStatus.DAGPENGER);
    }

    private boolean gjelderArbeidsforhold(FastsettBeregningsgrunnlagAndelDto endretAndel) {
        return endretAndel.getArbeidsgiverId() != null;
    }

    private BeregningsgrunnlagPrStatusOgAndel.Builder lagBuilderMedInntektOgInntektskategori(FastsettBeregningsgrunnlagAndelDto endretAndel) {
        return BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(endretAndel.getNyAndel() ? null : endretAndel.getAndelsnr()) // Opprettholder andelsnr som andel ble lagret med forrige gang
            .medAktivitetStatus(endretAndel.getAktivitetStatus())
            .medInntektskategori(endretAndel.getFastsatteVerdier().getInntektskategori())
            .medFordeltPrÅr(endretAndel.getFastsatteVerdier().finnEllerUtregnFastsattBeløpPrÅr())
            .medLagtTilAvSaksbehandler(endretAndel.getLagtTilAvSaksbehandler())
            .medArbforholdType(endretAndel.getArbeidsforholdType())
            .medBeregningsperiode(endretAndel.getBeregningsperiodeFom(), endretAndel.getBeregningsperiodeTom())
            .medFastsattAvSaksbehandler(true);
    }

    private void byggArbeidsforhold(BeregningsgrunnlagPeriode korrektPeriode, FastsettBeregningsgrunnlagAndelDto endretAndel, FastsatteVerdierDto verdierMedJustertRefusjon, BeregningsgrunnlagPrStatusOgAndel.Builder andelBuilder) {
        Arbeidsgiver arbeidsgiver = finnArbeidsgiver(endretAndel);
        Optional<BGAndelArbeidsforhold> arbeidsforholdOpt = finnRiktigArbeidsforholdFraGrunnlag(korrektPeriode, endretAndel);
        if (arbeidsforholdOpt.isPresent()) {
            var arbeidsforhold = arbeidsforholdOpt.get();
            BGAndelArbeidsforhold.Builder abeidsforholdBuilder = BGAndelArbeidsforhold.builder().medArbeidsgiver(arbeidsgiver)
                .medArbeidsforholdRef(endretAndel.getArbeidsforholdId())
                .medRefusjonskravPrÅr(verdierMedJustertRefusjon.getRefusjonPrÅr() != null ? BigDecimal.valueOf(verdierMedJustertRefusjon.getRefusjonPrÅr()) : BigDecimal.ZERO)
                .medArbeidsperiodeFom(arbeidsforhold.getArbeidsperiodeFom())
                .medArbeidsperiodeTom(arbeidsforhold.getArbeidsperiodeTom().orElse(null));
            if (!endretAndel.getLagtTilAvSaksbehandler()) {
                mapNaturalytelse(arbeidsforhold, abeidsforholdBuilder);
            }
            andelBuilder.medBGAndelArbeidsforhold(abeidsforholdBuilder);
        }
    }

    private void mapNaturalytelse(BGAndelArbeidsforhold arbeidsforhold, BGAndelArbeidsforhold.Builder abeidsforholdBuilder) {
        abeidsforholdBuilder
            .medNaturalytelseTilkommetPrÅr(arbeidsforhold.getNaturalytelseTilkommetPrÅr().orElse(null))
            .medNaturalytelseBortfaltPrÅr(arbeidsforhold.getNaturalytelseBortfaltPrÅr().orElse(null));
    }

    private Optional<BGAndelArbeidsforhold> finnRiktigArbeidsforholdFraGrunnlag(BeregningsgrunnlagPeriode korrektPeriode, FastsettBeregningsgrunnlagAndelDto endretAndel) {
        return korrektPeriode
            .getBeregningsgrunnlagPrStatusOgAndelList()
            .stream()
            .map(BeregningsgrunnlagPrStatusOgAndel::getBgAndelArbeidsforhold)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(a -> a.getArbeidsgiver().getIdentifikator().equals(endretAndel.getArbeidsgiverId()) && a.getArbeidsforholdRef().gjelderFor(endretAndel.getArbeidsforholdId()))
            .findFirst();
    }

    private Arbeidsgiver finnArbeidsgiver(FastsettBeregningsgrunnlagAndelDto endretAndel) {
        Arbeidsgiver arbeidsgiver;
        if (OrgNummer.erGyldigOrgnr(endretAndel.getArbeidsgiverId())) {
            arbeidsgiver = Arbeidsgiver.virksomhet(endretAndel.getArbeidsgiverId());
        } else {
            arbeidsgiver = Arbeidsgiver.person(new AktørId(endretAndel.getArbeidsgiverId()));
        }
        return arbeidsgiver;
    }

    private FastsatteVerdierDto lagVerdierMedFordeltRefusjon(Map<FastsettBeregningsgrunnlagAndelDto, BigDecimal> refusjonMap,
                                                             FastsettBeregningsgrunnlagAndelDto endretAndel, FastsatteVerdierDto fastsatteVerdier) {
        FastsatteVerdierDto verdierMedJustertRefusjon = new FastsatteVerdierDto(
            fastsatteVerdier.finnEllerUtregnFastsattBeløpPrÅr().intValue(),
            fastsatteVerdier.getInntektskategori(),
            fastsatteVerdier.getSkalHaBesteberegning());
        verdierMedJustertRefusjon.setRefusjonPrÅr(refusjonMap.get(endretAndel) != null ? refusjonMap.get(endretAndel).intValue() : null);
        return verdierMedJustertRefusjon;
    }

    private BeregningsgrunnlagPeriode getKorrektPeriode(Long behandlingId, List<BeregningsgrunnlagPeriode> perioder,
                                                        FastsettBeregningsgrunnlagPeriodeDto endretPeriode) {
        return perioder.stream()
            .filter(periode -> periode.getBeregningsgrunnlagPeriodeFom().equals(endretPeriode.getFom()))
            .findFirst()
            .orElseThrow(() -> FordelBeregningsgrunnlagHåndtererFeil.FACTORY.finnerIkkePeriodeFeil(behandlingId).toException());
    }

    private List<FastsettBeregningsgrunnlagAndelDto> sorterMedNyesteSist(FastsettBeregningsgrunnlagPeriodeDto endretPeriode) {
        Comparator<FastsettBeregningsgrunnlagAndelDto> fastsettBeregningsgrunnlagAndelDtoComparator = (a1, a2) -> {
            if (a1.getNyAndel()) {
                return 1;
            }
            if (a2.getNyAndel()) {
                return -1;
            }
            return 0;
        };
        return endretPeriode.getAndeler().stream().sorted(fastsettBeregningsgrunnlagAndelDtoComparator).collect(Collectors.toList());
    }
    private interface FordelBeregningsgrunnlagHåndtererFeil extends DeklarerteFeil {


        FordelBeregningsgrunnlagHåndtererFeil FACTORY = FeilFactory.create(FordelBeregningsgrunnlagHåndtererFeil.class);

        @TekniskFeil(feilkode = "FP-401647", feilmelding = "Finner ikke periode for eksisterende grunnlag. Behandling  %s", logLevel = LogLevel.WARN)
        Feil finnerIkkePeriodeFeil(long behandlingId);
    }

}
