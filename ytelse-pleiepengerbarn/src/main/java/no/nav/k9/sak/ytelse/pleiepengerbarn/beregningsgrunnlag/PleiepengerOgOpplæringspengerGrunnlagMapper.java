package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;
import static no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.MapTilUtbetalingsgradPrAktivitet.finnUtbetalingsgraderOgAktivitetsgrader;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagYtelsespesifiktGrunnlagMapper;
import no.nav.folketrygdloven.kalkulus.beregning.v1.OpplæringspengerGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PleiepengerNærståendeGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PleiepengerSyktBarnGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.UtbetalingsgradPrAktivitetDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.fpsak.tidsserie.LocalDateSegment;
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

    /**
     * Mapper ytelsesspesifikk informasjon for beregning. For PSB, OLP og PILS er dette utbetalingsgrader og aktivitetsgrader, samt dato for nye regler fra uttak (aksjonspunkt 9291)
     *
     * @param ref            Behandlingsreferanse
     * @param vilkårsperiode Vilkårsperiode som det beregnes for
     * @param iayGrunnlag iay-grunnlag
     * @return
     */
    @Override
    public YtelsespesifiktGrunnlagDto lagYtelsespesifiktGrunnlag(BehandlingReferanse ref, DatoIntervallEntitet vilkårsperiode, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var uttaksplan = uttakRestKlient.hentUttaksplan(ref.getBehandlingUuid(), false);
        var arbeidIPeriode = finnArbeidIPeriodeFraSøknad(ref, vilkårsperiode);
        var yrkesaktiviteter = iayGrunnlag.getAktørArbeidFraRegister(ref.getAktørId()).map(AktørArbeid::hentAlleYrkesaktiviteter).orElse(Collections.emptyList());
        var utbetalingsgrader = finnUtbetalingsgraderOgAktivitetsgrader(vilkårsperiode, Optional.ofNullable(uttaksplan), arbeidIPeriode, yrkesaktiviteter);
        var datoForNyeRegler = uttakNyeReglerRepository.finnDatoForNyeRegler(ref.getBehandlingId());
        return mapTilYtelseSpesifikkType(ref, utbetalingsgrader, datoForNyeRegler);
    }

    private List<Arbeid> finnArbeidIPeriodeFraSøknad(BehandlingReferanse ref, DatoIntervallEntitet vilkårsperiode) {
        var perioderFraSøknadene = periodeFraSøknadForBrukerTjeneste.hentPerioderFraSøknad(ref);
        var kravDokumenter = søknadsfristTjeneste.vurderSøknadsfrist(ref).keySet();
        var arbeidstidInput = new ArbeidstidMappingInput(kravDokumenter,
            perioderFraSøknadene,
            new LocalDateTimeline<>(List.of(new LocalDateSegment<>(vilkårsperiode.toLocalDateInterval(), true))),
            null,
            null);
        return new MapArbeid().map(arbeidstidInput);
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


}
