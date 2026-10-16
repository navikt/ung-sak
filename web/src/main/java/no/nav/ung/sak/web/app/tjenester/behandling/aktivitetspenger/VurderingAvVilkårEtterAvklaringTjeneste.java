package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sikkerhet.context.SubjectHandler;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.IkkeOppfyltDetaljertÅrsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.GenereltVilkårsvurderingResultat;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.VurderingAvVilkårPeriodeEtterAvklaringDto;
import no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger.VurderingEtterAvklaringResultat.AvklartPeriode;
import no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger.VurderingEtterAvklaringResultat.RegelEvaluering;
import no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger.VurderingEtterAvklaringResultat.RegelInput;
import no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger.VurderingEtterAvklaringResultat.VilkårsvurderingResultatOgEvaluering;
import no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger.VurderingEtterAvklaringResultat.VurdertPeriode;
import no.nav.ung.sak.typer.Periode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static no.nav.fpsak.tidsserie.LocalDateInterval.TIDENES_ENDE;

/**
 * Felles logikk for vurdering av et inngangsvilkår ved opphør eller avslått periode.
 */
@Dependent
public class VurderingAvVilkårEtterAvklaringTjeneste {

    private final VilkårResultatRepository vilkårResultatRepository;

    @Inject
    public VurderingAvVilkårEtterAvklaringTjeneste(VilkårResultatRepository vilkårResultatRepository) {
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    public VurderingEtterAvklaringResultat utled(long behandlingId,
                                                 VilkårType vilkårType,
                                                 List<VurderingAvVilkårPeriodeEtterAvklaringDto> vurdertePerioder,
                                                 LocalDateTimeline<IkkeOppfyltDetaljertÅrsak> årsakTidslinje) {
        if (årsakTidslinje.isEmpty()) {
            throw new IllegalArgumentException("Kan ikke vurdere " + vilkårType
                + " ved avslått periode/opphør uten at det finnes en ikkeOppfyltÅrsak (foreslått vilkårsavklaring) på behandlingen. behandlingId=" + behandlingId);
        }

        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId).orElseThrow();
        LocalDateTimeline<VilkårPeriode> eksisterendeVilkårperioder = relevanteVilkårsperioder(vilkårene, vilkårType);

        if (eksisterendeVilkårperioder.isEmpty()) {
            throw new IllegalArgumentException("Fant ingen relevante vilkårsperioder for " + vilkårType
                + " på behandlingId=" + behandlingId);
        }
        LocalDate maksDatoIVilkårsperioden = eksisterendeVilkårperioder.getMaxLocalDate();

        validerPerioderFraDtoOverlapperEksisterendeVilkårperioder(behandlingId, vilkårType, vurdertePerioder, eksisterendeVilkårperioder);

        LocalDateTimeline<VurderingAvVilkårPeriodeEtterAvklaringDto> vurdertTidslinje = new LocalDateTimeline<>(
            vurdertePerioder.stream()
                .map(it -> new LocalDateSegment<>(
                    it.periode().getFom(),
                    lukkÅpenPeriode(it.periode(), maksDatoIVilkårsperioden),
                    it))
                .toList())
            // unngår å innføre avslag der det er hull i de eksisterende vilkårsperiodene
            .intersection(eksisterendeVilkårperioder);

        // avklaringen kan strekke seg over hull i vilkårsperiodene (f.eks. etter opphørsdato); der finnes ingen vilkårsperiode å vurdere
        var avklaringTidslinje = årsakTidslinje.intersection(eksisterendeVilkårperioder);
        validerVurdertPeriodeErDekketAvAvklaring(behandlingId, vilkårType, vurdertTidslinje, avklaringTidslinje);
        // valideres mot den ikke-klippede avklaringen, slik at en avklaring som strekker seg over et hull i vilkårsperiodene ikke deler opp valideringen
        validerÉnVurderingPerAvklaring(behandlingId, vilkårType, vurdertTidslinje, årsakTidslinje);

        String vurdertAv = SubjectHandler.getSubjectHandler().getUid();
        LocalDateTime vurdertTidspunkt = LocalDateTime.now();

        var vurderinger = vurdertTidslinje.combine(avklaringTidslinje,
            (di, vurdering, årsak) ->
                new LocalDateSegment<>(di, byggResultat(di, vilkårType, vurdering.getValue(), årsak.getValue(), maksDatoIVilkårsperioden, vurdertAv, vurdertTidspunkt)),
            LocalDateTimeline.JoinStyle.INNER_JOIN
        );

        var regelInput = new RegelInput(
            vurdertePerioder.stream().map(VurderingAvVilkårEtterAvklaringTjeneste::tilVurdertPeriode).toList(),
            årsakTidslinje.segmenter().stream()
                .map(s -> new AvklartPeriode(s.getFom(), s.getTom(), s.getValue() == null ? null : s.getValue().getKode()))
                .toList(),
            eksisterendeVilkårperioder.getLocalDateIntervals().stream()
                .map(i -> new Periode(i.getFomDato(), i.getTomDato()))
                .toList()
        );

        return new VurderingEtterAvklaringResultat(vurderinger, regelInput);
    }

    private static VurdertPeriode tilVurdertPeriode(VurderingAvVilkårPeriodeEtterAvklaringDto dto) {
        return new VurdertPeriode(
            dto.periode().getFom(),
            dto.periode().getTom(),
            dto.erVilkårOppfylt());
    }

    // Delt med VilkårsavklaringRestTjeneste, slik at det som vises til vurdering er det samme som utled godtar
    static LocalDateTimeline<VilkårPeriode> relevanteVilkårsperioder(Vilkårene vilkårene, VilkårType vilkårType) {
        return vilkårene.getVilkårTimeline(vilkårType)
            .filterValue(v -> v.getUtfall() != Utfall.IKKE_RELEVANT)
            .filterValue(v -> v.getAvslagsårsak() != Avslagsårsak.AVKORTET);
    }

    private static void validerPerioderFraDtoOverlapperEksisterendeVilkårperioder(long behandlingId,
                                                                                  VilkårType vilkårType,
                                                                                  List<VurderingAvVilkårPeriodeEtterAvklaringDto> vurdertePerioder,
                                                                                  LocalDateTimeline<VilkårPeriode> eksisterendeVilkårperioder) {
        for (var dto : vurdertePerioder) {
            Periode periode = dto.periode();
            LocalDate fom = periode.getFom();
            LocalDate tom = periode.getTom();
            boolean erÅpenPeriode = tom == null || tom.equals(TIDENES_ENDE);

            if (erÅpenPeriode) {
                var fomTidslinje = new LocalDateTimeline<>(fom, fom, Boolean.TRUE);
                if (!fomTidslinje.intersects(eksisterendeVilkårperioder)) {
                    throw new IllegalArgumentException("Fom for åpen vurdert periode " + periode
                        + " overlapper ikke eksisterende vilkårperioder for " + vilkårType + " på behandlingId=" + behandlingId);
                }
            } else {
                var periodeTidslinje = new LocalDateTimeline<>(fom, tom, Boolean.TRUE);
                if (!periodeTidslinje.disjoint(eksisterendeVilkårperioder).isEmpty()) {
                    throw new IllegalArgumentException("Lukket vurdert periode " + periode
                        + " overlapper ikke i sin helhet med eksisterende vilkårperioder for " + vilkårType + " på behandlingId=" + behandlingId);
                }
            }
        }
    }

    private static void validerVurdertPeriodeErDekketAvAvklaring(long behandlingId,
                                                                 VilkårType vilkårType,
                                                                 LocalDateTimeline<VurderingAvVilkårPeriodeEtterAvklaringDto> vurdertTidslinje,
                                                                 LocalDateTimeline<IkkeOppfyltDetaljertÅrsak> avklaringTidslinje) {
        if (vurdertTidslinje.isEmpty()) {
            throw new IllegalArgumentException("Må ha minst 1 vurdert periode, og må overlappe eksisterende vilkårtidslinje for " + vilkårType
                + " på behandlingId=" + behandlingId);
        }

        var utenAvklaring = vurdertTidslinje.disjoint(avklaringTidslinje);
        if (!utenAvklaring.isEmpty()) {
            throw new IllegalArgumentException(
                "Forsøker å vurdere perioder som ikke dekkes av en ikkeOppfyltÅrsak (mangler foreslått vilkårsavklaring). Gjelder perioder: " + utenAvklaring);
        }

        var utenVurdering = avklaringTidslinje.disjoint(vurdertTidslinje);
        if (!utenVurdering.isEmpty()) {
            throw new IllegalArgumentException(
                "Vilkårsvurdering fyller ikke hele perioden som er avklart, og som derfor må vurderes på nytt. Gjelder perioder: " + utenVurdering);
        }
    }

    // VilkårsavklaringRestTjeneste viser ett utfall og én vurdering per avklaring; Tillater derfor ikke flere vurderinger for en avklaring frem til vi har en måte å presentere det på.
    // Sammenligner innholdet uten periode, siden periodene til dto-ene allerede er dekket av at de til sammen fyller avklaringen.
    private static void validerÉnVurderingPerAvklaring(long behandlingId,
                                                       VilkårType vilkårType,
                                                       LocalDateTimeline<VurderingAvVilkårPeriodeEtterAvklaringDto> vurdertTidslinje,
                                                       LocalDateTimeline<IkkeOppfyltDetaljertÅrsak> årsakTidslinje) {
        for (var avklaring : årsakTidslinje.segmenter()) {
            var ulikeVurderinger = vurdertTidslinje.intersection(avklaring.getLocalDateInterval()).stream()
                .map(LocalDateSegment::getValue)
                .map(v -> List.of(v.erVilkårOppfylt(), Objects.toString(v.begrunnelse(), ""), Objects.toString(v.fritekstVurderingBrev(), "")))
                .distinct()
                .toList();
            if (ulikeVurderinger.size() > 1) {
                throw new IllegalArgumentException("Avklaringen for " + vilkårType + " i periode " + avklaring.getLocalDateInterval()
                    + " må vurderes samlet med ett utfall, men har " + ulikeVurderinger.size() + " ulike vurderinger. behandlingId=" + behandlingId);
            }
        }
    }

    private static VilkårsvurderingResultatOgEvaluering byggResultat(LocalDateInterval interval,
                                                                     VilkårType vilkårType,
                                                                     VurderingAvVilkårPeriodeEtterAvklaringDto vurdering,
                                                                     IkkeOppfyltDetaljertÅrsak årsakFraAvklaring,
                                                                     LocalDate maksDatoIVilkårsperioden,
                                                                     String vurdertAv,
                                                                     LocalDateTime vurdertTidspunkt) {
        var ikkeOppfyltÅrsak = vurdering.erVilkårOppfylt() ? null : årsakFraAvklaring;

        if (ikkeOppfyltÅrsak != null && ikkeOppfyltÅrsak.kreverFritekst()) {
            if (vurdering.fritekstVurderingBrev() == null || vurdering.fritekstVurderingBrev().isBlank()) {
                throw new IllegalArgumentException("fritekstVurderingBrev er påkrevd når avklaringens årsak "
                    + ikkeOppfyltÅrsak.getKode() + " krever fritekst. Gjelder periode: " + interval);
            }
        }

        var resultat = new GenereltVilkårsvurderingResultat(
            vilkårType,
            vurdering.erVilkårOppfylt(),
            ikkeOppfyltÅrsak,
            true,
            vurdering.begrunnelse(),
            vurdering.fritekstVurderingBrev(),
            vurdertAv,
            vurdertTidspunkt);

        var periode = vurdering.periode();
        var lukketTom = lukkÅpenPeriode(periode, maksDatoIVilkårsperioden);
        var evaluering = new RegelEvaluering(
            interval.getFomDato(),
            interval.getTomDato(),
            (vurdering.erVilkårOppfylt() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT).getKode(),
            ikkeOppfyltÅrsak == null ? null : ikkeOppfyltÅrsak.getKode(),
            !interval.getFomDato().equals(periode.getFom()) || !interval.getTomDato().equals(lukketTom)
        );

        return new VilkårsvurderingResultatOgEvaluering(resultat, evaluering);
    }

    static LocalDate lukkÅpenPeriode(Periode periode, LocalDate senesteTomVilkårsperiode) {
        var erÅpenPeriode = periode.getTom() == null || periode.getTom().equals(TIDENES_ENDE);
        return erÅpenPeriode ? senesteTomVilkårsperiode : periode.getTom();
    }
}
