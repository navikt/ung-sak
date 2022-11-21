package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått.OpplæringGodkjenningStatus.GODKJENT;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått.OpplæringGodkjenningStatus.IKKE_GODKJENT;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått.OpplæringGodkjenningStatus.IKKE_GODKJENT_INSTITUSJON;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått.OpplæringGodkjenningStatus.IKKE_GODKJENT_REISETID;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått.OpplæringGodkjenningStatus.IKKE_GODKJENT_SYKDOMSVILKÅR;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått.OpplæringGodkjenningStatus.MANGLER_VURDERING;

import java.util.Objects;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.Aksjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;

@Dependent
public class GjennomgåttOpplæringTjeneste {

    private final VilkårResultatRepository vilkårResultatRepository;
    private final VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private final UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;
    private final VurdertOpplæringRepository vurdertOpplæringRepository;
    private final GjennomgåttOpplæringTidslinjeUtleder tidslinjeUtleder;

    @Inject
    public GjennomgåttOpplæringTjeneste(VilkårResultatRepository vilkårResultatRepository,
                                        @FagsakYtelseTypeRef(OPPLÆRINGSPENGER) VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                                        UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository,
                                        VurdertOpplæringRepository vurdertOpplæringRepository) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
        this.vurdertOpplæringRepository = vurdertOpplæringRepository;
        this.tidslinjeUtleder = new GjennomgåttOpplæringTidslinjeUtleder();
    }

    public Aksjon vurder(BehandlingReferanse referanse) {

        var vilkårene = vilkårResultatRepository.hent(referanse.getBehandlingId());

        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(referanse.getBehandlingId(), VilkårType.GJENNOMGÅ_OPPLÆRING);
        var tidslinjeTilVurdering = TidslinjeUtil.tilTidslinjeKomprimert(perioderTilVurdering);

        var uttaksPerioderGrunnlag = uttakPerioderGrunnlagRepository.hentGrunnlag(referanse.getBehandlingId()).orElseThrow();
        var perioderFraSøknad = uttaksPerioderGrunnlag.getRelevantSøknadsperioder().getPerioderFraSøknadene();

        var vurdertOpplæringGrunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(referanse.getBehandlingId()).orElse(null);

        var tidslinje = tidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, tidslinjeTilVurdering);

        if (tidslinje.filterValue(value -> Objects.equals(value, MANGLER_VURDERING)).stream().findFirst().isPresent()) {
            return Aksjon.TRENGER_AVKLARING;
        }

        lagreVilkårsResultat(referanse, vilkårene, tidslinje);

        return Aksjon.FORTSETT;
    }

    private void lagreVilkårsResultat(BehandlingReferanse referanse, Vilkårene vilkårene, LocalDateTimeline<OpplæringGodkjenningStatus> tidslinje) {
        var vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårene)
            .medKantIKantVurderer(perioderTilVurderingTjeneste.getKantIKantVurderer());
        var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VilkårType.GJENNOMGÅ_OPPLÆRING);

        leggTilVilkårsresultatTidligereVilkår(vilkårBuilder, tidslinje);

        leggTilVilkårsresultatgjennomgåttOpplæring(vilkårBuilder, tidslinje);

        leggTilVilkårsresultatReisetid(vilkårBuilder, tidslinje);

        vilkårResultatBuilder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(referanse.getBehandlingId(), vilkårResultatBuilder.build());
    }

    private void leggTilVilkårsresultatTidligereVilkår(VilkårBuilder vilkårBuilder, LocalDateTimeline<OpplæringGodkjenningStatus> tidslinje) {
        var tidslinjeUtenGodkjentInstitusjon = tidslinje.filterValue(value -> Objects.equals(value, IKKE_GODKJENT_INSTITUSJON));
        leggTilVilkårResultat(vilkårBuilder, tidslinjeUtenGodkjentInstitusjon, Utfall.IKKE_OPPFYLT, Avslagsårsak.IKKE_GODKJENT_INSTITUSJON);

        var tidslinjeUtenSykdomsvilkår = tidslinje.filterValue(value -> Objects.equals(value, IKKE_GODKJENT_SYKDOMSVILKÅR));
        leggTilVilkårResultat(vilkårBuilder, tidslinjeUtenSykdomsvilkår, Utfall.IKKE_OPPFYLT, Avslagsårsak.IKKE_DOKUMENTERT_SYKDOM_SKADE_ELLER_LYTE); // TODO: Endre til noe mer fornuftig
    }

    private void leggTilVilkårsresultatgjennomgåttOpplæring(VilkårBuilder vilkårBuilder, LocalDateTimeline<OpplæringGodkjenningStatus> tidslinje) {
        var godkjentTidslinje = tidslinje.filterValue(value -> Objects.equals(value, GODKJENT));
        var ikkeGodkjentTidslinje = tidslinje.filterValue(value -> Objects.equals(value, IKKE_GODKJENT));

        leggTilVilkårResultat(vilkårBuilder, godkjentTidslinje, Utfall.OPPFYLT, Avslagsårsak.UDEFINERT);
        leggTilVilkårResultat(vilkårBuilder, ikkeGodkjentTidslinje, Utfall.IKKE_OPPFYLT, Avslagsårsak.IKKE_GJENNOMGÅTT_OPPLÆRING);
    }

    private void leggTilVilkårsresultatReisetid(VilkårBuilder vilkårBuilder, LocalDateTimeline<OpplæringGodkjenningStatus> tidslinje) {
        var ikkeGodkjentReisetidTidslinje = tidslinje.filterValue(value -> Objects.equals(value, IKKE_GODKJENT_REISETID));

        leggTilVilkårResultat(vilkårBuilder, ikkeGodkjentReisetidTidslinje, Utfall.IKKE_OPPFYLT, Avslagsårsak.IKKE_GODKJENT_REISETID);
    }

    private static void leggTilVilkårResultat(VilkårBuilder vilkårBuilder, LocalDateTimeline<?> tidslinje, Utfall utfall, Avslagsårsak avslagsårsak) {
        TidslinjeUtil.tilDatoIntervallEntiteter(tidslinje)
            .forEach(datoIntervallEntitet -> vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(datoIntervallEntitet)
                .medUtfall(utfall)
                .medAvslagsårsak(avslagsårsak)));
    }
}
