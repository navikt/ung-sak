package no.nav.ung.ytelse.aktivitetspenger.medlemskap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.k9.søknad.felles.type.Landkode;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapPeriode;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårJsonObjectMapper;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.stream.Collectors;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class ForutgåendeMedlemskapsvilkårSteg implements BehandlingSteg {

    private VilkårResultatRepository vilkårResultatRepository;
    private OppgittForutgåendeMedlemskapRepository forutgåendeMedlemskapRepository;
    private BehandlingRepository behandlingRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;

    public ForutgåendeMedlemskapsvilkårSteg() {
    }

    @Inject
    public ForutgåendeMedlemskapsvilkårSteg(VilkårResultatRepository vilkårResultatRepository,
                                            OppgittForutgåendeMedlemskapRepository forutgåendeMedlemskapRepository,
                                            @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                            BehandlingRepository behandlingRepository) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.forutgåendeMedlemskapRepository = forutgåendeMedlemskapRepository;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var vilkårene = vilkårResultatRepository.hent(behandlingId);

        var vilkår = vilkårene.getVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET).orElseThrow();
        var ikkeVurdertePerioder = vilkår.getPerioder().stream()
            .filter(it -> Objects.equals(it.getUtfall(), Utfall.IKKE_VURDERT))
            .map(VilkårPeriode::getPeriode)
            .collect(Collectors.toSet());

        if (ikkeVurdertePerioder.isEmpty()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var perioderTilVurdering = getPerioderTilVurderingTjeneste(behandling.getFagsakYtelseType(), behandling.getType())
            .utled(behandlingId, VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET);

        if (perioderTilVurdering.isEmpty()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        return vurderForutgåendeMedlemskap(perioderTilVurdering, behandlingId, vilkårene);
    }

    private BehandleStegResultat vurderForutgåendeMedlemskap(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, Long behandlingId, Vilkårene vilkårene) {
        var tidligsteVirkningsdato = perioderTilVurdering.stream()
            .map(DatoIntervallEntitet::getFomDato)
            .min(LocalDate::compareTo)
            .orElseThrow();

        var forutgåendePeriodeTilVurdering = lagForutgåendePeriodeTilVurdering(tidligsteVirkningsdato);

        var grunnlagOpt = forutgåendeMedlemskapRepository.hentGrunnlagHvisEksisterer(behandlingId);
        if (grunnlagOpt.isEmpty()) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAP));
        }

        var grunnlag = grunnlagOpt.get();

        var bostederTidslinje = lagBostederTidslinje(grunnlag);

        var vurdering = vurderBosteder(forutgåendePeriodeTilVurdering, bostederTidslinje);

        var trengerManuellVurdering = vurdering.filterValue(v -> v != Utfall.OPPFYLT);

        if (trengerManuellVurdering.isEmpty()) {
            oppfyllVilkår(bostederTidslinje, perioderTilVurdering, behandlingId, Vilkårene.builderFraEksisterende(vilkårene));
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAP));
    }

    private void oppfyllVilkår(LocalDateTimeline<String> bostederTidslinje, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, Long behandlingId, VilkårResultatBuilder vilkårResultatBuilder) {
        var jsonMapper = new VilkårJsonObjectMapper();
        String regelInput = jsonMapper.writeValueAsString(new RegelInput(bostederTidslinje));
        String regelEvaluering = jsonMapper.writeValueAsString(new RegelEvaluering("OPPFYLT", "Alle bosteder i forutgående 5-årsperiode er i land med gyldig trygdeavtale"));

        var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET);

        perioderTilVurdering.stream()
            .map(it -> vilkårBuilder.hentBuilderFor(it)
                .medUtfall(Utfall.OPPFYLT)
                .medAvslagsårsak(null)
                .medRegelInput(regelInput)
                .medRegelEvaluering(regelEvaluering))
            .forEach(vilkårBuilder::leggTil);

        vilkårResultatBuilder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(behandlingId, vilkårResultatBuilder.build());
    }

    private static LocalDateInterval lagForutgåendePeriodeTilVurdering(LocalDate tidligsteVirkningsdato) {
        return new LocalDateInterval(tidligsteVirkningsdato.minusYears(5), tidligsteVirkningsdato.minusDays(1));
    }

    private static LocalDateTimeline<Utfall> vurderBosteder(LocalDateInterval forutgåendePeriodeTilVurdering, LocalDateTimeline<String> bostederTidslinje) {
        return new LocalDateTimeline<>(forutgåendePeriodeTilVurdering, Boolean.TRUE)
            .combine(bostederTidslinje, ForutgåendeMedlemskapsvilkårSteg::vurderBosted, JoinStyle.LEFT_JOIN);
    }

    private static LocalDateSegment<Utfall> vurderBosted(LocalDateInterval intervall, LocalDateSegment<Boolean> lhs, LocalDateSegment<String> rhs) {
        if (rhs == null || rhs.getValue() == null) {
            return new LocalDateSegment<>(intervall, Utfall.IKKE_VURDERT);
        }
        if (TrygdeavtaleLandOppslag.erGyldigTrygdeavtaleLand(rhs.getValue(), intervall.getFomDato())) {
            return new LocalDateSegment<>(intervall, Utfall.OPPFYLT);
        }
        return new LocalDateSegment<>(intervall, Utfall.IKKE_OPPFYLT);
    }

    private static LocalDateTimeline<String> lagBostederTidslinje(OppgittForutgåendeMedlemskapGrunnlag grunnlag) {
        // Bruker kun grunnlag for den nyeste søknaden og antar at den overskriver tidligere oppgitte perioder. Hvis grunnlagene må merges i fremtiden, gjøres det her.
        var nyestePeriode = grunnlag.getOppgittePerioder().stream()
            .max(Comparator.comparing(OppgittForutgåendeMedlemskapPeriode::getMottattTidspunkt))
            .orElseThrow();

        var bostedetNorgeTidslinje = new LocalDateTimeline<>(nyestePeriode.getPeriode().getFomDato(), nyestePeriode.getPeriode().getTomDato(), Landkode.NORGE.getLandkode());

        var bostederUtlandTidslinje = new LocalDateTimeline<>(
            nyestePeriode.getBostederUtland().stream()
                .map(b -> new LocalDateSegment<>(b.getPeriode().getFomDato(), b.getPeriode().getTomDato(), b.getLandkode()))
                .toList());

        return bostederUtlandTidslinje.crossJoin(bostedetNorgeTidslinje);
    }

    record RegelInput(LocalDateTimeline<String> bostederLandkodeTidslinje) { }

    record RegelEvaluering(String utfall, String begrunnelse) {}

    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(FagsakYtelseType fagsakYtelseType, BehandlingType behandlingType) {
        return VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, fagsakYtelseType, behandlingType);
    }
}
