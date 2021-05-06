package no.nav.k9.sak.ytelse.omsorgspenger.beregnytelse;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.aarskvantum.kontrakter.Aktivitet;
import no.nav.k9.aarskvantum.kontrakter.Utfall;
import no.nav.k9.aarskvantum.kontrakter.Uttaksperiode;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.revurdering.ytelse.RevurderingBehandlingsresultatutleder;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.domene.behandling.steg.foreslåresultat.ForeslåBehandlingsresultatTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;

@FagsakYtelseTypeRef("OMP")
@ApplicationScoped
public class OmsorgspengerForeslåBehandlingsresultatTjeneste extends ForeslåBehandlingsresultatTjeneste {

    private static final Logger log = LoggerFactory.getLogger(OmsorgspengerForeslåBehandlingsresultatTjeneste.class);

    private OmsorgspengerGrunnlagRepository grunnlagRepository;
    private ÅrskvantumTjeneste årskvantumTjeneste;

    private VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste;
    private BeregningsresultatRepository beregningsresultatRepository;
    private VurderSøknadsfristTjeneste<OppgittFraværPeriode> vurderSøknadsfristTjeneste;

    OmsorgspengerForeslåBehandlingsresultatTjeneste() {
        // for proxy
    }

    @Inject
    public OmsorgspengerForeslåBehandlingsresultatTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                                           VedtakVarselRepository vedtakVarselRepository,
                                                           OmsorgspengerGrunnlagRepository grunnlagRepository,
                                                           ÅrskvantumTjeneste årskvantumTjeneste,
                                                           @FagsakYtelseTypeRef("OMP") VurderSøknadsfristTjeneste<OppgittFraværPeriode> vurderSøknadsfristTjeneste,
                                                           @FagsakYtelseTypeRef RevurderingBehandlingsresultatutleder revurderingBehandlingsresultatutleder,
                                                           @FagsakYtelseTypeRef("OMP") VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste,
                                                           BeregningsresultatRepository beregningsresultatRepository) {
        super(repositoryProvider, vedtakVarselRepository, revurderingBehandlingsresultatutleder);
        this.grunnlagRepository = grunnlagRepository;
        this.årskvantumTjeneste = årskvantumTjeneste;
        this.vurderSøknadsfristTjeneste = vurderSøknadsfristTjeneste;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.beregningsresultatRepository = beregningsresultatRepository;
    }

    @Override
    protected DatoIntervallEntitet getMaksPeriode(Long behandlingId) {
        return grunnlagRepository.hentMaksPeriode(behandlingId).orElseThrow();
    }

    @Override
    protected boolean skalAvslåsBasertPåAndreForhold(BehandlingReferanse ref) {
        var kravDokumenter = vurderSøknadsfristTjeneste.hentPerioderTilVurdering(ref).keySet();
        var harSøknad = kravDokumenter.stream().anyMatch(dok -> KravDokumentType.SØKNAD == dok.getType());
        if (harSøknad) {
            return skalAvslåsBasertPåIngenTilkjentYtelseEtterBeregning(ref);
        }
        return skalAvslåsBasertPåAvslåtteUttaksperioder(ref);
    }

    private boolean skalAvslåsBasertPåIngenTilkjentYtelseEtterBeregning(BehandlingReferanse ref) {
        List<BeregningsresultatPeriode> beregningsresultatPerioder = beregningsresultatRepository.hentEndeligBeregningsresultat(ref.getBehandlingId())
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList());

        NavigableSet<DatoIntervallEntitet> aktuelleBeregningVilkårPerioder = vilkårsPerioderTilVurderingTjeneste.utled(ref.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        boolean ingenPerioderHarTilkjentYtelse = aktuelleBeregningVilkårPerioder.stream()
            .noneMatch(vilkårsperiode -> periodeHarTilkjentYtelse(beregningsresultatPerioder, vilkårsperiode));
        if (ingenPerioderHarTilkjentYtelse) {
            log.info("Avslår behandling {}. Ingen aktuell vilkårsperiode endte med tilkjent ytelse", ref.getBehandlingUuid());
        }
        return ingenPerioderHarTilkjentYtelse;
    }

    private boolean periodeHarTilkjentYtelse(List<BeregningsresultatPeriode> beregningsresultatPerioder, DatoIntervallEntitet periode) {
        return beregningsresultatPerioder.stream()
            .anyMatch(brPeriode -> brPeriode.getPeriode().overlapper(periode) && brPeriode.getDagsats() > 0);
    }

    private boolean skalAvslåsBasertPåAvslåtteUttaksperioder(BehandlingReferanse ref) {
        var årskvantumForbrukteDager = årskvantumTjeneste.hentÅrskvantumForBehandling(ref.getBehandlingUuid());
        var sisteUttaksplan = årskvantumForbrukteDager.getSisteUttaksplan();
        if (sisteUttaksplan == null) {
            log.info("Avslår behandling. Har ingen uttaksplan for behandlingUuid={}", ref.getBehandlingUuid());
            return true;
        }

        List<Uttaksperiode> uttaksperioder = sisteUttaksplan.getAktiviteter()
            .stream()
            .map(Aktivitet::getUttaksperioder)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        boolean avslå = uttaksperioder.stream().allMatch(it -> Utfall.AVSLÅTT.equals(it.getUtfall()));

        if (avslå) {
            log.info("Avslår behandling {} pga alle uttaksperioder avslått: {}", ref.getBehandlingUuid(), uttaksperioder);
        }
        return avslå;
    }
}
