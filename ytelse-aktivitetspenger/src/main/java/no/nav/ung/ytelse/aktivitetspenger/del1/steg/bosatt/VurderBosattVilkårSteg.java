package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.bosatt.FraflyttingsÅrsak;
import no.nav.ung.kodeverk.bosatt.Kilde;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårJsonObjectMapper;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsAvklaringHolder;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsPeriodeAvklaring;
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

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_BOSTEDVILKÅR;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_BOSTEDVILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class VurderBosattVilkårSteg extends VilkårVurderingSteg {

    private static final Duration DEFAULT_VENTEFRIST = Duration.ofDays(14);
    private static final VilkårJsonObjectMapper VILKAR_JSON_OBJECT_MAPPER = new VilkårJsonObjectMapper();

    private ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private EtterlysningTjeneste etterlysningTjeneste;
    private BostedsGrunnlagRepository bostedsGrunnlagRepository;

    VurderBosattVilkårSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderBosattVilkårSteg(ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste,
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

        List<EtterlysningData> etterlysninger = etterlysningTjeneste.hentGjeldendeEtterlysninger(
            behandlingId, kontekst.getFagsakId(), EtterlysningType.UTTALELSE_BOSTED);
        Map<LocalDate, EtterlysningData> etterlysningPerFom = etterlysninger.stream()
            .collect(Collectors.toMap(e -> e.periode().getFomDato(), e -> e));

        var grunnlag = bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId).orElseThrow(() -> new IllegalStateException("Forventer grunnlag med bostedsavklaringer"));
        BostedsAvklaringHolder holder = grunnlag.getHolder();

        LocalDateTimeline<StegUtfall> stegutfallTidslinje = tidslinjeTilVurdering.map(segment -> vurder(segment, etterlysningPerFom, holder));

        // Etterlysning sendt, svar ikke mottatt ennå — sett behandling på vent
        if (!stegutfallTidslinje.filterValue(StegUtfall.VENTER_PÅ_UTTALELSE_FRA_BRUKER::equals).isEmpty()) {
            return settPåVent(stegutfallTidslinje, etterlysningPerFom);
        }

        // Auto-vurder alle ferdigperioder (ingen etterlysning, utløpt, eller svar uten uttalelse)
        autoVurder(behandlingId, stegutfallTidslinje, holder);
        // Finn perioder som krever manuell vurdering av vilkåret
        if (!stegutfallTidslinje.filterValue(StegUtfall.VILKÅR_VURDERES_MANUELT::equals).isEmpty()) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.MANUELL_VURDERING_BOSTEDSVILKÅR));
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private static List<LocalDateSegment<StegUtfall>> vurder(LocalDateSegment<Boolean> segment, Map<LocalDate, EtterlysningData> etterlysningPerFom, BostedsAvklaringHolder holder) {
        LocalDate fom = segment.getFom();
        EtterlysningData etterlysning = etterlysningPerFom.get(fom);
        BostedsPeriodeAvklaring avklaring = holder.getPeriodeAvklaring(segment.getFom()).orElseThrow(() -> new IllegalStateException("Forventer å finne en bostedsperiodeavklaring for "));
        boolean erÅrsakSomSkalVurderesManuelt = FraflyttingsÅrsak.ANNET.equals(avklaring.getFraflyttingsÅrsak());
        boolean erKildeSøknad = Kilde.SØKNAD.equals(avklaring.getKilde());

        if (etterlysning == null) {
            // Grunnlag finnes, ingen etterlysning → ferdig (auto-vurdering)
            return List.of(new LocalDateSegment<>(segment.getLocalDateInterval(), StegUtfall.VILKÅR_VURDERES_AUTOMATISK));
        } else if (erVentende(etterlysning)) {
            return List.of(new LocalDateSegment<>(segment.getLocalDateInterval(), StegUtfall.VENTER_PÅ_UTTALELSE_FRA_BRUKER));
        } else if (harMottattSvarMedUttalelse(etterlysning) || erÅrsakSomSkalVurderesManuelt || erKildeSøknad) {
            return List.of(new LocalDateSegment<>(segment.getLocalDateInterval(), StegUtfall.VILKÅR_VURDERES_MANUELT));
        }
        return List.of(new LocalDateSegment<>(segment.getLocalDateInterval(), StegUtfall.VILKÅR_VURDERES_AUTOMATISK));
    }


    private static BehandleStegResultat settPåVent(LocalDateTimeline<StegUtfall> stegutfallTidslinje, Map<LocalDate, EtterlysningData> etterlysningPerFom) {
        Set<LocalDate> ventendeFom = stegutfallTidslinje.filterValue(StegUtfall.VENTER_PÅ_UTTALELSE_FRA_BRUKER::equals)
            .toSegments().stream().map(LocalDateSegment::getFom).collect(Collectors.toSet());
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

    private static boolean erVentende(EtterlysningData etterlysning) {
        return etterlysning.status() == EtterlysningStatus.OPPRETTET
            || etterlysning.status() == EtterlysningStatus.VENTER;
    }

    private static boolean harMottattSvarMedUttalelse(EtterlysningData etterlysning) {
        return etterlysning != null
            && etterlysning.status() == EtterlysningStatus.MOTTATT_SVAR
            && etterlysning.uttalelseData() != null
            && etterlysning.uttalelseData().harUttalelse();
    }

    private void autoVurder(long behandlingId,
                            LocalDateTimeline<StegUtfall> stegutfallTidslinje,
                            BostedsAvklaringHolder holder) {

        Vilkårene vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Forventer vilkårresultat for behandling " + behandlingId));

        var builder = Vilkårene.builderFraEksisterende(vilkårene);
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.BOSTEDSVILKÅR);
        stegutfallTidslinje.filterValue(StegUtfall.VILKÅR_VURDERES_AUTOMATISK::equals)
            .toSegments()
            .forEach(s -> {
                BostedsPeriodeAvklaring periodeAvklaring = holder.getPeriodeAvklaring(s.getFom()).orElseThrow(() -> new IllegalStateException("Kan ikke vurdere vilkår automatisk uten faktaopplysning"));
                String regelInput = lagRegelInput(periodeAvklaring);

                if (!periodeAvklaring.isErBosattITrondheim()) {
                    // Bruker er ikke bosatt fra start, avslår hele perioden
                    var periodeBuilder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fra(s.getLocalDateInterval()))
                        .medRegelInput(regelInput);
                    settIkkeOppfylt(periodeBuilder);
                    vilkårBuilder.leggTil(periodeBuilder);
                } else if (periodeAvklaring.getFraflyttingsDato() == null) {
                    // Bruker er bosatt fra start og fraflyttingsdato er ikke satt, vilkåret er oppfylt i hele perioden
                    var periodeBuilder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fra(s.getLocalDateInterval()));
                    periodeBuilder.medUtfall(Utfall.OPPFYLT).medRegelInput(regelInput);
                    vilkårBuilder.leggTil(periodeBuilder);
                } else {
                    LocalDate fraflyttingsDato = periodeAvklaring.getFraflyttingsDato();
                    if (!fraflyttingsDato.isAfter(s.getFom())) {
                        throw new IllegalStateException("Forventer ar fraflyttingsdato er etter skjæringstidspunkt: " + periodeAvklaring);
                    } else if (fraflyttingsDato.isAfter(s.getTom())) {
                        // Vilkårsperioden er endret og fraflyttingsdato ligger utenfor perioden (etter sluttdato), vilkåret er oppfylt i hele perioden
                        var periodeBuilder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fra(s.getLocalDateInterval()));
                        periodeBuilder.medUtfall(Utfall.OPPFYLT).medRegelInput(regelInput);
                        vilkårBuilder.leggTil(periodeBuilder);
                    } else {
                        // Bruker flytter fra Trondheim i løpet av perioden, Setter oppfylt fram til fraflytting, deretter avslått
                        var oppfyltBuilder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), fraflyttingsDato.minusDays(1)));
                        oppfyltBuilder.medUtfall(Utfall.OPPFYLT).medRegelInput(regelInput);
                        vilkårBuilder.leggTil(oppfyltBuilder);

                        var ikkeOppfyltBuilder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fraOgMedTilOgMed(fraflyttingsDato, s.getTom()));
                        ikkeOppfyltBuilder.medRegelInput(regelInput);
                        settIkkeOppfylt(ikkeOppfyltBuilder);
                        vilkårBuilder.leggTil(ikkeOppfyltBuilder);
                    }
                }
            });

        builder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(behandlingId, builder.build());
    }

    private static void settIkkeOppfylt(no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder periodeBuilder) {
        periodeBuilder.medUtfall(Utfall.IKKE_OPPFYLT)
            .medAvslagsårsak(Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED);
    }

    private static String lagRegelInput(BostedsPeriodeAvklaring periodeAvklaring) {
        return VILKAR_JSON_OBJECT_MAPPER.writeValueAsString(new RegelInput(
            periodeAvklaring.getReferanse(),
            periodeAvklaring.getSkjæringstidspunkt(),
            periodeAvklaring.isErBosattITrondheim(),
            periodeAvklaring.getFraflyttingsDato(),
            periodeAvklaring.getFraflyttingsÅrsak(),
            periodeAvklaring.getKilde()));
    }

    private record RegelInput(UUID referanse,
                              LocalDate skjaeringstidspunkt,
                              boolean erBosattITrondheim,
                              LocalDate fraflyttingsDato,
                              FraflyttingsÅrsak fraflyttingsAarsak,
                              Kilde kilde) {
    }

    private enum StegUtfall {
        VILKÅR_VURDERES_AUTOMATISK,
        VILKÅR_VURDERES_MANUELT,
        VENTER_PÅ_UTTALELSE_FRA_BRUKER

    }

}
