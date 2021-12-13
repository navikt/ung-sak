package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.fagsak.SakInfotrygdMigrering;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.PreconditionBeregningAksjonspunktUtleder;
import no.nav.k9.sak.domene.iay.modell.AktørYtelse;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.YtelseAnvist;
import no.nav.k9.sak.domene.iay.modell.YtelseFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef("PSB")
public class PSBPreconditionBeregningAksjonspunktUtleder implements PreconditionBeregningAksjonspunktUtleder {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private FagsakRepository fagsakRepository;

    private boolean toggleMigrering;

    public PSBPreconditionBeregningAksjonspunktUtleder() {
    }

    @Inject
    public PSBPreconditionBeregningAksjonspunktUtleder(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                       @BehandlingTypeRef @FagsakYtelseTypeRef("PSB") VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste,
                                                       FagsakRepository fagsakRepository,
                                                       @KonfigVerdi(value = "PSB_INFOTRYGD_MIGRERING", required = false, defaultVerdi = "false") boolean toggleMigrering) {
        this.perioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.toggleMigrering = toggleMigrering;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        if (!toggleMigrering) {
            return Collections.emptyList();
        }
        NavigableSet<DatoIntervallEntitet> perioderTilVurdering = perioderTilVurderingTjeneste.utled(param.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        var eksisterendeInfotrygdMigreringer = fagsakRepository.hentSakInfotrygdMigreringer(param.getRef().getFagsakId());

        var eksisterendeMigreringTilVurdering = finnEksisterendeMigreringTilVurdering(perioderTilVurdering, eksisterendeInfotrygdMigreringer);

        List<YtelseAnvist> anvistePeriodeSomManglerSøknad = finnAnvistePerioderFraInfotrygdUtenSøknad(param, perioderTilVurdering, eksisterendeMigreringTilVurdering);

        if (!anvistePeriodeSomManglerSøknad.isEmpty()) {
            return List.of(AksjonspunktResultat.opprettForAksjonspunktMedFrist(
                AksjonspunktDefinisjon.AUTO_VENT_PÅ_SØKNAD_FOR_PERIODE,
                Venteårsak.AVV_DOK,
                LocalDateTime.MAX));
        }

        if (!eksisterendeMigreringTilVurdering.isEmpty()) {

            return List.of(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.OVERSTYR_BEREGNING_INPUT));
        }
        return Collections.emptyList();
    }

    private List<YtelseAnvist> finnAnvistePerioderFraInfotrygdUtenSøknad(AksjonspunktUtlederInput param, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, List<SakInfotrygdMigrering> eksisterendeMigreringTilVurdering) {
        var fullstendigePerioder = perioderTilVurderingTjeneste.utledFullstendigePerioder(param.getBehandlingId());
        var førsteStpTilVurdering = perioderTilVurdering.stream().map(DatoIntervallEntitet::getFomDato).min(Comparator.naturalOrder()).orElseThrow();
        var førsteMigrerteTilVurdering = eksisterendeMigreringTilVurdering.stream().map(SakInfotrygdMigrering::getSkjæringstidspunkt)
            .min(Comparator.naturalOrder());
        InntektArbeidYtelseGrunnlag iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(param.getBehandlingId());
        Optional<AktørYtelse> aktørYtelse = iayGrunnlag.getAktørYtelseFraRegister(param.getAktørId());
        YtelseFilter ytelseFilter = lagInfotrygdPSBFilter(aktørYtelse);
        var anvistePerioderUtenSøknad = ytelseFilter.getFiltrertYtelser().stream()
            .flatMap(y -> y.getYtelseAnvist().stream())
            .filter(ya -> harAnvisningEtterMigrertPeriode(førsteMigrerteTilVurdering, ya) || harAnvisningSammeÅrSomFørsteSøknad(førsteStpTilVurdering, ya))
            .filter(ya -> fullstendigePerioder.stream().noneMatch(p -> p.overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(ya.getAnvistFOM(), ya.getAnvistTOM()))))
            .collect(Collectors.toList());
        return anvistePerioderUtenSøknad;
    }

    private boolean harAnvisningSammeÅrSomFørsteSøknad(LocalDate førsteStpTilVurdering, YtelseAnvist ya) {
        return ya.getAnvistTOM().getYear() == førsteStpTilVurdering.getYear();
    }

    private Boolean harAnvisningEtterMigrertPeriode(Optional<LocalDate> førsteMigrerteTilVurdering, YtelseAnvist ya) {
        return førsteMigrerteTilVurdering.map(stp -> ya.getAnvistFOM().isAfter(stp)).orElse(false);
    }

    private List<SakInfotrygdMigrering> finnEksisterendeMigreringTilVurdering(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, List<SakInfotrygdMigrering> eksisterendeInfotrygdMigreringer) {
        var migreringTilVurdering = eksisterendeInfotrygdMigreringer.stream()
            .filter(sim -> perioderTilVurdering.stream().anyMatch(periode -> periode.inkluderer(sim.getSkjæringstidspunkt())))
            .collect(Collectors.toList());
        var antallPerioderMedOverlapp = perioderTilVurdering.stream().filter(periode -> migreringTilVurdering.stream().map(SakInfotrygdMigrering::getSkjæringstidspunkt)
            .anyMatch(periode::inkluderer)).count();
        if (migreringTilVurdering.size() > antallPerioderMedOverlapp) {
            throw new IllegalStateException("Forventer maksimalt en migrering til vurdering per periode");
        }
        return migreringTilVurdering;
    }

    private YtelseFilter lagInfotrygdPSBFilter(Optional<AktørYtelse> aktørYtelse) {
        return new YtelseFilter(aktørYtelse).filter(y ->
            y.getYtelseType().equals(FagsakYtelseType.PSB) &&
                y.getKilde().equals(Fagsystem.INFOTRYGD));
    }


}
