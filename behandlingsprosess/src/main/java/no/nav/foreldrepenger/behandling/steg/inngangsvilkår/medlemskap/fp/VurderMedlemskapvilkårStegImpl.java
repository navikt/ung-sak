package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.medlemskap.fp;

import static java.util.Collections.singletonList;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.InngangsvilkårFellesTjeneste;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.InngangsvilkårStegImpl;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapsvilkårPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapsvilkårPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultat;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(kode = "VURDERMV")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderMedlemskapvilkårStegImpl extends InngangsvilkårStegImpl {

    private static List<VilkårType> STØTTEDE_VILKÅR = singletonList(
        VilkårType.MEDLEMSKAPSVILKÅRET
    );

    private MedlemskapVilkårPeriodeRepository medlemskapVilkårPeriodeRepository;

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    @Inject
    public VurderMedlemskapvilkårStegImpl(BehandlingRepositoryProvider repositoryProvider, InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste, SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        super(repositoryProvider, inngangsvilkårFellesTjeneste, BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR);
        this.medlemskapVilkårPeriodeRepository = repositoryProvider.getMedlemskapVilkårPeriodeRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    public List<VilkårType> vilkårHåndtertAvSteg() {
        return STØTTEDE_VILKÅR;
    }

    @Override
    protected void utførtRegler(BehandlingskontrollKontekst kontekst, Behandling behandling, RegelResultat regelResultat, DatoIntervallEntitet periode) {
        LocalDate skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()).getUtledetSkjæringstidspunkt();

        MedlemskapVilkårPeriodeGrunnlagEntitet.Builder grBuilder = medlemskapVilkårPeriodeRepository.hentBuilderFor(behandling);
        MedlemskapsvilkårPeriodeEntitet.Builder builder = grBuilder.getPeriodeBuilder();
        MedlemskapsvilkårPerioderEntitet.Builder periodeBuilder = builder.getBuilderForVurderingsdato(skjæringstidspunkt);
        final var utfall = regelResultat.getVilkårResultat().getVilkårene()
            .stream()
            .filter(v -> v.getVilkårType().equals(VilkårType.MEDLEMSKAPSVILKÅRET))
            .map(Vilkår::getPerioder).flatMap(Collection::stream).filter(it -> it.getPeriode().inkluderer(skjæringstidspunkt))
            .map(VilkårPeriode::getUtfall).findFirst().orElseThrow();
        periodeBuilder.medVilkårUtfall(utfall);
        builder.leggTil(periodeBuilder);
        grBuilder.medMedlemskapsvilkårPeriode(builder);
        medlemskapVilkårPeriodeRepository.lagreMedlemskapsvilkår(behandling, grBuilder);
    }
}
