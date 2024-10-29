package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagYtelsespesifiktGrunnlagMapper;
import no.nav.folketrygdloven.kalkulus.beregning.v1.AktivitetDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.OpplæringspengerGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PeriodeMedUtbetalingsgradDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PleiepengerNærståendeGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PleiepengerSyktBarnGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.UtbetalingsgradPrAktivitetDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Aktivitetsgrad;
import no.nav.folketrygdloven.kalkulus.felles.v1.Aktør;
import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Organisasjon;
import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;
import no.nav.folketrygdloven.kalkulus.felles.v1.Utbetalingsgrad;
import no.nav.folketrygdloven.kalkulus.kodeverk.UttakArbeidType;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.uttak.UttakNyeReglerRepository;
import no.nav.k9.sak.domene.iay.modell.AktørArbeid;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.PSBVurdererSøknadsfristTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.PeriodeFraSøknadForBrukerTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.ArbeidstidMappingInput;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.MapArbeid;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeid;

@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@ApplicationScoped
public class PleiepengerOgOpplæringspengerGrunnlagMapper implements BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<YtelsespesifiktGrunnlagDto> {

    private UttakTjeneste uttakRestKlient;
    private UttakNyeReglerRepository uttakNyeReglerRepository;

    private PeriodeFraSøknadForBrukerTjeneste periodeFraSøknadForBrukerTjeneste;

    private PSBVurdererSøknadsfristTjeneste søknadsfristTjeneste;

    public PleiepengerOgOpplæringspengerGrunnlagMapper() {
        // for proxy
    }

    @Inject
    public PleiepengerOgOpplæringspengerGrunnlagMapper(UttakTjeneste uttakRestKlient,
                                                       UttakNyeReglerRepository uttakNyeReglerRepository,
                                                       PeriodeFraSøknadForBrukerTjeneste periodeFraSøknadForBrukerTjeneste,
                                                       @Any PSBVurdererSøknadsfristTjeneste søknadsfristTjeneste) {
        this.uttakRestKlient = uttakRestKlient;
        this.uttakNyeReglerRepository = uttakNyeReglerRepository;
        this.periodeFraSøknadForBrukerTjeneste = periodeFraSøknadForBrukerTjeneste;
        this.søknadsfristTjeneste = søknadsfristTjeneste;
    }

    @Override
    public YtelsespesifiktGrunnlagDto lagYtelsespesifiktGrunnlag(BehandlingReferanse ref, DatoIntervallEntitet vilkårsperiode, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var uttaksplan = uttakRestKlient.hentUttaksplan(ref.getBehandlingUuid(), false);


        List<UtbetalingsgradPrAktivitetDto> utbetalingsgrader = new ArrayList<>();
        var arbeidIPeriode = finnArbeidIPeriode(ref, vilkårsperiode);
        var yrkesaktiviteter = iayGrunnlag.getAktørArbeidFraRegister(ref.getAktørId()).map(AktørArbeid::hentAlleYrkesaktiviteter).orElse(Collections.emptyList());

        if (uttaksplan != null) {
            utbetalingsgrader = MapTilUtbetalingsgradPrAktivitet.finnUtbetalingsgraderOgAktivitetsgrader(vilkårsperiode, uttaksplan, arbeidIPeriode, yrkesaktiviteter);
        } else {
            utbetalingsgrader = finnUtbetalingsgraderFraSøknadsdata(arbeidIPeriode);
        }

        var datoForNyeRegler = uttakNyeReglerRepository.finnDatoForNyeRegler(ref.getBehandlingId());


        return mapTilYtelseSpesifikkType(ref, utbetalingsgrader, datoForNyeRegler);
    }

    private List<Arbeid> finnArbeidIPeriode(BehandlingReferanse ref, DatoIntervallEntitet vilkårsperiode) {
        var perioderFraSøknadene = periodeFraSøknadForBrukerTjeneste.hentPerioderFraSøknad(ref);
        var kravDokumenter = søknadsfristTjeneste.vurderSøknadsfrist(ref).keySet();
        var arbeidstidInput = new ArbeidstidMappingInput(kravDokumenter,
            perioderFraSøknadene,
            new LocalDateTimeline<>(vilkårsperiode.toLocalDateInterval(), true),
            null,
            null);
        return new MapArbeid().map(arbeidstidInput);
    }

    private List<UtbetalingsgradPrAktivitetDto> finnUtbetalingsgraderFraSøknadsdata(List<Arbeid> arbeidIPeriode) {
        List<UtbetalingsgradPrAktivitetDto> utbetalingsgrader;

        utbetalingsgrader = arbeidIPeriode.stream()
            .filter(a -> !a.getPerioder().isEmpty())
            .map(a -> {
                var perioder = a.getPerioder().entrySet().stream().map(p -> new PeriodeMedUtbetalingsgradDto(
                    new Periode(p.getKey().getFom(), p.getKey().getTom()),
                    Utbetalingsgrad.fra(100),
                    Aktivitetsgrad.fra(FinnAktivitetsgrad.finnAktivitetsgrad(p.getValue())))).toList();
                Aktør aktør = null;
                if (a.getArbeidsforhold().getOrganisasjonsnummer() != null) {
                    aktør = new Organisasjon(a.getArbeidsforhold().getOrganisasjonsnummer());
                } else if (a.getArbeidsforhold().getAktørId() != null) {
                    aktør = new AktørIdPersonident(a.getArbeidsforhold().getAktørId());
                }
                return new UtbetalingsgradPrAktivitetDto(new AktivitetDto(
                    aktør,
                    a.getArbeidsforhold().getArbeidsforholdId() == null ? null : new InternArbeidsforholdRefDto(a.getArbeidsforhold().getArbeidsforholdId()),
                    mapUttakArbeidType(no.nav.k9.kodeverk.uttak.UttakArbeidType.fraKode(a.getArbeidsforhold().getType()))), perioder);
            }).toList();
        return utbetalingsgrader;
    }


    private YtelsespesifiktGrunnlagDto mapTilYtelseSpesifikkType(BehandlingReferanse ref, List<UtbetalingsgradPrAktivitetDto> utbetalingsgrader, Optional<LocalDate> datoForNyeRegler) {
        return switch (ref.getFagsakYtelseType()) {
            case PLEIEPENGER_SYKT_BARN ->
                new PleiepengerSyktBarnGrunnlag(utbetalingsgrader, datoForNyeRegler.orElse(null));
            case PLEIEPENGER_NÆRSTÅENDE ->
                new PleiepengerNærståendeGrunnlag(utbetalingsgrader, datoForNyeRegler.orElse(null));
            case OPPLÆRINGSPENGER -> new OpplæringspengerGrunnlag(utbetalingsgrader);
            default ->
                throw new IllegalStateException("Ikke støttet ytelse for kalkulus Pleiepenger: " + ref.getFagsakYtelseType());
        };
    }

    // Inntil man lager en switch
    private static UttakArbeidType mapUttakArbeidType(no.nav.k9.kodeverk.uttak.UttakArbeidType uttakArbeidType) {
        return switch (uttakArbeidType) {
            case ARBEIDSTAKER -> UttakArbeidType.ORDINÆRT_ARBEID;
            case FRILANSER -> UttakArbeidType.FRILANS;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE;
            case DAGPENGER -> UttakArbeidType.DAGPENGER;
            case INAKTIV -> UttakArbeidType.MIDL_INAKTIV;
            case KUN_YTELSE -> UttakArbeidType.BRUKERS_ANDEL;
            case IKKE_YRKESAKTIV, IKKE_YRKESAKTIV_UTEN_ERSTATNING -> UttakArbeidType.IKKE_YRKESAKTIV;
            case PLEIEPENGER_AV_DAGPENGER -> UttakArbeidType.PLEIEPENGER_AV_DAGPENGER;
            case SYKEPENGER_AV_DAGPENGER -> UttakArbeidType.SYKEPENGER_AV_DAGPENGER;
            case ANNET -> UttakArbeidType.ANNET;
            case null -> null;
        };
    }


}
