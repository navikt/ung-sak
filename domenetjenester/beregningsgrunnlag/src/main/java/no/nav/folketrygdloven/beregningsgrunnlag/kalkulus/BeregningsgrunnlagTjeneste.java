package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.BeregningsgrunnlagPrReferanse;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
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
        UUID bgReferanse = finnBeregningsgrunnlagsReferanseFor(referanse.getBehandlingId(), skjæringstidspunkt, false, BehandlingType.REVURDERING.equals(referanse.getBehandlingType()));
        grunnlagRepository.lagre(referanse.getBehandlingId(), new BeregningsgrunnlagPeriode(bgReferanse, skjæringstidspunkt));

        return finnTjeneste(referanse.getFagsakYtelseType()).startBeregning(referanse, ytelseGrunnlag, bgReferanse, skjæringstidspunkt);
    }

    @Override
    public KalkulusResultat fortsettBeregning(BehandlingReferanse ref, LocalDate skjæringstidspunkt, BehandlingStegType stegType) {
        var bgReferanse = finnBeregningsgrunnlagsReferanseFor(ref.getBehandlingId(), skjæringstidspunkt, true, false);

        return finnTjeneste(ref.getFagsakYtelseType()).fortsettBeregning(ref.getFagsakYtelseType(), bgReferanse, stegType);
    }

    @Override
    public OppdaterBeregningsgrunnlagResultat oppdaterBeregning(HåndterBeregningDto håndterBeregningDto, BehandlingReferanse ref, LocalDate skjæringstidspunkt) {
        var bgReferanse = finnBeregningsgrunnlagsReferanseFor(ref.getBehandlingId(), skjæringstidspunkt, true, false);
        OppdaterBeregningsgrunnlagResultat oppdaterBeregningsgrunnlagResultat = finnTjeneste(ref.getFagsakYtelseType()).oppdaterBeregning(håndterBeregningDto, bgReferanse);
        oppdaterBeregningsgrunnlagResultat.setSkjæringstidspunkt(skjæringstidspunkt);
        return oppdaterBeregningsgrunnlagResultat;
    }

    @Override
    public List<OppdaterBeregningsgrunnlagResultat> oppdaterBeregningListe(Map<LocalDate, HåndterBeregningDto> håndterMap, BehandlingReferanse ref) {
        Map<UUID, LocalDate> referanseTilStpMap = håndterMap.keySet().stream()
            .collect(Collectors.toMap(v -> finnBeregningsgrunnlagsReferanseFor(ref.getBehandlingId(), v, true, false), v -> v));
        Map<LocalDate, UUID> stpTilReferanseMap = referanseTilStpMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        Map<UUID, HåndterBeregningDto> referanseTilDtoMap = håndterMap.entrySet().stream().collect(Collectors.toMap(e -> stpTilReferanseMap.get(e.getKey()), Map.Entry::getValue));
        List<OppdaterBeregningsgrunnlagResultat> resultatListe = finnTjeneste(ref.getFagsakYtelseType()).oppdaterBeregningListe(ref, referanseTilDtoMap);
        resultatListe.forEach(e -> e.setSkjæringstidspunkt(referanseTilStpMap.get(e.getReferanse())));
        return resultatListe;
    }


    @Override
    public Optional<Beregningsgrunnlag> hentEksaktFastsatt(BehandlingReferanse ref, LocalDate skjæringstidspunkt) {
        var bgReferanse = finnBeregningsgrunnlagsReferanseFor(ref.getBehandlingId(), skjæringstidspunkt, true, false);

        return finnTjeneste(ref.getFagsakYtelseType()).hentEksaktFastsatt(ref.getFagsakYtelseType(), bgReferanse);
    }

    @Override
    public List<Beregningsgrunnlag> hentEksaktFastsattForAllePerioderInkludertAvslag(BehandlingReferanse ref) {
        var vilkårene = vilkårResultatRepository.hent(ref.getBehandlingId());
        var vilkår = vilkårene.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow();

        return vilkår.getPerioder()
            .stream()
            .filter(it -> Utfall.OPPFYLT.equals(it.getUtfall()) || Utfall.IKKE_OPPFYLT.equals(it.getUtfall()))
            .map(VilkårPeriode::getSkjæringstidspunkt)
            .map(it -> hentEksaktFastsatt(ref, it)) // TODO:
            .flatMap(Optional::stream)
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(Beregningsgrunnlag::getSkjæringstidspunkt))
            .collect(Collectors.toList());
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
            .flatMap(Optional::stream)
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(Beregningsgrunnlag::getSkjæringstidspunkt))
            .collect(Collectors.toList());
    }

    @Override
    public List<BeregningsgrunnlagDto> hentBeregningsgrunnlagDtoer(BehandlingReferanse ref) {
        var beregningsgrunnlagPerioderGrunnlag = grunnlagRepository.hentGrunnlag(ref.getBehandlingId());
        if (beregningsgrunnlagPerioderGrunnlag.isPresent()) {
            var tjeneste = finnTjeneste(ref.getFagsakYtelseType());
            var vilkårene = vilkårResultatRepository.hent(ref.getBehandlingId());
            var vilkår = vilkårene.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow();
            var grunnlag = beregningsgrunnlagPerioderGrunnlag.get();

            var bgReferanser = vilkår.getPerioder()
                .stream()
                .map(VilkårPeriode::getSkjæringstidspunkt)
                .filter(it -> grunnlag.finnFor(it).isPresent())
                .map(it -> new BeregningsgrunnlagReferanse(grunnlag.finnFor(it).map(BeregningsgrunnlagPeriode::getEksternReferanse).orElseThrow(), it))
                .filter(it -> Objects.nonNull(it.getReferanse()))
                .collect(Collectors.toSet());

            return tjeneste.hentBeregningsgrunnlagListeDto(ref, bgReferanser).getBeregningsgrunnlagListe()
                .stream()
                .filter(bg -> bg.getBeregningsgrunnlag() != null)
                .map(BeregningsgrunnlagPrReferanse::getBeregningsgrunnlag)
                .collect(Collectors.toList());
        }
        return List.of();
    }

    @Override
    public Optional<Beregningsgrunnlag> hentFastsatt(BehandlingReferanse ref, LocalDate skjæringstidspunkt) {
        var bgReferanse = finnBeregningsgrunnlagsReferanseFor(ref.getBehandlingId(), skjæringstidspunkt, false);

        if (bgReferanse.isEmpty()) {
            return Optional.empty();
        }
        return finnTjeneste(ref.getFagsakYtelseType()).hentFastsatt(bgReferanse.get(), ref.getFagsakYtelseType());
    }

    @Override
    public Optional<BeregningsgrunnlagGrunnlag> hentGrunnlag(BehandlingReferanse ref, LocalDate skjæringstidspunkt) {
        var bgReferanse = finnBeregningsgrunnlagsReferanseFor(ref.getBehandlingId(), skjæringstidspunkt, false);

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
            .map(it -> new BeregningsgrunnlagKobling(it, finnBeregningsgrunnlagsReferanseFor(ref.getBehandlingId(), it, true, false)))
            .collect(Collectors.toList());
    }

    @Override
    public void deaktiverBeregningsgrunnlag(BehandlingReferanse ref, LocalDate skjæringstidspunkt) {
        var bgReferanse = finnBeregningsgrunnlagsReferanseFor(ref.getBehandlingId(), skjæringstidspunkt, false);
        bgReferanse.ifPresent(bgRef -> finnTjeneste(ref.getFagsakYtelseType()).deaktiverBeregningsgrunnlag(ref.getFagsakYtelseType(), bgRef));
    }

    @Override
    public void gjenopprettInitiell(BehandlingReferanse ref) {
        grunnlagRepository.gjenopprettInitiell(ref.getBehandlingId());
    }

    @Override
    public Boolean erEndringIBeregning(Long behandlingId1, Long behandlingId2, LocalDate skjæringstidspunkt) {
        var behandling1 = behandlingRepository.hentBehandling(behandlingId1);
        var behandling2 = behandlingRepository.hentBehandling(behandlingId2);
        var bgReferanse1 = finnBeregningsgrunnlagsReferanseFor(behandlingId1, skjæringstidspunkt, false);
        var bgReferanse2 = finnBeregningsgrunnlagsReferanseFor(behandlingId2, skjæringstidspunkt, false);

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

    private UUID finnBeregningsgrunnlagsReferanseFor(Long behandlingId, LocalDate skjæringstidspunkt, boolean kreverEksisterendeReferanse, boolean skalLageNyVedLikSomInitiell) {
        var bgReferanse = finnBeregningsgrunnlagsReferanseFor(behandlingId, skjæringstidspunkt, skalLageNyVedLikSomInitiell);

        if (bgReferanse.isEmpty() && kreverEksisterendeReferanse) {
            throw new IllegalStateException("Forventer at referansen eksisterer for skjæringstidspunkt=" + skjæringstidspunkt);
        }
        return bgReferanse.orElse(UUID.randomUUID());
    }

    private Optional<UUID> finnBeregningsgrunnlagsReferanseFor(Long behandlingId, LocalDate skjæringstidspunkt, boolean skalLageNyVedLikSomInitiell) {
        var grunnlagOptional = grunnlagRepository.hentGrunnlag(behandlingId);
        if (grunnlagOptional.isPresent()) {
            var grunnlag = grunnlagOptional.get();

            var beregningsgrunnlagPeriodeOpt = grunnlag.finnFor(skjæringstidspunkt);
            var grunnlagReferanse = beregningsgrunnlagPeriodeOpt.map(BeregningsgrunnlagPeriode::getEksternReferanse);
            if (grunnlagReferanse.isPresent() && skalLageNyVedLikSomInitiell) {
                var initilVersjon = grunnlagRepository.getInitiellVersjon(behandlingId);
                if (initilVersjon.isPresent()) {
                    var initReferanse = initilVersjon.get().finnFor(skjæringstidspunkt).map(BeregningsgrunnlagPeriode::getEksternReferanse);
                    if (initReferanse.isPresent() && grunnlagReferanse.get().equals(initReferanse.get())) {
                        grunnlagReferanse = Optional.empty();
                    }
                }
            }

            return grunnlagReferanse;
        }
        return Optional.empty();
    }
}
