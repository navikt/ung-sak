package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.varsel.EndringType;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsAvklaring;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseRepository;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseV2;
import no.nav.ung.sak.etterlysning.EtterlysningData;
import no.nav.ung.sak.etterlysning.EtterlysningOgUttalelseTjeneste;
import no.nav.ung.sak.etterlysning.EtterlysningTjeneste;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.vilkår.ManuelleVilkårRekkefølgeTjeneste;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import no.nav.ung.sak.vilkår.VilkårVurderingSteg;

import java.time.Duration;
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
    private UttalelseRepository uttalelseRepository;
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
                            EtterlysningRepository etterlysningRepository,
                            UttalelseRepository uttalelseRepository,
                            BostedsGrunnlagRepository bostedsGrunnlagRepository,
                            @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste,
                            EtterlysningTjeneste etterlysningTjeneste) {
        super(vilkårResultatRepository, vilkårTjeneste, behandlingRepository, vilkårsPerioderTilVurderingTjeneste);
        this.manuelleVilkårRekkefølgeTjeneste = manuelleVilkårRekkefølgeTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.uttalelseRepository = uttalelseRepository;
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
        List<EtterlysningData> etterlysninger = etterlysningTjeneste.hentGjeldendeEtterlysninger(behandlingId, kontekst.getFagsakId(), EtterlysningType.UTTALELSE_BOSTED);
        // Aktiv etterlysning (OPPRETTET/VENTER) → sett behandling på vent
        var ventendeEtterlysninger = etterlysninger.stream()
            .filter(it ->
                it.status() == EtterlysningStatus.OPPRETTET ||
                    it.status() == EtterlysningStatus.VENTER)
            .toList();
        if (!ventendeEtterlysninger.isEmpty()) {
            LocalDateTime frist = ventendeEtterlysninger.stream().map(EtterlysningData::frist)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(LocalDateTime.now().plus(DEFAULT_VENTEFRIST));
            return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(
                AksjonspunktResultat.opprettForAksjonspunktMedFrist(
                    EtterlysningType.UTTALELSE_BOSTED.tilAutopunktDefinisjon(),
                    EtterlysningType.UTTALELSE_BOSTED.mapTilVenteårsak(),
                    frist
                )
            ));
        }

        // Etterlysning avsluttet (MOTTATT_SVAR eller UTLØPT) → sjekk utfall
        var sisteEtterlysning = etterlysninger.stream()
            .filter(it -> it.status() == EtterlysningStatus.MOTTATT_SVAR)
            .toList();
        if (!sisteEtterlysning.isEmpty()) {
            boolean harUttalelse = uttalelseRepository.hentUttalelser(behandlingId, EndringType.AVKLAR_BOSTED)
                .stream().anyMatch(UttalelseV2::harUttalelse);
            if (harUttalelse) {
                // Bruker har sendt uttalelse → saksbehandler må ta stilling
                return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VURDER_BOSTED));
            }
            // UTLØPT eller MOTTATT_SVAR uten uttalelse → automatisk vurdering
            autoVurder(behandlingId);
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        // Ingen etterlysning ennå → saksbehandler registrerer faktagrunnlag
        if (vilkårResultatRepository.finnesRelevantPeriode(behandlingId, getAktuellVilkårType())) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VURDER_BOSTED));
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private void autoVurder(long behandlingId) {
        var grunnlag = bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Forventer bostedsgrunnlag for automatisk vurdering, behandlingId=" + behandlingId));

        Map<java.time.LocalDate, Boolean> bosattPerSkjæringstidspunkt = grunnlag.getHolder().getAvklaringer().stream()
            .collect(Collectors.toMap(BostedsAvklaring::getSkjæringstidspunkt, BostedsAvklaring::erBosattITrondheim));

        Vilkårene vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Forventer vilkårresultat for behandling " + behandlingId));

        var builder = Vilkårene.builderFraEksisterende(vilkårene);
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.BOSTEDSVILKÅR);

        vilkårene.getVilkårTimeline(VilkårType.BOSTEDSVILKÅR).stream()
            .filter(s -> s.getValue().getUtfall() != Utfall.IKKE_RELEVANT)
            .forEach(s -> {
                var fom = s.getFom();
                Boolean erBosattITrondheim = bosattPerSkjæringstidspunkt.get(fom);
                if (erBosattITrondheim == null) {
                    throw new IllegalStateException("Mangler bostedsavklaring for skjæringstidspunkt " + fom + " i behandling " + behandlingId);
                }
                var periodeBuilder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fraOgMedTilOgMed(fom, s.getTom()));
                if (erBosattITrondheim) {
                    periodeBuilder.medUtfall(Utfall.OPPFYLT);
                } else {
                    periodeBuilder.medUtfall(Utfall.IKKE_OPPFYLT)
                        .medAvslagsårsak(Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED);
                }
                vilkårBuilder.leggTil(periodeBuilder);
            });

        builder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(behandlingId, builder.build());
    }

}
