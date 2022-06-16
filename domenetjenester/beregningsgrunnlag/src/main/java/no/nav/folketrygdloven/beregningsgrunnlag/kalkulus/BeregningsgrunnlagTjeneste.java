package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.BgRef;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagKobling;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.SamletKalkulusResultat;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.BeregningsgrunnlagPrReferanse;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagListe;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;
import no.nav.k9.sak.vilkår.VilkårPeriodeFilterProvider;
import no.nav.k9.sak.vilkår.VilkårTjeneste;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPeriode;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPerioderGrunnlag;
import no.nav.k9.sak.ytelse.beregning.grunnlag.InputOverstyringPeriode;

@Dependent
public class BeregningsgrunnlagTjeneste implements BeregningTjeneste {

    private static final Logger log = LoggerFactory.getLogger(BeregningsgrunnlagTjeneste.class);

    private final Instance<KalkulusApiTjeneste> kalkulusTjenester;
    private final BeregningPerioderGrunnlagRepository grunnlagRepository;
    private final HentReferanserTjeneste hentReferanserTjeneste;
    private final VilkårTjeneste vilkårTjeneste;
    private final VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider;

    @Inject
    public BeregningsgrunnlagTjeneste(@Any Instance<KalkulusApiTjeneste> kalkulusTjenester,
                                      VilkårResultatRepository vilkårResultatRepository,
                                      BeregningPerioderGrunnlagRepository grunnlagRepository,
                                      VilkårTjeneste vilkårTjeneste,
                                      VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider) {
        this.kalkulusTjenester = kalkulusTjenester;
        this.grunnlagRepository = grunnlagRepository;
        this.vilkårTjeneste = vilkårTjeneste;
        this.hentReferanserTjeneste = new HentReferanserTjeneste(grunnlagRepository, vilkårResultatRepository);
        this.vilkårPeriodeFilterProvider = vilkårPeriodeFilterProvider;
    }


    @Override
    public SamletKalkulusResultat startBeregning(BehandlingReferanse referanse, Collection<PeriodeTilVurdering> vilkårsperioder, BehandlingStegType stegType) {
        if (vilkårsperioder == null || vilkårsperioder.isEmpty()) {
            throw new IllegalArgumentException("Forventer minst en vilkårsperiode");
        }
        var skjæringstidspunkter = vilkårsperioder.stream()
            .map(PeriodeTilVurdering::getPeriode)
            .map(DatoIntervallEntitet::getFomDato)
            .collect(Collectors.toCollection(TreeSet::new));
        var bgReferanser = hentReferanserTjeneste.finnReferanseEllerLagNy(referanse.getBehandlingId(), skjæringstidspunkter, false, BehandlingType.REVURDERING.equals(referanse.getBehandlingType()));

        if (bgReferanser.isEmpty()) {
            throw new IllegalArgumentException("Forventer minst en bgReferanse");
        }
        lagreReferanser(referanse, bgReferanser);
        List<BeregnInput> beregningInput = lagBeregnInput(referanse, vilkårsperioder, bgReferanser);
        return finnTjeneste(referanse.getFagsakYtelseType()).beregn(referanse, beregningInput, stegType);
    }

    @Override
    public void kopier(BehandlingReferanse referanse, Collection<PeriodeTilVurdering> vilkårsperioder) {
        if (vilkårsperioder == null || vilkårsperioder.isEmpty()) {
            throw new IllegalArgumentException("Forventer minst en vilkårsperiode");
        }
        if (!vilkårsperioder.stream().allMatch(PeriodeTilVurdering::erForlengelse)) {
            throw new IllegalArgumentException("Kun forlengelseperioder kan kopieres fra original behandling");
        }
        var skjæringstidspunkter = vilkårsperioder.stream()
            .map(PeriodeTilVurdering::getPeriode)
            .map(DatoIntervallEntitet::getFomDato)
            .collect(Collectors.toCollection(TreeSet::new));
        var bgReferanser = hentReferanserTjeneste.finnReferanseEllerLagNy(referanse.getBehandlingId(), skjæringstidspunkter, false, BehandlingType.REVURDERING.equals(referanse.getBehandlingType()));

        if (bgReferanser.isEmpty()) {
            throw new IllegalArgumentException("Forventer minst en bgReferanse");
        }
        lagreReferanser(referanse, bgReferanser);
        List<BeregnInput> beregningInput = lagBeregnInput(referanse, vilkårsperioder, bgReferanser);
        finnTjeneste(referanse.getFagsakYtelseType()).kopier(referanse, beregningInput);
    }


    @Override
    public SamletKalkulusResultat beregn(BehandlingReferanse referanse, Collection<PeriodeTilVurdering> vilkårsperioder, BehandlingStegType stegType) {
        if (vilkårsperioder == null || vilkårsperioder.isEmpty()) {
            throw new IllegalArgumentException("Forventer minst en vilkårsperiode");
        }
        var skjæringstidspunkter = vilkårsperioder.stream()
            .map(PeriodeTilVurdering::getPeriode)
            .map(DatoIntervallEntitet::getFomDato)
            .collect(Collectors.toCollection(TreeSet::new));
        var bgReferanser = hentReferanserTjeneste.finnBeregningsgrunnlagsReferanseFor(referanse.getBehandlingId(), skjæringstidspunkter, true, false);
        List<BeregnInput> beregningInput = lagBeregnInput(referanse, vilkårsperioder, bgReferanser);
        return finnTjeneste(referanse.getFagsakYtelseType()).beregn(referanse, beregningInput, stegType);
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
    public List<OppdaterBeregningsgrunnlagResultat> oppdaterBeregningListe(Map<LocalDate, HåndterBeregningDto> stpTilDtoMap,
                                                                           BehandlingReferanse ref) {
        if (stpTilDtoMap == null || stpTilDtoMap.isEmpty()) {
            throw new IllegalArgumentException("Forventer minst ett ytelseGrunnlag");
        }
        var sortertMap = new TreeMap<>(stpTilDtoMap);

        var bgReferanser = hentReferanserTjeneste.finnReferanseEllerLagNy(ref.getBehandlingId(), sortertMap.keySet(), true, false);

        Map<UUID, LocalDate> referanseTilStpMap = bgReferanser.stream().collect(Collectors.toMap(BgRef::getRef, BgRef::getStp));

        Map<LocalDate, UUID> stpTilReferanseMap = referanseTilStpMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey, (e1, e2) -> e1, TreeMap::new));
        Map<UUID, HåndterBeregningDto> referanseTilDtoMap = sortertMap.entrySet().stream().collect(Collectors.toMap(e -> stpTilReferanseMap.get(e.getKey()), Map.Entry::getValue));
        List<OppdaterBeregningsgrunnlagResultat> resultatListe = finnTjeneste(ref.getFagsakYtelseType()).oppdaterBeregningListe(ref, bgReferanser, referanseTilDtoMap);

        resultatListe.forEach(e -> e.setSkjæringstidspunkt(referanseTilStpMap.get(e.getReferanse()))); // sett stp for hvert resultat resultater

        return resultatListe;
    }

    @Override
    public List<Beregningsgrunnlag> hentEksaktFastsatt(BehandlingReferanse ref, Collection<LocalDate> skjæringstidspunkter) {
        var bgReferanser = hentReferanserTjeneste.finnReferanseEllerLagNy(ref.getBehandlingId(), skjæringstidspunkter, true, false);

        return finnTjeneste(ref.getFagsakYtelseType()).hentEksaktFastsatt(ref, bgReferanser);
    }

    @Override
    public List<Beregningsgrunnlag> hentEksaktFastsattForAllePerioderInkludertAvslag(BehandlingReferanse ref) {
        var vilkårene = vilkårTjeneste.hentVilkårResultat(ref.getBehandlingId());
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
        var vilkårene = vilkårTjeneste.hentVilkårResultat(ref.getBehandlingId());
        var vilkår = vilkårene.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow();

        List<LocalDate> skjæringstidspunkt = vilkår.getPerioder()
            .stream()
            .filter(it -> !Utfall.IKKE_OPPFYLT.equals(it.getUtfall()))
            .peek(it -> {
                if (!Objects.equals(Utfall.OPPFYLT, it.getGjeldendeUtfall())) {
                    throw new ManglerBeregningsgrunnlagException("Mangler grunnlag pga periode som ikke er vurdert.");
                }
            })
            .map(VilkårPeriode::getSkjæringstidspunkt)
            .collect(Collectors.toList());

        List<Beregningsgrunnlag> fastsatt = hentEksaktFastsatt(ref, skjæringstidspunkt);

        if (skjæringstidspunkt.size() != fastsatt.size()) {
            throw new ManglerBeregningsgrunnlagException("Avvik mellom innvilgede perioder og grunnlag :: bg:" + fastsatt.size() + " -- vp" + skjæringstidspunkt.size());
        }

        return fastsatt
            .stream()
            .sorted(Comparator.comparing(Beregningsgrunnlag::getSkjæringstidspunkt))
            .collect(Collectors.toList());
    }

    private List<BeregnInput> lagBeregnInput(BehandlingReferanse referanse,
                                             Collection<PeriodeTilVurdering> vilkårsperioder,
                                             List<BgRef> bgReferanser) {
        var perioder = vilkårsperioder.stream().map(PeriodeTilVurdering::getPeriode).collect(Collectors.toCollection(TreeSet::new));
        var originalReferanserMap = hentReferanserTjeneste.finnMapTilOriginaleReferanserUtenAvslag(referanse, perioder, bgReferanser);
        return bgReferanser.stream().map(e -> {
            var bgRef = e.getRef();
            var stp = e.getStp();
            var vilkårsperiode = vilkårsperioder.stream().filter(p -> p.getPeriode().getFomDato().equals(stp)).findFirst().orElseThrow();
            Optional<InputOverstyringPeriode> inputOverstyring = finnInputOverstyring(referanse, stp);
            return new BeregnInput(bgRef,
                vilkårsperiode.getPeriode(),
                vilkårsperiode.erForlengelse(),
                originalReferanserMap.get(bgRef),
                inputOverstyring.orElse(null));
        }).collect(Collectors.toList());
    }

    private void lagreReferanser(BehandlingReferanse referanse,
                                 List<BgRef> bgReferanser) {
        bgReferanser.forEach(e -> {
            var bgRef = e.getRef();
            var stp = e.getStp();
            grunnlagRepository.lagre(referanse.getBehandlingId(), new BeregningsgrunnlagPeriode(bgRef, stp));
        });
    }


    @Override
    public List<BeregningsgrunnlagDto> hentBeregningsgrunnlagDtoer(BehandlingReferanse ref) {

        var beregningsgrunnlag = hentBeregningsgrunnlag(ref);
        if (beregningsgrunnlag.isEmpty()) {
            return List.of();
        } else {
            return beregningsgrunnlag.get().getBeregningsgrunnlagListe()
                .stream()
                .filter(this::harBeregningsgrunnlagOgStp)
                .map(BeregningsgrunnlagPrReferanse::getBeregningsgrunnlag)
                .sorted(Comparator.comparing(BeregningsgrunnlagDto::getSkjæringstidspunkt))
                .collect(Collectors.toList());
        }
    }

    @Override
    public Optional<BeregningsgrunnlagListe> hentBeregningsgrunnlag(BehandlingReferanse ref) {
        var beregningsgrunnlagPerioderGrunnlag = grunnlagRepository.hentGrunnlag(ref.getBehandlingId());
        var vilkårene = vilkårTjeneste.hentHvisEksisterer(ref.getBehandlingId());
        var vilkårOptional = vilkårene.flatMap(it -> it.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR));
        if (harGrunnlagsperioder(beregningsgrunnlagPerioderGrunnlag) && vilkårOptional.isPresent()) {
            var vilkår = vilkårOptional.get();
            var tjeneste = finnTjeneste(ref.getFagsakYtelseType());
            var grunnlag = beregningsgrunnlagPerioderGrunnlag.orElseThrow();

            var bgReferanser = vilkår.getPerioder()
                .stream()
                .map(VilkårPeriode::getSkjæringstidspunkt)
                .filter(it -> grunnlag.finnGrunnlagFor(it).isPresent())
                .map(it -> new BeregningsgrunnlagReferanse(grunnlag.finnGrunnlagFor(it).map(BeregningsgrunnlagPeriode::getEksternReferanse).orElseThrow(), it))
                .filter(it -> Objects.nonNull(it.getReferanse()))
                .collect(Collectors.toSet());

            return Optional.of(tjeneste.hentBeregningsgrunnlagListeDto(ref, bgReferanser));
        }
        return Optional.empty();
    }

    private boolean harGrunnlagsperioder(Optional<BeregningsgrunnlagPerioderGrunnlag> beregningsgrunnlagPerioderGrunnlag) {
        return !beregningsgrunnlagPerioderGrunnlag.map(BeregningsgrunnlagPerioderGrunnlag::getGrunnlagPerioder).orElse(Collections.emptyList())
            .isEmpty();
    }

    @Override
    public List<BeregningsgrunnlagGrunnlag> hentGrunnlag(BehandlingReferanse ref, Collection<LocalDate> skjæringstidspunkter) {
        var bgReferanser = hentReferanserTjeneste.finnBeregningsgrunnlagsReferanseFor(ref.getBehandlingId(), skjæringstidspunkter, false, false);
        if (bgReferanser.isEmpty()) {
            return List.of();
        }
        var tjeneste = finnTjeneste(ref.getFagsakYtelseType());
        return tjeneste.hentGrunnlag(ref, bgReferanser);
    }

    public List<BeregningsgrunnlagKobling> hentKoblingerForPerioder(BehandlingReferanse ref) {
        var vilkårene = vilkårTjeneste.hentVilkårResultat(ref.getBehandlingId());
        var vilkår = vilkårene.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow();
        var skjæringstidspunkter = vilkår.getPerioder()
            .stream()
            .map(VilkårPeriode::getSkjæringstidspunkt)
            .sorted()
            .distinct()
            .collect(Collectors.toList());
        var referanser = hentReferanserTjeneste.finnBeregningsgrunnlagsReferanseFor(ref.getBehandlingId(), skjæringstidspunkter, false, false);
        var forlengelsePerioder = finnForlengelseperioder(ref, vilkår);
        return referanser.stream()
            .filter(it -> !it.erGenerertReferanse())
            .map(it -> new BeregningsgrunnlagKobling(it.getStp(), it.getRef(), forlengelsePerioder.stream().anyMatch(p -> p.getFomDato().equals(it.getStp()))))
            .collect(Collectors.toList());
    }

    public List<BeregningsgrunnlagKobling> hentKoblingerForPerioderTilVurdering(BehandlingReferanse ref) {
        var perioderTilVurdering = vilkårTjeneste.utledPerioderTilVurdering(ref, VilkårType.BEREGNINGSGRUNNLAGVILKÅR);

        var vilkårene = vilkårTjeneste.hentVilkårResultat(ref.getBehandlingId());
        var vilkår = vilkårene.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow();
        var skjæringstidspunkter = vilkår.getPerioder()
            .stream()
            .map(VilkårPeriode::getSkjæringstidspunkt)
            .sorted()
            .distinct()
            .collect(Collectors.toList());
        var referanser = hentReferanserTjeneste.finnBeregningsgrunnlagsReferanseFor(ref.getBehandlingId(), skjæringstidspunkter, false, false);
        var forlengelsePerioder = finnForlengelseperioder(ref, vilkår);
        return referanser.stream()
            .filter(it -> !it.erGenerertReferanse())
            .filter(it -> perioderTilVurdering.stream().anyMatch(p -> p.getFomDato().equals(it.getStp())))
            .map(it -> new BeregningsgrunnlagKobling(it.getStp(), it.getRef(), forlengelsePerioder.stream().anyMatch(p -> p.getFomDato().equals(it.getStp()))))
            .collect(Collectors.toList());
    }

    private Set<DatoIntervallEntitet> finnForlengelseperioder(BehandlingReferanse ref, Vilkår vilkår) {
        var filter = vilkårPeriodeFilterProvider.getFilter(ref, true);
        var forlengelsePerioder = filter.filtrerPerioder(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).collect(Collectors.toSet()), VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .stream().filter(PeriodeTilVurdering::erForlengelse)
            .map(PeriodeTilVurdering::getPeriode)
            .collect(Collectors.toSet());
        return forlengelsePerioder;
    }

    public void deaktiverBeregningsgrunnlagForAvslåttEllerFjernetPeriode(BehandlingReferanse ref) {
        var vilkårOptional = vilkårTjeneste.hentHvisEksisterer(ref.getBehandlingId())
            .flatMap(v -> v.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR));

        if (vilkårOptional.isPresent()) {
            var vilkår = vilkårOptional.get();
            var vilkårsSkjæringspunkter = vilkår.getPerioder().stream().map(VilkårPeriode::getSkjæringstidspunkt).collect(Collectors.toSet());
            var grunnlagOpt = grunnlagRepository.hentGrunnlag(ref.getBehandlingId());
            var avslåtteSkjæringstidspunkt = vilkår.getPerioder().stream()
                .filter(vp -> vp.getUtfall().equals(Utfall.IKKE_OPPFYLT))
                .map(VilkårPeriode::getSkjæringstidspunkt)
                .collect(Collectors.toSet());
            var referanserSomSkalDeaktiveres = grunnlagOpt.stream().flatMap(g -> g.getGrunnlagPerioder()
                    .stream())
                .map(p -> new BgRef(p.getEksternReferanse(), p.getSkjæringstidspunkt()))
                .filter(it -> erAvslått(avslåtteSkjæringstidspunkt, it) ||
                    harFjernetSkjæringstidspunkt(vilkårsSkjæringspunkter, it))
                .collect(Collectors.toList());

            if (!referanserSomSkalDeaktiveres.isEmpty()) {
                Optional<BeregningsgrunnlagPerioderGrunnlag> initiellVersjon = Objects.equals(ref.getBehandlingType(), BehandlingType.REVURDERING) ? grunnlagRepository.getInitiellVersjon(ref.getBehandlingId()) : Optional.empty();
                var bgReferanser = referanserSomSkalDeaktiveres.stream()
                    .filter(it -> erIkkeInitiellVersjon(initiellVersjon, it))
                    .map(BgRef::getRef)
                    .collect(Collectors.toList());
                finnTjeneste(ref.getFagsakYtelseType()).deaktiverBeregningsgrunnlag(ref.getFagsakYtelseType(), ref.getSaksnummer(), ref.getBehandlingUuid(), bgReferanser);
            }
        }

    }

    private boolean harFjernetSkjæringstidspunkt(Set<LocalDate> vilkårsSkjæringspunkter, BgRef it) {
        return vilkårsSkjæringspunkter.stream().noneMatch(vstp -> it.getStp().equals(vstp));
    }

    private boolean erAvslått(Set<LocalDate> avslåtteSkjæringstidspunkt, BgRef it) {
        return avslåtteSkjæringstidspunkt.stream().anyMatch(stp -> it.getStp().equals(stp));
    }

    @Override
    public void gjenopprettInitiell(BehandlingReferanse ref) {
        grunnlagRepository.gjenopprettInitiell(ref.getBehandlingId());
    }

    private boolean harBeregningsgrunnlagOgStp(BeregningsgrunnlagPrReferanse<BeregningsgrunnlagDto> bg) {
        if (bg.getBeregningsgrunnlag() == null) {
            return false;
        }
        if (bg.getBeregningsgrunnlag().getSkjæringstidspunkt() == null) {
            log.info("Mottok beregningsgrunnlag uten stp for referanse {}", bg.getEksternReferanse());
            return false;
        }
        return true;
    }

    private Optional<InputOverstyringPeriode> finnInputOverstyring(BehandlingReferanse referanse, LocalDate stp) {
        Optional<InputOverstyringPeriode> inputOverstyring = grunnlagRepository.hentGrunnlag(referanse.getBehandlingId()).stream()
            .flatMap(gr -> gr.getInputOverstyringPerioder().stream())
            .filter(p -> p.getSkjæringstidspunkt().equals(stp))
            .findFirst();
        return inputOverstyring;
    }

    private boolean erIkkeInitiellVersjon(Optional<BeregningsgrunnlagPerioderGrunnlag> initiellVersjon, BgRef it) {
        return !initiellVersjon.flatMap(at -> at.finnGrunnlagFor(it.getStp())
                .map(BeregningsgrunnlagPeriode::getEksternReferanse))
            .equals(Optional.of(it.getRef()));
    }

    private KalkulusApiTjeneste finnTjeneste(FagsakYtelseType fagsakYtelseType) {
        return FagsakYtelseTypeRef.Lookup.find(kalkulusTjenester, fagsakYtelseType)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke kalkulustjeneste for " + fagsakYtelseType));
    }

}
