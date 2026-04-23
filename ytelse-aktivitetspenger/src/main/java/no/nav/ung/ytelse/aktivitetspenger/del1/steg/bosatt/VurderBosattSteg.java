package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsAvklaring;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlagRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.EtterlysningData;
import no.nav.ung.sak.etterlysning.EtterlysningTjeneste;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.vilkår.ManuelleVilkårRekkefølgeTjeneste;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import no.nav.ung.sak.vilkår.VilkårVurderingSteg;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_BOSTED;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_BOSTED)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class VurderBosattSteg extends VilkårVurderingSteg {

    private static final Duration DEFAULT_VENTEFRIST = Duration.ofDays(14);

    private ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private EtterlysningTjeneste etterlysningTjeneste;
    private BostedsGrunnlagRepository bostedsGrunnlagRepository;

    VurderBosattSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderBosattSteg(ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste,
                            VilkårResultatRepository vilkårResultatRepository,
                            VilkårTjeneste vilkårTjeneste,
                            BehandlingRepository behandlingRepository,
                            BostedsGrunnlagRepository bostedsGrunnlagRepository,
                            @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste,
                            EtterlysningTjeneste etterlysningTjeneste) {
        super(vilkårResultatRepository, vilkårTjeneste, behandlingRepository, vilkårsPerioderTilVurderingTjeneste);
        this.manuelleVilkårRekkefølgeTjeneste = manuelleVilkårRekkefølgeTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.bostedsGrunnlagRepository = bostedsGrunnlagRepository;
        this.etterlysningTjeneste = etterlysningTjeneste;
    }

    @Override
    public VilkårType getAktuellVilkårType() {
        return VilkårType.BOSTEDSVILKÅR;
    }

    @Override
    public Set<VilkårType> getVilkårAvhengigheter(FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        EnumSet<VilkårType> avhengigheter = EnumSet.noneOf(VilkårType.class);
        avhengigheter.add(VilkårType.ALDERSVILKÅR);
        avhengigheter.add(VilkårType.SØKNADSFRIST);
        avhengigheter.addAll(manuelleVilkårRekkefølgeTjeneste.finnManuelleVilkårSomErFør(getAktuellVilkårType(), ytelseType, behandlingType));
        return avhengigheter;
    }

    @Override
    public BehandleStegResultat utførResten(BehandlingskontrollKontekst kontekst) {
        long behandlingId = kontekst.getBehandlingId();
        LocalDateTimeline<Boolean> tidslinjeTilVurdering = finnPerioderSomSkalVurderes(kontekst);

        if (tidslinjeTilVurdering.isEmpty()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        // Hent gjeldende etterlysninger og bygg fom-dato-oppslag
        List<EtterlysningData> etterlysninger = etterlysningTjeneste.hentGjeldendeEtterlysninger(
            behandlingId, kontekst.getFagsakId(), EtterlysningType.UTTALELSE_BOSTED);
        Map<LocalDate, EtterlysningData> etterlysningPerFom = etterlysninger.stream()
            .collect(Collectors.toMap(e -> e.periode().getFomDato(), e -> e));

        // Hent foreslåtte avklaringer for å sjekke om saksbehandler allerede har vurdert (og var enig med søknaden)
        Collection<BostedsAvklaring> foreslåtteAvklaringer = bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId)
            .map(g -> g.getForeslåttHolder() != null ? g.getForeslåttHolder().getAvklaringer() : List.<BostedsAvklaring>of())
            .orElse(List.of());

        // Klassifiser perioder per fom-dato
        Set<LocalDate> ventendeFom = new LinkedHashSet<>();
        Set<DatoIntervallEntitet> skalFastsettePerioder = new LinkedHashSet<>();
        Set<LocalDate> trengerFastsettingFom = new LinkedHashSet<>();
        Set<LocalDate> trengerSaksbehandlerFom = new LinkedHashSet<>();

        tidslinjeTilVurdering.stream().forEach(segment -> {
            LocalDate fom = segment.getFom();
            EtterlysningData etterlysning = etterlysningPerFom.get(fom);

            if (etterlysning == null) {
                // Saksbehandler er enig med søknaden → foreslåtte avklaringer ble lagret uten etterlysning
                boolean harForeslåttAvklaring = foreslåtteAvklaringer.stream()
                    .anyMatch(a -> !a.getFomDato().isBefore(fom) && !a.getFomDato().isAfter(segment.getTom()));
                if (harForeslåttAvklaring) {
                    skalFastsettePerioder.add(DatoIntervallEntitet.fraOgMedTilOgMed(fom, segment.getTom()));
                } else {
                    trengerSaksbehandlerFom.add(fom);
                }
            } else if (etterlysning.status() == EtterlysningStatus.OPPRETTET
                || etterlysning.status() == EtterlysningStatus.VENTER) {
                ventendeFom.add(fom);
            } else if (etterlysning.status() == EtterlysningStatus.UTLØPT
                || (etterlysning.status() == EtterlysningStatus.MOTTATT_SVAR
                && etterlysning.uttalelseData() != null
                && !etterlysning.uttalelseData().harUttalelse())) {
                skalFastsettePerioder.add(DatoIntervallEntitet.fraOgMedTilOgMed(fom, segment.getTom()));
            } else if (etterlysning.status() == EtterlysningStatus.MOTTATT_SVAR
                && etterlysning.uttalelseData() != null
                && etterlysning.uttalelseData().harUttalelse()) {
                trengerFastsettingFom.add(fom);
            }
        });

        // VURDER_BOSTED først – perioder uten etterlysning trenger initial registrering fra saksbehandler.
        // Vi vil ikke sette behandlingen på vent før alle nødvendige etterlysninger er opprettet.
        if (!trengerSaksbehandlerFom.isEmpty()) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VURDER_BOSTED));
        }

        // Sett behandling på vent hvis alle etterlysninger er opprettet, men ingen svar er mottatt ennå
        if (!ventendeFom.isEmpty()) {
            LocalDateTime frist = ventendeFom.stream()
                .map(fom -> Optional.ofNullable(etterlysningPerFom.get(fom)).map(EtterlysningData::frist).orElse(null))
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(LocalDateTime.now().plus(DEFAULT_VENTEFRIST));
            return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(
                AksjonspunktResultat.opprettForAksjonspunktMedFrist(
                    EtterlysningType.UTTALELSE_BOSTED.tilAutopunktDefinisjon(),
                    EtterlysningType.UTTALELSE_BOSTED.mapTilVenteårsak(),
                    frist
                )
            ));
        }

        // FASTSETT_BOSTED – saksbehandler bekrefter/korrigerer etter brukerens uttalelse
        if (!trengerFastsettingFom.isEmpty()) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.FASTSETT_BOSTED));
        }

        // Fastsett perioder med tilstrekkelig svar (etter aksjonspunkt-beslutningen)
        if (!skalFastsettePerioder.isEmpty()) {
            bostedsGrunnlagRepository.fastsettForeslåtteAvklaringer(behandlingId, skalFastsettePerioder);
        }

        // Auto-vurder alle fastsatte perioder
        autoVurder(behandlingId);
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private void autoVurder(long behandlingId) {
        var grunnlag = bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Forventer bostedsgrunnlag for automatisk vurdering, behandlingId=" + behandlingId));

        var fastsattHolder = grunnlag.getFastsattHolder();
        if (fastsattHolder == null) {
            return;
        }

        List<BostedsAvklaring> avklaringerSortert = fastsattHolder.getAvklaringer().stream()
            .sorted(Comparator.comparing(BostedsAvklaring::getFomDato))
            .toList();

        Vilkårene vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Forventer vilkårresultat for behandling " + behandlingId));

        var builder = Vilkårene.builderFraEksisterende(vilkårene);
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.BOSTEDSVILKÅR);

        vilkårene.getVilkårTimeline(VilkårType.BOSTEDSVILKÅR).stream()
            .filter(s -> s.getValue().getUtfall() != Utfall.IKKE_RELEVANT)
            .forEach(s -> {
                LocalDate segmentFom = s.getFom();
                LocalDate segmentTom = s.getTom();

                // Finn alle avklaringer med fomDato innenfor dette segmentet, sortert
                List<BostedsAvklaring> avklaringerISegment = avklaringerSortert.stream()
                    .filter(a -> !a.getFomDato().isBefore(segmentFom) && !a.getFomDato().isAfter(segmentTom))
                    .toList();

                if (avklaringerISegment.isEmpty()) {
                    return;
                }

                for (int i = 0; i < avklaringerISegment.size(); i++) {
                    var avklaring = avklaringerISegment.get(i);
                    LocalDate fom = avklaring.getFomDato();
                    LocalDate tom = (i + 1 < avklaringerISegment.size())
                        ? avklaringerISegment.get(i + 1).getFomDato().minusDays(1)
                        : segmentTom;

                    var periodeBuilder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
                    if (avklaring.erBosattITrondheim()) {
                        periodeBuilder.medUtfall(Utfall.OPPFYLT);
                    } else {
                        periodeBuilder.medUtfall(Utfall.IKKE_OPPFYLT)
                            .medAvslagsårsak(Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED);
                    }
                    vilkårBuilder.leggTil(periodeBuilder);
                }
            });

        builder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(behandlingId, builder.build());
    }

}
