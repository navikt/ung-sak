package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPeriode;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPerioderGrunnlag;
import no.nav.k9.sak.ytelse.beregning.grunnlag.InputOverstyringPeriode;

@Dependent
public class BeregningsgrunnlagTjeneste implements BeregningTjeneste {

    private static final Logger log = LoggerFactory.getLogger(BeregningsgrunnlagTjeneste.class);

    private final Instance<KalkulusApiTjeneste> kalkulusTjenester;
    private final VilkårResultatRepository vilkårResultatRepository;
    private final BeregningPerioderGrunnlagRepository grunnlagRepository;
    private final HentReferanserTjeneste hentReferanserTjeneste;

    @Inject
    public BeregningsgrunnlagTjeneste(@Any Instance<KalkulusApiTjeneste> kalkulusTjenester,
                                      VilkårResultatRepository vilkårResultatRepository,
                                      BeregningPerioderGrunnlagRepository grunnlagRepository) {
        this.kalkulusTjenester = kalkulusTjenester;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.grunnlagRepository = grunnlagRepository;
        this.hentReferanserTjeneste = new HentReferanserTjeneste(grunnlagRepository, vilkårResultatRepository);
    }

    @Override
    public SamletKalkulusResultat beregn(BehandlingReferanse referanse, Collection<DatoIntervallEntitet> vilkårsperioder, BehandlingStegType stegType) {
        if (vilkårsperioder == null || vilkårsperioder.isEmpty()) {
            throw new IllegalArgumentException("Forventer minst en vilkårsperiode");
        }
        var skjæringstidspunkter = vilkårsperioder.stream()
            .map(DatoIntervallEntitet::getFomDato)
            .collect(Collectors.toCollection(TreeSet::new));
        var bgReferanser = hentReferanserTjeneste.finnReferanseEllerLagNy(referanse.getBehandlingId(), skjæringstidspunkter, false, BehandlingType.REVURDERING.equals(referanse.getBehandlingType()));

        if (bgReferanser.isEmpty()) {
            throw new IllegalArgumentException("Forventer minst en bgReferanse");
        }
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

    private List<BeregnInput> lagBeregnInput(BehandlingReferanse referanse, Collection<DatoIntervallEntitet> vilkårsperioder, List<BgRef> bgReferanser) {
        var originalReferanserMap = hentReferanserTjeneste.finnMapTilOriginaleReferanserUtenAvslag(referanse, vilkårsperioder, bgReferanser);
        var beregningInput = bgReferanser.stream().map(e -> {
            var bgRef = e.getRef();
            var stp = e.getStp();
            var vilkårsperiode = vilkårsperioder.stream().filter(p -> p.getFomDato().equals(stp)).findFirst().orElseThrow();
            grunnlagRepository.lagre(referanse.getBehandlingId(), new BeregningsgrunnlagPeriode(bgRef, stp));
            Optional<InputOverstyringPeriode> inputOverstyring = finnInputOverstyring(referanse, stp);
            return new BeregnInput(bgRef, vilkårsperiode, originalReferanserMap.get(bgRef), inputOverstyring.orElse(null));
        }).collect(Collectors.toList());
        return beregningInput;
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
        if (harGrunnlagsperioder(beregningsgrunnlagPerioderGrunnlag)) {
            var tjeneste = finnTjeneste(ref.getFagsakYtelseType());
            var vilkårene = vilkårResultatRepository.hent(ref.getBehandlingId());
            var vilkår = vilkårene.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow();
            var grunnlag = beregningsgrunnlagPerioderGrunnlag.get();

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

    @Override
    public List<BeregningsgrunnlagKobling> hentKoblingerForInnvilgedePerioder(BehandlingReferanse ref) {
        var vilkårene = vilkårResultatRepository.hent(ref.getBehandlingId());
        var vilkår = vilkårene.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow();

        var skjæringstidspunkter = vilkår.getPerioder()
            .stream()
            .map(VilkårPeriode::getSkjæringstidspunkt)
            .sorted()
            .distinct()
            .collect(Collectors.toList());
        var referanser = hentReferanserTjeneste.finnBeregningsgrunnlagsReferanseFor(ref.getBehandlingId(), skjæringstidspunkter, false, false);
        return referanser.stream()
            .filter(it -> !it.erGenerertReferanse())
            .map(it -> new BeregningsgrunnlagKobling(it.getStp(), it.getRef()))
            .collect(Collectors.toList());
    }

    @Override
    public void deaktiverBeregningsgrunnlag(BehandlingReferanse ref, Collection<LocalDate> skjæringstidspunkter) {
        var sortert = new TreeSet<>(skjæringstidspunkter);
        var referanser = hentReferanserTjeneste.finnBeregningsgrunnlagsReferanseFor(ref.getBehandlingId(), sortert, false, false);
        if (!referanser.isEmpty()) {
            Optional<BeregningsgrunnlagPerioderGrunnlag> initiellVersjon = Objects.equals(ref.getBehandlingType(), BehandlingType.REVURDERING) ? grunnlagRepository.getInitiellVersjon(ref.getBehandlingId()) : Optional.empty();
            var bgReferanser = referanser.stream()
                .filter(it -> !it.erGenerertReferanse())
                .filter(it -> erIkkeInitiellVersjon(initiellVersjon, it))
                .map(BgRef::getRef)
                .collect(Collectors.toList());
            finnTjeneste(ref.getFagsakYtelseType()).deaktiverBeregningsgrunnlag(ref.getFagsakYtelseType(), ref.getSaksnummer(), bgReferanser);
        }
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
