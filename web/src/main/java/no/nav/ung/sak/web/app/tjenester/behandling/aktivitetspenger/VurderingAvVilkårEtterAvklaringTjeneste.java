package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sikkerhet.context.SubjectHandler;
import no.nav.ung.kodeverk.vilkår.IkkeOppfyltDetaljertÅrsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.VilkårsvurderingResultat;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.VurderingAvVilkårPeriodeEtterAvklaringDto;
import no.nav.ung.sak.typer.Periode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static no.nav.fpsak.tidsserie.LocalDateInterval.TIDENES_ENDE;

/**
 * Felles logikk for vurdering av et inngangsvilkår ved opphør eller avslått periode.
 */
@ApplicationScoped
public class VurderingAvVilkårEtterAvklaringTjeneste {

    private VilkårResultatRepository vilkårResultatRepository;

    VurderingAvVilkårEtterAvklaringTjeneste() {
        // for CDI proxy
    }

    @Inject
    public VurderingAvVilkårEtterAvklaringTjeneste(VilkårResultatRepository vilkårResultatRepository) {
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    public LocalDateTimeline<VilkårsvurderingResultat> utled(long behandlingId,
                                                             VilkårType vilkårType,
                                                             List<VurderingAvVilkårPeriodeEtterAvklaringDto> vurdertePerioder,
                                                             LocalDateTimeline<IkkeOppfyltDetaljertÅrsak> årsakTidslinje) {
        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId).orElseThrow();
        LocalDateTimeline<VilkårPeriode> eksisterendeVilkårperioder = vilkårene.getVilkårTimeline(vilkårType)
            .filterValue(v -> v.getUtfall() != Utfall.IKKE_RELEVANT);
        if (eksisterendeVilkårperioder.isEmpty()) {
            throw new IllegalArgumentException("Fant ingen relevante vilkårsperioder for " + vilkårType
                + " på behandlingId=" + behandlingId);
        }
        LocalDate maksDatoIVilkårsperioden = eksisterendeVilkårperioder.getMaxLocalDate();

        LocalDateTimeline<VurderingAvVilkårPeriodeEtterAvklaringDto> vurdertTidslinje = new LocalDateTimeline<>(
            vurdertePerioder.stream()
                .map(it -> new LocalDateSegment<>(
                    it.periode().getFom(),
                    lukkÅpenPeriode(it.periode(), maksDatoIVilkårsperioden),
                    it))
                .toList())
            // unngår å innføre avslag der det er hull i de eksisterende vilkårsperiodene
            .intersection(eksisterendeVilkårperioder);

        validerVurdertPeriodeErDekketAvAvklaring(behandlingId, vilkårType, vurdertTidslinje, årsakTidslinje);

        String vurdertAv = SubjectHandler.getSubjectHandler().getUid();
        LocalDateTime vurdertTidspunkt = LocalDateTime.now();

        return vurdertTidslinje.combine(årsakTidslinje,
            (di, vurdering, årsak) ->
                new LocalDateSegment<>(di, byggResultat(di, vilkårType, vurdering.getValue(), årsak.getValue(), vurdertAv, vurdertTidspunkt)),
            LocalDateTimeline.JoinStyle.INNER_JOIN
        );
    }

    private static void validerVurdertPeriodeErDekketAvAvklaring(long behandlingId,
                                                                 VilkårType vilkårType,
                                                                 LocalDateTimeline<VurderingAvVilkårPeriodeEtterAvklaringDto> vurdertTidslinje,
                                                                 LocalDateTimeline<IkkeOppfyltDetaljertÅrsak> årsakTidslinje) {
        if (årsakTidslinje.isEmpty()) {
            throw new IllegalArgumentException("Kan ikke vurdere " + vilkårType
                + " ved avslått periode/opphør uten at det finnes en ikkeOppfyltÅrsak (foreslått vilkårsavklaring) på behandlingen. behandlingId=" + behandlingId);
        }
        var utenAvklaring = vurdertTidslinje.disjoint(årsakTidslinje);
        if (!utenAvklaring.isEmpty()) {
            throw new IllegalArgumentException(
                "Forsøker å vurdere perioder som ikke dekkes av en ikkeOppfyltÅrsak (mangler foreslått vilkårsavklaring). Gjelder perioder: " + utenAvklaring);
        }
    }

    private static VilkårsvurderingResultat byggResultat(LocalDateInterval interval,
                                                         VilkårType vilkårType,
                                                         VurderingAvVilkårPeriodeEtterAvklaringDto vurdering,
                                                         IkkeOppfyltDetaljertÅrsak årsakFraAvklaring,
                                                         String vurdertAv,
                                                         LocalDateTime vurdertTidspunkt) {
        var ikkeOppfyltÅrsak = vurdering.erVilkårOppfylt() ? null : årsakFraAvklaring;

        if (ikkeOppfyltÅrsak != null && ikkeOppfyltÅrsak.kreverFritekst()) {
            if (vurdering.fritekstVurderingBrev() == null || vurdering.fritekstVurderingBrev().isBlank()) {
                throw new IllegalArgumentException("fritekstVurderingBrev er påkrevd når avklaringens årsak "
                    + ikkeOppfyltÅrsak.getKode() + " krever fritekst. Gjelder periode: " + interval);
            }
        }

        return new VilkårsvurderingResultat(
            vilkårType,
            vurdering.erVilkårOppfylt(),
            ikkeOppfyltÅrsak,
            true,
            vurdering.begrunnelse(),
            vurdering.fritekstVurderingBrev(),
            vurdertAv,
            vurdertTidspunkt);
    }

    static LocalDate lukkÅpenPeriode(Periode periode, LocalDate senesteTomVilkårsperiode) {
        var erÅpenPeriode = periode.getTom() == null || periode.getTom().equals(TIDENES_ENDE);
        return erÅpenPeriode ? senesteTomVilkårsperiode : periode.getTom();
    }
}
