package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagKobling;
import no.nav.folketrygdloven.beregningsgrunnlag.output.KalkulusResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.output.OppdaterBeregningsgrunnlagResultat;
import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPeriode;

@Dependent
public class BeregningsgrunnlagTjeneste implements BeregningTjeneste {

    private Instance<KalkulusApiTjeneste> kalkulusTjenester;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private BeregningPerioderGrunnlagRepository grunnlagRepository;

    @Inject
    public BeregningsgrunnlagTjeneste(@Any Instance<KalkulusApiTjeneste> kalkulusTjenester,
                                      BehandlingRepository behandlingRepository,
                                      VilkårResultatRepository vilkårResultatRepository,
                                      BeregningPerioderGrunnlagRepository grunnlagRepository) {
        this.kalkulusTjenester = kalkulusTjenester;
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.grunnlagRepository = grunnlagRepository;
    }

    @Override
    public KalkulusResultat startBeregning(BehandlingReferanse referanse, YtelsespesifiktGrunnlagDto ytelseGrunnlag, LocalDate skjæringstidspunkt) {
        UUID bgReferanse = finnBeregningsgrunnlagsReferanseFor(referanse.getBehandlingId(), skjæringstidspunkt, false);
        grunnlagRepository.lagre(referanse.getBehandlingId(), new BeregningsgrunnlagPeriode(bgReferanse, skjæringstidspunkt));

        return finnTjeneste(referanse.getFagsakYtelseType()).startBeregning(referanse, ytelseGrunnlag, bgReferanse, skjæringstidspunkt);
    }

    @Override
    public KalkulusResultat fortsettBeregning(BehandlingReferanse ref, LocalDate skjæringstidspunkt, BehandlingStegType stegType) {
        var bgReferanse = finnBeregningsgrunnlagsReferanseFor(ref.getBehandlingId(), skjæringstidspunkt, true);

        return finnTjeneste(ref.getFagsakYtelseType()).fortsettBeregning(ref.getFagsakYtelseType(), bgReferanse, stegType);
    }

    @Override
    public OppdaterBeregningsgrunnlagResultat oppdaterBeregning(HåndterBeregningDto håndterBeregningDto, BehandlingReferanse ref, LocalDate skjæringstidspunkt) {
        var bgReferanse = finnBeregningsgrunnlagsReferanseFor(ref.getBehandlingId(), skjæringstidspunkt, true);

        return finnTjeneste(ref.getFagsakYtelseType()).oppdaterBeregning(håndterBeregningDto, bgReferanse);
    }

    @Override
    public Beregningsgrunnlag hentEksaktFastsatt(BehandlingReferanse ref, LocalDate skjæringstidspunkt) {
        var bgReferanse = finnBeregningsgrunnlagsReferanseFor(ref.getBehandlingId(), skjæringstidspunkt, true);

        return finnTjeneste(ref.getFagsakYtelseType()).hentEksaktFastsatt(ref.getFagsakYtelseType(), bgReferanse);
    }

    @Override
    public Optional<Beregningsgrunnlag> hentEksaktFastsattForFørstePeriode(BehandlingReferanse ref) {
        var vilkårene = vilkårResultatRepository.hent(ref.getBehandlingId());
        var vilkår = vilkårene.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow();

        return vilkår.getPerioder()
            .stream()
            .filter(it -> Utfall.OPPFYLT.equals(it.getUtfall()))
            .map(VilkårPeriode::getSkjæringstidspunkt)
            .min(LocalDate::compareTo)
            .map(it -> hentEksaktFastsatt(ref, it));
    }

    @Override
    public List<Beregningsgrunnlag> hentEksaktFastsattForAllePerioder(BehandlingReferanse ref) {
        var vilkårene = vilkårResultatRepository.hent(ref.getBehandlingId());
        var vilkår = vilkårene.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow();

        return vilkår.getPerioder()
            .stream()
            .filter(it -> Utfall.OPPFYLT.equals(it.getUtfall()))
            .map(VilkårPeriode::getSkjæringstidspunkt)
            .map(it -> hentEksaktFastsatt(ref, it)) // TODO:
            .filter(v -> v != null)
            .sorted(Comparator.comparing(Beregningsgrunnlag::getSkjæringstidspunkt))
            .collect(Collectors.toList());
    }

    @Override
    public BeregningsgrunnlagDto hentBeregningsgrunnlagDto(BehandlingReferanse ref, LocalDate skjæringstidspunkt) {
        var bgReferanse = finnBeregningsgrunnlagsReferanseFor(ref.getBehandlingId(), skjæringstidspunkt, true);

        return finnTjeneste(ref.getFagsakYtelseType()).hentBeregningsgrunnlagDto(ref, bgReferanse);
    }

    @Override
    public List<BeregningsgrunnlagDto> hentBeregningsgrunnlagDtoer(BehandlingReferanse ref) {
        var beregningsgrunnlagPerioderGrunnlag = grunnlagRepository.hentGrunnlag(ref.getBehandlingId());
        if (beregningsgrunnlagPerioderGrunnlag.isPresent()) {
            var tjeneste = finnTjeneste(ref.getFagsakYtelseType());
            return beregningsgrunnlagPerioderGrunnlag.get()
                .getGrunnlagPerioder()
                .stream()
                .map(it -> tjeneste.hentBeregningsgrunnlagDto(ref, it.getEksternReferanse()))
                .filter(v -> v != null)
                .collect(Collectors.toList());
        }
        return List.of();
    }

    @Override
    public Optional<Beregningsgrunnlag> hentFastsatt(BehandlingReferanse ref, LocalDate skjæringstidspunkt) {
        var bgReferanse = finnBeregningsgrunnlagsReferanseFor(ref.getBehandlingId(), skjæringstidspunkt);

        if (bgReferanse.isEmpty()) {
            return Optional.empty();
        }
        return finnTjeneste(ref.getFagsakYtelseType()).hentFastsatt(bgReferanse.get(), ref.getFagsakYtelseType());
    }

    @Override
    public Optional<BeregningsgrunnlagGrunnlag> hentGrunnlag(BehandlingReferanse ref, LocalDate skjæringstidspunkt) {
        var bgReferanse = finnBeregningsgrunnlagsReferanseFor(ref.getBehandlingId(), skjæringstidspunkt);

        if (bgReferanse.isEmpty()) {
            return Optional.empty();
        }

        return finnTjeneste(ref.getFagsakYtelseType()).hentGrunnlag(ref.getFagsakYtelseType(), bgReferanse.get());
    }

    @Override
    public List<BeregningsgrunnlagKobling> hentKoblinger(BehandlingReferanse ref) {
        var vilkårene = vilkårResultatRepository.hent(ref.getBehandlingId());
        var vilkår = vilkårene.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow();

        return vilkår.getPerioder()
            .stream()
            .map(VilkårPeriode::getSkjæringstidspunkt)
            .map(it -> new BeregningsgrunnlagKobling(it, finnBeregningsgrunnlagsReferanseFor(ref.getBehandlingId(), it, true)))
            .collect(Collectors.toList());
    }

    @Override
    public Optional<Beregningsgrunnlag> hentBeregningsgrunnlagForId(BehandlingReferanse ref, LocalDate skjæringstidspunkt, UUID bgGrunnlagsVersjon) {
        var bgReferanse = finnBeregningsgrunnlagsReferanseFor(ref.getBehandlingId(), skjæringstidspunkt);

        if (bgReferanse.isEmpty()) {
            return Optional.empty();
        }
        return finnTjeneste(ref.getFagsakYtelseType()).hentBeregningsgrunnlagForId(bgReferanse.get(), ref.getFagsakYtelseType(), bgGrunnlagsVersjon);
    }

    @Override
    public void deaktiverBeregningsgrunnlag(BehandlingReferanse ref, LocalDate skjæringstidspunkt) {
        var bgReferanse = finnBeregningsgrunnlagsReferanseFor(ref.getBehandlingId(), skjæringstidspunkt, true);

        finnTjeneste(ref.getFagsakYtelseType()).deaktiverBeregningsgrunnlag(ref.getFagsakYtelseType(), bgReferanse);
    }

    @Override
    public Boolean erEndringIBeregning(Long behandlingId1, Long behandlingId2, LocalDate skjæringstidspunkt) {
        var behandling1 = behandlingRepository.hentBehandling(behandlingId1);
        var behandling2 = behandlingRepository.hentBehandling(behandlingId2);
        var bgReferanse1 = finnBeregningsgrunnlagsReferanseFor(behandlingId1, skjæringstidspunkt);
        var bgReferanse2 = finnBeregningsgrunnlagsReferanseFor(behandlingId2, skjæringstidspunkt);

        if (bgReferanse1.isEmpty() || bgReferanse2.isEmpty()) {
            return false;
        }

        return finnTjeneste(behandling1.getFagsakYtelseType()).erEndringIBeregning(behandling1.getFagsakYtelseType(), bgReferanse1.orElseThrow(), behandling2.getFagsakYtelseType(),
            bgReferanse2.orElseThrow());
    }

    private KalkulusApiTjeneste finnTjeneste(FagsakYtelseType fagsakYtelseType) {
        return FagsakYtelseTypeRef.Lookup.find(kalkulusTjenester, fagsakYtelseType)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke kalkulustjeneste for " + fagsakYtelseType));
    }

    private UUID finnBeregningsgrunnlagsReferanseFor(Long behandlingId, LocalDate skjæringstidspunkt, boolean kreverEksisterendeReferanse) {
        var bgReferanse = finnBeregningsgrunnlagsReferanseFor(behandlingId, skjæringstidspunkt);

        if (bgReferanse.isEmpty() && kreverEksisterendeReferanse) {
            throw new IllegalStateException("Forventer at referansen eksisterer for skjæringstidspunkt=" + skjæringstidspunkt);
        }
        return bgReferanse.orElse(UUID.randomUUID());
    }

    private Optional<UUID> finnBeregningsgrunnlagsReferanseFor(Long behandlingId, LocalDate skjæringstidspunkt) {
        var grunnlagOptional = grunnlagRepository.hentGrunnlag(behandlingId);

        if (grunnlagOptional.isPresent()) {
            var grunnlag = grunnlagOptional.get();

            var beregningsgrunnlagPeriodeOpt = grunnlag.finnFor(skjæringstidspunkt);

            return beregningsgrunnlagPeriodeOpt.map(BeregningsgrunnlagPeriode::getEksternReferanse).filter(v -> v != null);
        }
        return Optional.empty();
    }
}
