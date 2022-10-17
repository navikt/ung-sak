package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon.InstitusjonGodkjenningStatus.IKKE_GODKJENT;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon.InstitusjonGodkjenningStatus.MANGLER_VURDERING;

import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.GodkjentOpplæringsinstitusjonTjeneste;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;

@BehandlingStegRef(value = BehandlingStegType.VURDER_INSTITUSJON_VILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.OPPLÆRINGSPENGER)
@ApplicationScoped
public class VurderInstitusjonSteg implements BehandlingSteg {

    private BehandlingRepositoryProvider repositoryProvider;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private VurderInstitusjonTjeneste vurderInstitusjonTjeneste;

    VurderInstitusjonSteg() {
        // CDI
    }

    @Inject
    public VurderInstitusjonSteg(BehandlingRepositoryProvider repositoryProvider,
                                 @FagsakYtelseTypeRef(OPPLÆRINGSPENGER) @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                                 VurdertOpplæringRepository vurdertOpplæringRepository,
                                 GodkjentOpplæringsinstitusjonTjeneste godkjentOpplæringsinstitusjonTjeneste,
                                 UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.repositoryProvider = repositoryProvider;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.vurderInstitusjonTjeneste = new VurderInstitusjonTjeneste(perioderTilVurderingTjeneste, vurdertOpplæringRepository, godkjentOpplæringsinstitusjonTjeneste, uttakPerioderGrunnlagRepository);
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var tidslinjeTilVurderingMedInstitusjonsgodkjenning = vurderInstitusjonTjeneste.hentTidslinjeTilVurderingMedInstitusjonsGodkjenning(kontekst.getBehandlingId());

        var manglerVurderingTidslinje = tidslinjeTilVurderingMedInstitusjonsgodkjenning.filterValue(godkjenning -> Objects.equals(godkjenning, MANGLER_VURDERING));
        if (!manglerVurderingTidslinje.isEmpty()) {
            return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(
                AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.VURDER_INSTITUSJON)));
        }

        var ikkeGodkjentTidslinje = tidslinjeTilVurderingMedInstitusjonsgodkjenning.filterValue(godkjenning -> Objects.equals(godkjenning, IKKE_GODKJENT));
        lagreIkkeGodkjentVilkår(kontekst.getBehandlingId(), TidslinjeUtil.tilDatoIntervallEntiteter(ikkeGodkjentTidslinje));

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private void lagreIkkeGodkjentVilkår(Long behandlingsId, NavigableSet<DatoIntervallEntitet> perioder) {
        var vilkårene = vilkårResultatRepository.hent(behandlingsId);

        var vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårene)
            .medKantIKantVurderer(perioderTilVurderingTjeneste.getKantIKantVurderer());
        var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VilkårType.NØDVENDIG_OPPLÆRING);

        perioder.forEach(datoIntervallEntitet -> vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(datoIntervallEntitet)
            .medUtfall(Utfall.IKKE_OPPFYLT)
            .medAvslagsårsak(Avslagsårsak.IKKE_GODKJENT_INSTITUSJON)));

        vilkårResultatBuilder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(behandlingsId, vilkårResultatBuilder.build());
    }
}
