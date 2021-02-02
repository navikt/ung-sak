package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.BgRef;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagKobling;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.SamletKalkulusResultat;
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
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPeriode;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPerioderGrunnlag;

@Dependent
public class BeregningsgrunnlagTjeneste implements BeregningTjeneste {

    private Instance<KalkulusApiTjeneste> kalkulusTjenester;
    private VilkårResultatRepository vilkårResultatRepository;
    private BeregningPerioderGrunnlagRepository grunnlagRepository;

    @Inject
    public BeregningsgrunnlagTjeneste(@Any Instance<KalkulusApiTjeneste> kalkulusTjenester,
                                      VilkårResultatRepository vilkårResultatRepository,
                                      BeregningPerioderGrunnlagRepository grunnlagRepository) {
        this.kalkulusTjenester = kalkulusTjenester;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.grunnlagRepository = grunnlagRepository;
    }

    @Override
    public SamletKalkulusResultat startBeregning(BehandlingReferanse referanse, List<DatoIntervallEntitet> vilkårsperioder) {
        if(vilkårsperioder == null || vilkårsperioder.isEmpty()){
            throw new IllegalArgumentException("Forventer minst en vilkårsperiode");
        }
        var skjæringstidspunkter = vilkårsperioder.stream()
            .map(DatoIntervallEntitet::getFomDato)
            .collect(Collectors.toCollection(TreeSet::new));

        var bgReferanser = finnReferanseEllerLagNy(referanse.getBehandlingId(), skjæringstidspunkter, false, BehandlingType.REVURDERING.equals(referanse.getBehandlingType()));

        if (bgReferanser.size() != skjæringstidspunkter.size()) {
            throw new IllegalStateException("Mismatch størrelse bgReferanser: " + bgReferanser + ", skjæringstidspunkter:" + skjæringstidspunkter);
        } else if (bgReferanser.isEmpty()){
            throw new IllegalArgumentException("Forventer minst en bgReferanse");
        }

        var beregningInput = bgReferanser.stream().map(e -> {
            var bgRef = e.getRef();
            var stp = e.getStp();
            var vilkårsperiode = vilkårsperioder.stream().filter(p -> p.getFomDato().equals(stp)).findFirst().orElseThrow();
            grunnlagRepository.lagre(referanse.getBehandlingId(), new BeregningsgrunnlagPeriode(bgRef, stp));
            return new StartBeregningInput(bgRef, vilkårsperiode);
        }).collect(Collectors.toList());

        return finnTjeneste(referanse.getFagsakYtelseType()).startBeregning(referanse, beregningInput);
    }

    @Override
    public SamletKalkulusResultat fortsettBeregning(BehandlingReferanse ref, Collection<LocalDate> skjæringstidspunkter, BehandlingStegType stegType) {
        if(skjæringstidspunkter == null || skjæringstidspunkter.isEmpty()){
            throw new IllegalArgumentException("Forventer minst ett ytelseGrunnlag");
        }
        var bgReferanser = finnReferanseEllerLagNy(ref.getBehandlingId(), skjæringstidspunkter, true, false);
        if (bgReferanser.size() != skjæringstidspunkter.size()) {
            throw new IllegalStateException("Mismatch størrelse bgReferanser: " + bgReferanser + ", skjæringstidspunkter:" + skjæringstidspunkter);
        }
        var tjeneste = finnTjeneste(ref.getFagsakYtelseType());
        return tjeneste.fortsettBeregning(ref, bgReferanser, stegType);
    }

    @Override
    public OppdaterBeregningsgrunnlagResultat oppdaterBeregning(HåndterBeregningDto dto, BehandlingReferanse ref, LocalDate skjæringstidspunkt) {
        Objects.requireNonNull(skjæringstidspunkt, "skjæringstidspunkt");
        var resultater = oppdaterBeregningListe(Map.of(skjæringstidspunkt, dto), ref);
        if (resultater.size() == 1) {
            var res = resultater.get(0);
            res.setSkjæringstidspunkt(skjæringstidspunkt);
            return res;
        } else {
            // skal ikke kunne skje
            throw new IllegalStateException("Forventet å få 1 resultat, fikk: " + resultater.size() + " for stp=" + skjæringstidspunkt);
        }
    }

    @Override
    public List<OppdaterBeregningsgrunnlagResultat> oppdaterBeregningListe(Map<LocalDate, HåndterBeregningDto> stpTilDtoMap, BehandlingReferanse ref) {
        if(stpTilDtoMap == null || stpTilDtoMap.isEmpty()){
            throw new IllegalArgumentException("Forventer minst ett ytelseGrunnlag");
        }
        var sortertMap = new TreeMap<>(stpTilDtoMap);

        var bgReferanser = finnReferanseEllerLagNy(ref.getBehandlingId(), sortertMap.keySet(), true, false);

        if (bgReferanser.size() != sortertMap.size()) {
            throw new IllegalStateException("Mismatch størrelse bgReferanser: " + bgReferanser + ", skjæringstidspunkter:" + sortertMap.keySet());
        }

        Map<UUID, LocalDate> referanseTilStpMap = bgReferanser.stream().collect(Collectors.toMap(BgRef::getRef, BgRef::getStp));

        Map<LocalDate, UUID> stpTilReferanseMap = referanseTilStpMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey, (e1, e2) -> e1, TreeMap::new));
        Map<UUID, HåndterBeregningDto> referanseTilDtoMap = sortertMap.entrySet().stream().collect(Collectors.toMap(e -> stpTilReferanseMap.get(e.getKey()), Map.Entry::getValue));
        List<OppdaterBeregningsgrunnlagResultat> resultatListe = finnTjeneste(ref.getFagsakYtelseType()).oppdaterBeregningListe(ref, bgReferanser, referanseTilDtoMap);

        resultatListe.forEach(e -> e.setSkjæringstidspunkt(referanseTilStpMap.get(e.getReferanse()))); // sett stp for hvert resultat resultater

        return resultatListe;
    }

    @Override
    public List<Beregningsgrunnlag> hentEksaktFastsatt(BehandlingReferanse ref, Collection<LocalDate> skjæringstidspunkter) {
        var bgReferanser = finnReferanseEllerLagNy(ref.getBehandlingId(), skjæringstidspunkter, true, false);

        return finnTjeneste(ref.getFagsakYtelseType()).hentEksaktFastsatt(ref, bgReferanser);
    }

    @Override
    public List<Beregningsgrunnlag> hentEksaktFastsattForAllePerioderInkludertAvslag(BehandlingReferanse ref) {
        var vilkårene = vilkårResultatRepository.hent(ref.getBehandlingId());
        var vilkår = vilkårene.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow();

        var perioder = vilkår.getPerioder()
            .stream()
            .filter(it -> Utfall.OPPFYLT.equals(it.getUtfall()) || Utfall.IKKE_OPPFYLT.equals(it.getUtfall()))
            .map(VilkårPeriode::getSkjæringstidspunkt)
            .sorted()
            .distinct()
            .collect(Collectors.toList());

        return hentEksaktFastsatt(ref, perioder);
    }

    @Override
    public List<Beregningsgrunnlag> hentEksaktFastsattForAllePerioder(BehandlingReferanse ref) {
        var vilkårene = vilkårResultatRepository.hent(ref.getBehandlingId());
        var vilkår = vilkårene.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow();

        List<LocalDate> skjæringstidspunkt = vilkår.getPerioder()
            .stream()
            .filter(it -> Utfall.OPPFYLT.equals(it.getUtfall()))
            .map(VilkårPeriode::getSkjæringstidspunkt)
            .collect(Collectors.toList());

        List<Beregningsgrunnlag> fastsatt = hentEksaktFastsatt(ref, skjæringstidspunkt);
        return fastsatt
            .stream()
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

            return bgReferanser.isEmpty() ? List.of() : tjeneste.hentBeregningsgrunnlagListeDto(ref, bgReferanser).getBeregningsgrunnlagListe()
                .stream()
                .filter(bg -> bg.getBeregningsgrunnlag() != null)
                .map(BeregningsgrunnlagPrReferanse::getBeregningsgrunnlag)
                .sorted(Comparator.comparing(BeregningsgrunnlagDto::getSkjæringstidspunkt))
                .collect(Collectors.toList());
        }
        return List.of();
    }

    @Override
    public List<BeregningsgrunnlagGrunnlag> hentGrunnlag(BehandlingReferanse ref, Collection<LocalDate> skjæringstidspunkter) {
        var bgReferanser = finnBeregningsgrunnlagsReferanseFor(ref.getBehandlingId(), skjæringstidspunkter, false, false);
        if (bgReferanser.isEmpty()) {
            return List.of();
        }
        var tjeneste = finnTjeneste(ref.getFagsakYtelseType());
        return tjeneste.hentGrunnlag(ref, bgReferanser);
    }

    @Override
    public List<BeregningsgrunnlagKobling> hentKoblinger(BehandlingReferanse ref) {
        var vilkårene = vilkårResultatRepository.hent(ref.getBehandlingId());
        var vilkår = vilkårene.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow();

        var skjæringstidspunkter = vilkår.getPerioder()
            .stream()
            .map(VilkårPeriode::getSkjæringstidspunkt)
            .sorted()
            .distinct()
            .collect(Collectors.toList());
        var referanser = finnBeregningsgrunnlagsReferanseFor(ref.getBehandlingId(), skjæringstidspunkter, true, false);
        return referanser.stream()
            .map(it -> new BeregningsgrunnlagKobling(it.getStp(), it.getRef()))
            .collect(Collectors.toList());
    }

    @Override
    public void deaktiverBeregningsgrunnlag(BehandlingReferanse ref, Collection<LocalDate> skjæringstidspunkter) {
        var sortert = new TreeSet<>(skjæringstidspunkter);
        var referanser = finnBeregningsgrunnlagsReferanseFor(ref.getBehandlingId(), sortert, false, false);
        if (!referanser.isEmpty()) {
            var bgReferanser = referanser.stream()
                .filter(it -> !it.erGenerertReferanse())
                .map(BgRef::getRef)
                .collect(Collectors.toList());
            finnTjeneste(ref.getFagsakYtelseType()).deaktiverBeregningsgrunnlag(ref.getFagsakYtelseType(), ref.getSaksnummer(), bgReferanser);
        }
    }

    @Override
    public void gjenopprettInitiell(BehandlingReferanse ref) {
        grunnlagRepository.gjenopprettInitiell(ref.getBehandlingId());
    }

    private KalkulusApiTjeneste finnTjeneste(FagsakYtelseType fagsakYtelseType) {
        return FagsakYtelseTypeRef.Lookup.find(kalkulusTjenester, fagsakYtelseType)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke kalkulustjeneste for " + fagsakYtelseType));
    }

    private List<BgRef> finnReferanseEllerLagNy(Long behandlingId,
                                                Collection<LocalDate> skjæringstidspunkter,
                                                boolean kreverEksisterendeReferanse,
                                                boolean skalLageNyVedLikSomInitiell) {
        var refs = new ArrayList<>(finnBeregningsgrunnlagsReferanseFor(behandlingId, skjæringstidspunkter, kreverEksisterendeReferanse, skalLageNyVedLikSomInitiell));

        // generer refs som ikke eksisterer
        var referanserAlleredeDekket = BgRef.getStps(refs);
        for (var stp : skjæringstidspunkter) {
            if (!referanserAlleredeDekket.contains(stp)) {
                refs.add(new BgRef(stp));
            }
        }

        return Collections.unmodifiableList(refs);
    }

    private List<BgRef> finnBeregningsgrunnlagsReferanseFor(Long behandlingId,
                                                            Collection<LocalDate> skjæringstidspunkter,
                                                            boolean kreverEksisterendeReferanse,
                                                            boolean skalLageNyVedLikSomInitiell) {
        var grunnlagOptional = grunnlagRepository.hentGrunnlag(behandlingId);
        if (grunnlagOptional.isEmpty()) {
            return List.of();
        }

        var bgReferanser = finnBeregningsgrunnlagsReferanseForGrunnlag(behandlingId, grunnlagOptional.get(), skjæringstidspunkter, skalLageNyVedLikSomInitiell);

        if (bgReferanser.isEmpty() && !skjæringstidspunkter.isEmpty()) {
            throw new IllegalStateException("Forventer at referansen eksisterer for skjæringstidspunkt=" + skjæringstidspunkter);
        } else if (kreverEksisterendeReferanse) {
            var first = bgReferanser.stream().filter(BgRef::erGenerertReferanse).findFirst();
            if (first.isPresent()) {
                throw new IllegalStateException("Forventer at referansen eksisterer for skjæringstidspunkt=" + first.get().getStp());
            }
        }
        return bgReferanser;
    }

    private List<BgRef> finnBeregningsgrunnlagsReferanseForGrunnlag(Long behandlingId,
                                                                    BeregningsgrunnlagPerioderGrunnlag grunnlag,
                                                                    Collection<LocalDate> skjæringstidspunkter,
                                                                    boolean skalLageNyVedLikSomInitiell) {

        var grunnlagInitiellVersjon = grunnlagRepository.getInitiellVersjon(behandlingId);
        var resultater = new TreeSet<BgRef>();

        for (var stp : new TreeSet<>(skjæringstidspunkter)) {
            var beregningsgrunnlagPeriodeOpt = grunnlag.finnFor(stp);
            var grunnlagReferanse = beregningsgrunnlagPeriodeOpt.map(BeregningsgrunnlagPeriode::getEksternReferanse);
            if (grunnlagReferanse.isPresent() && skalLageNyVedLikSomInitiell) {
                if (grunnlagInitiellVersjon.isPresent()) {
                    var initReferanse = grunnlagInitiellVersjon.get().finnFor(stp).map(BeregningsgrunnlagPeriode::getEksternReferanse);
                    if (initReferanse.isPresent() && grunnlagReferanse.get().equals(initReferanse.get())) {
                        grunnlagReferanse = Optional.empty();
                    }
                }
            }

            resultater.add(new BgRef(grunnlagReferanse.orElse(null), stp));
        }
        return List.copyOf(resultater);
    }
}
