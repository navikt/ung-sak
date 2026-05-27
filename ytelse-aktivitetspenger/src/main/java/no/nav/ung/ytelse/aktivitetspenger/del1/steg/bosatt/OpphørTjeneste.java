package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.bosatt.OpphørKilde;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårJsonObjectMapper;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.bosatt.OpphørResultat;
import no.nav.ung.sak.behandlingslager.bosatt.OpphørResultatRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import java.time.LocalDate;
import java.util.Map;

@ApplicationScoped
public class OpphørTjeneste {

    private static final VilkårJsonObjectMapper VILKAR_JSON_OBJECT_MAPPER = new VilkårJsonObjectMapper();

    private OpphørResultatRepository opphørResultatRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    OpphørTjeneste() {
        // CDI
    }

    @Inject
    public OpphørTjeneste(OpphørResultatRepository opphørResultatRepository,
                          VilkårResultatRepository vilkårResultatRepository) {
        this.opphørResultatRepository = opphørResultatRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    public void utledOgLagreVilkår(long behandlingId, VilkårType vilkårType, LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        if (tidslinjeTilVurdering.isEmpty()) {
            return;
        }

        Map<LocalDate, OpphørResultat> opphørPerStp = opphørResultatRepository.hentAktiveForBehandlingSomMap(behandlingId, vilkårType);

        Vilkårene vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Forventer vilkårresultat for behandling " + behandlingId));

        var builder = Vilkårene.builderFraEksisterende(vilkårene);
        var vilkårBuilder = builder.hentBuilderFor(vilkårType);

        for (var segment : tidslinjeTilVurdering.toSegments()) {
            LocalDate stp = segment.getFom();
            OpphørResultat opphør = opphørPerStp.get(stp);

            if (opphør == null) {
                continue;
            }

            String regelInput = lagRegelInput(opphør);
            Avslagsårsak avslagsårsak = opphør.getOpphørÅrsak();
            LocalDate opphørDato = opphør.getOpphørDato();

            if (opphørDato.isBefore(stp)) {
                var periodeBuilder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fra(segment.getLocalDateInterval()))
                    .medRegelInput(regelInput)
                    .medUtfall(Utfall.IKKE_OPPFYLT)
                    .medAvslagsårsak(avslagsårsak);

                if (opphør.getKilde() == OpphørKilde.AUTOMATISK) {
                    periodeBuilder
                        .nullstillFritekstvurderinger()
                        .medManueltVurdert(false);
                }

                vilkårBuilder.leggTil(periodeBuilder);
            } else if (opphørDato.isAfter(segment.getTom())) {
                var periodeBuilder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fra(segment.getLocalDateInterval()))
                    .medRegelInput(regelInput)
                    .medUtfall(Utfall.OPPFYLT);

                vilkårBuilder.leggTil(periodeBuilder);
            } else {
                var oppfyltBuilder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fraOgMedTilOgMed(stp, opphørDato.minusDays(1)));
                oppfyltBuilder
                    .medRegelInput(regelInput)
                    .medUtfall(Utfall.OPPFYLT);

                if (opphør.getKilde() == OpphørKilde.AUTOMATISK) {
                    oppfyltBuilder
                        .nullstillFritekstvurderinger()
                        .medManueltVurdert(false);
                }
                vilkårBuilder.leggTil(oppfyltBuilder);

                var ikkeOppfyltBuilder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fraOgMedTilOgMed(opphørDato, segment.getTom()));
                ikkeOppfyltBuilder.medRegelInput(regelInput)
                    .medUtfall(Utfall.IKKE_OPPFYLT)
                    .medAvslagsårsak(avslagsårsak);

                if (opphør.getKilde() == OpphørKilde.AUTOMATISK) {
                    ikkeOppfyltBuilder
                        .nullstillFritekstvurderinger()
                        .medManueltVurdert(false);
                }
                vilkårBuilder.leggTil(ikkeOppfyltBuilder);
            }
        }

        builder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(behandlingId, builder.build());
    }

    public void deaktiverOppfylteOpphørsresultater(long behandlingId, VilkårType vilkårType, LocalDateTimeline<Boolean> oppfyltTidslinje) {
        if (oppfyltTidslinje.isEmpty()) {
            return;
        }

        Map<LocalDate, OpphørResultat> opphørPerStp = opphørResultatRepository.hentAktiveForBehandlingSomMap(behandlingId, vilkårType);

        for (var segment : oppfyltTidslinje.toSegments()) {
            for (var entry : opphørPerStp.entrySet()) {
                OpphørResultat opphør = entry.getValue();
                LocalDate opphørDato = opphør.getOpphørDato();
                if (opphørDato != null
                    && !opphørDato.isBefore(segment.getFom())
                    && !opphørDato.isAfter(segment.getTom())) {
                    opphør.deaktiver();
                }
            }
        }
    }

    static String lagRegelInput(OpphørResultat opphørResultat) {
        return VILKAR_JSON_OBJECT_MAPPER.writeValueAsString(new RegelInput(
            opphørResultat.getSkjæringstidspunkt(),
            opphørResultat.getOpphørDato(),
            opphørResultat.getOpphørÅrsak(),
            opphørResultat.getKilde(),
            opphørResultat.getBegrunnelse()));
    }

    private record RegelInput(
        LocalDate skjaeringstidspunkt,
        LocalDate opphorDato,
        Avslagsårsak opphorAarsak,
        OpphørKilde kilde,
        String begrunnelse) {
    }
}
