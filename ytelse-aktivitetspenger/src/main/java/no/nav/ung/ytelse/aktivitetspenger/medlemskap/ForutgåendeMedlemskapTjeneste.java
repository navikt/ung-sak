package no.nav.ung.ytelse.aktivitetspenger.medlemskap;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapPeriode;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.SøktStartdato;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.ung.sak.kontrakt.aktivitetspenger.medlemskap.MedlemskapAvslagsÅrsakType;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.kontrakt.vilkår.medlemskap.ForutgåendeMedlemskapResponse;
import no.nav.ung.sak.kontrakt.vilkår.medlemskap.MedlemskapDto;
import no.nav.ung.sak.kontrakt.vilkår.medlemskap.MedlemskapPeriodeInfoDto;
import no.nav.ung.sak.kontrakt.vilkår.medlemskap.UtenlandsoppholdDto;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.perioder.AktivitetspengerSøknadsperiodeTjeneste;

import java.util.*;

@Dependent
public class ForutgåendeMedlemskapTjeneste {

    private final OppgittForutgåendeMedlemskapRepository forutgåendeMedlemskapRepository;
    private final BehandlingRepository behandlingRepository;
    private final StartdatoRepository startdatoRepository;
    private final VilkårResultatRepository vilkårResultatRepository;
    private final Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;

    @Inject
    public ForutgåendeMedlemskapTjeneste(
        OppgittForutgåendeMedlemskapRepository forutgåendeMedlemskapRepository,
        BehandlingRepository behandlingRepository,
        VilkårResultatRepository vilkårResultatRepository,
        StartdatoRepository startdatoRepository,
        @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester) {
        this.forutgåendeMedlemskapRepository = forutgåendeMedlemskapRepository;
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.startdatoRepository = startdatoRepository;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
    }


    private static LocalDateSegmentCombinator<VilkårPeriode, MedlemskapDto, MedlemskapPeriodeInfoDto> combinatorVilkårPeriodeTilVurderingOgMedlemskapFraBruker(LocalDateTimeline<Boolean> periodeTilVurderingTidslinje) {
        return (LocalDateInterval di, LocalDateSegment<VilkårPeriode> lhs, LocalDateSegment<MedlemskapDto> rhs) -> {
            var vp = lhs.getValue();
            var tilVurdering = !periodeTilVurderingTidslinje.intersection(di).isEmpty();
            var medlemskapFraBruker = rhs.getValue() != null ? rhs.getValue() : null;
            var avslagsårsak = mapAvslagsårsak(vp.getAvslagsårsak());
            MedlemskapPeriodeInfoDto medlemskapPeriodeInfoDto = new MedlemskapPeriodeInfoDto(
                new Periode(di.getFomDato(), di.getTomDato()),
                vp.getGjeldendeUtfall(),
                avslagsårsak,
                vp.getBegrunnelse(),
                tilVurdering,
                vp.getErManueltVurdert(),
                medlemskapFraBruker
            );
            return new LocalDateSegment<>(di, medlemskapPeriodeInfoDto);
        };
    }


    public ForutgåendeMedlemskapResponse hentMedlemskapOgVilkårSomDto(BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());

        var medlemskap = hentMedlemskapForBehandlingSomDto(behandling.getId());

        var vilkår = vilkårResultatRepository.hent(behandling.getId())
            .getVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET)
            .orElseThrow(() -> new IllegalStateException("Mangler vilkårsvurdering av forutgående medlemskap"));

        var vilkårTidslinje = new LocalDateTimeline<>(vilkår.getPerioder().stream()
            .filter(it -> it.getUtfall() != Utfall.IKKE_RELEVANT)
            .map(it -> new LocalDateSegment<>(it.getFom(), it.getTom(), it))
            .toList());

        var startdatoGrunnlag = startdatoRepository.hentGrunnlag(behandling.getId()).orElseThrow();
        var oppgittMedlemskapTidslinje = lagOppgittMedlemskapTidslinjeSplittetPåStartdatoer(medlemskap, startdatoGrunnlag);
        var periodeTilVurderingTidslinje = lagPeriodeTilVurderingTidslinje(behandling);
        var medlemskapsInfoTidslinje = vilkårTidslinje
            .combine(oppgittMedlemskapTidslinje,
                combinatorVilkårPeriodeTilVurderingOgMedlemskapFraBruker(periodeTilVurderingTidslinje),
                LocalDateTimeline.JoinStyle.LEFT_JOIN);

        return new ForutgåendeMedlemskapResponse(TidslinjeUtil.values(medlemskapsInfoTidslinje));
    }

    private LocalDateTimeline<MedlemskapDto> lagOppgittMedlemskapTidslinjeSplittetPåStartdatoer(List<MedlemskapDto> medlemskap, StartdatoGrunnlag startdatoGrunnlag) {
        LocalDateTimeline<SøktStartdato> virkningstidspunktTidslinje = startdatoGrunnlag.getOppgitteStartdatoer().getStartdatoer().stream()
            .map(s -> AktivitetspengerSøknadsperiodeTjeneste.tidslinjeFraVirkningstidspunkt(s.getStartdato(), s))
            .reduce(LocalDateTimeline.empty(), LocalDateTimeline::crossJoin)
            .compress();

        return virkningstidspunktTidslinje.mapSegment(s ->
            medlemskap.stream()
                .filter(it -> it.journalpostId().equals(s.getJournalpostId().getVerdi()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Finner ikke oppgitt medlemskap for journalpostId " + s.getJournalpostId().getVerdi())));

    }

    private LocalDateTimeline<Boolean> lagPeriodeTilVurderingTidslinje(Behandling behandling) {
        var perioderTilVurderingTjeneste = getPerioderTilVurderingTjeneste(behandling.getFagsakYtelseType(), behandling.getType());
        NavigableSet<DatoIntervallEntitet> perioderTilVurdering = perioderTilVurderingTjeneste.utled(behandling.getId(), VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET);
        return TidslinjeUtil.tilTidslinje(perioderTilVurdering);
    }

    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(FagsakYtelseType fagsakYtelseType, BehandlingType behandlingType) {
        return VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, fagsakYtelseType, behandlingType);
    }

    private static MedlemskapAvslagsÅrsakType mapAvslagsårsak(Avslagsårsak avslagsårsak) {
        if (avslagsårsak == null) return null;
        return switch (avslagsårsak) {
            case SØKER_ER_IKKE_MEDLEM -> MedlemskapAvslagsÅrsakType.SØKER_IKKE_MEDLEM;
            default -> throw new IllegalStateException("Unexpected value: " + avslagsårsak);
        };
    }

    public List<MedlemskapDto> hentMedlemskapForBehandlingSomDto(Long behandlingId) {
        Optional<OppgittForutgåendeMedlemskapGrunnlag> grunnlagOpt = forutgåendeMedlemskapRepository.hentGrunnlagHvisEksisterer(behandlingId);
        if (grunnlagOpt.isEmpty()) {
            return Collections.emptyList();
        }
        var grunnlag = grunnlagOpt.get();

        return grunnlag.getOppgittePerioder().stream()
            .map(ForutgåendeMedlemskapTjeneste::mapTilDto)
            .toList();
    }

    private static MedlemskapDto mapTilDto(OppgittForutgåendeMedlemskapPeriode m) {
        return new MedlemskapDto(
            m.getPeriode().tilPeriode(),
            m.harBoddINorge(),
            m.harJobbetINorge(),
            m.harJobbetUtenforNorge(),
            m.getJournalpostId().getVerdi(),
            m.getUtenlandsopphold().stream().map(u ->
                new UtenlandsoppholdDto(
                    new Periode(u.getPeriode().getFomDato(), u.getPeriode().getTomDato()),
                    mapLandTilNorskNavn(u.getLand().getKode()),
                    u.getLand().getKode(),
                    TrygdeavtaleLandOppslag.erGyldigTrygdeavtaleLand(u.getLand(), u.getPeriode().getFomDato()),
                    u.harJobbetIPerioden(),
                    u.getUtenlandskNasjonalId()
                )).toList());
    }

    private static final Map<String, String> LANDKODE_TIL_NORSK_NAVN = lagLandkodeTilNorskNavn();

    private static Map<String, String> lagLandkodeTilNorskNavn() {
        Map<String, String> result = new HashMap<>();
        for (String alpha2 : Locale.getISOCountries()) {
            try {
                Locale locale = new Locale.Builder().setRegion(alpha2).build();
                result.put(locale.getISO3Country(), locale.getDisplayCountry(Locale.forLanguageTag("nb-NO")));
            } catch (MissingResourceException | IllformedLocaleException ignored) {
            }
        }
        return Map.copyOf(result);
    }

    private static String mapLandTilNorskNavn(String landkodeAlpha3) {
        return LANDKODE_TIL_NORSK_NAVN.getOrDefault(landkodeAlpha3, landkodeAlpha3);
    }
}
