package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bistandsvilkår;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandlingskontroll.BehandlingÅrsakTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AktivitetspengerInngangsvilkårResultatGrunnlag;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaringForeslått;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlag;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.ung.sak.etterlysning.VilkårsavklaringEtterlysningTjeneste;
import no.nav.ung.sak.inngangsvilkår.avklaring.Vilkårsavklaring;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.del1.InngangsvilkårVurderingTjeneste;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bistandsvariant av {@code BostedAvklaringTjeneste}. Bruker fellesmodellen fra fase 0
 * ({@link VilkårsavklaringGrunnlagRepository} og {@link VilkårsavklaringEtterlysningTjeneste}) i stedet for et
 * vilkårsspesifikt grunnlagsrepository. Bevisst uten delt baseklasse med bosted — komposisjon, ikke arv.
 */
@ApplicationScoped
@BehandlingÅrsakTypeRef(BehandlingÅrsakType.ENDRET_BISTANDSBEHOV)
public class BistandAvklaringTjeneste implements VilkårsavklaringTjeneste {

    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;
    private VilkårsavklaringEtterlysningTjeneste vilkårsavklaringEtterlysningTjeneste;
    private InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;

    public BistandAvklaringTjeneste() {
        // for CDI proxy
    }

    @Inject
    public BistandAvklaringTjeneste(VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository,
                                    VilkårsavklaringEtterlysningTjeneste vilkårsavklaringEtterlysningTjeneste,
                                    InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste,
                                    InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository) {
        this.vilkårsavklaringGrunnlagRepository = vilkårsavklaringGrunnlagRepository;
        this.vilkårsavklaringEtterlysningTjeneste = vilkårsavklaringEtterlysningTjeneste;
        this.inngangsvilkårVurderingTjeneste = inngangsvilkårVurderingTjeneste;
        this.inngangsvilkårVurderingRepository = inngangsvilkårVurderingRepository;
    }

    public List<VilkårPeriodeAvklaring> hentForeslåtteAvklaringer(long behandlingId) {
        return vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId, VilkårType.BISTANDSVILKÅR)
            .map(g -> List.copyOf(g.getForeslåtteAvklaringer()))
            .orElse(List.of());
    }

    public Set<VilkårPeriodeAvklaring> lagreForeslåttAvklaringOgSettVilkårIkkeVurdert(List<BistandAvklaringInnhold> nyeAvklaringer, String vurdertAv, LocalDateTime vurdertTidspunkt, long behandlingId) {
        Set<VilkårPeriodeAvklaringForeslått> nyePeriodeAvklaringer = nyeAvklaringer.stream()
            .map(it -> BistandAvklaringDataMapper.mapTilVilkårPeriodeAvklaring(it, vurdertAv, vurdertTidspunkt))
            .collect(Collectors.toSet());
        return vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandlingId, VilkårType.BISTANDSVILKÅR, nyePeriodeAvklaringer);
    }

    public void oppdaterEtterlysninger(Behandling behandling,
                                       Collection<VilkårPeriodeAvklaring> tidligereForeslåtteAvklaringer,
                                       Collection<VilkårPeriodeAvklaring> nyeForeslåtteAvklaringer) {
        vilkårsavklaringEtterlysningTjeneste.oppdaterEtterlysninger(behandling, EtterlysningType.UTTALELSE_BISTAND, tidligereForeslåtteAvklaringer, nyeForeslåtteAvklaringer);
    }

    // Hvis saksbehandler endrer perioden det avklares for etter at vilkårsvurdering er utført, gjelder ikke lenger vurderingen og den delen som ikke overlapper med ny avklaring må gjenopprettes fra forrige behandling.
    // Vilkårsperioden som avklaringen gjelder for settes til ikke vurdert, slik at den kan vurderes på nytt (automatisk eller i aksjonspunkt)
    public void gjenopprettTidligereVilkårsvurderingVedBehovOgSettAvklartPeriodeTilIkkeVurdert(AksjonspunktOppdaterParameter param,
                                                                                              List<VilkårPeriodeAvklaring> tidligereForeslåtteAvklaringer,
                                                                                              List<BistandAvklaringInnhold> nyeAvklaringer) {
        var tidligereTidslinje = TidslinjeUtil.tilTidslinjeKomprimert(tidligereForeslåtteAvklaringer.stream().map(VilkårPeriodeAvklaring::getPeriode).toList());
        var nyTidslinje = TidslinjeUtil.tilTidslinjeKomprimert(nyeAvklaringer.stream().map(BistandAvklaringInnhold::periode).toList());
        var tidslinjeSomIkkeHåndteresAvNyAvklaring = tidligereTidslinje.disjoint(nyTidslinje);

        inngangsvilkårVurderingTjeneste.gjenopprettForrigeVurderingForPerioderIkkeVurdertBistand(param.getBehandlingId(), param.getVilkårResultatBuilder(), tidslinjeSomIkkeHåndteresAvNyAvklaring);
        // Bistandsgrunnlaget kan mangle helt (ingen vurderinger lagret ennå), og settBistandsvilkårResultat kaster da.
        // Bosted har ikke samme behov, siden bostedsgrunnlaget alltid opprettes ved søknadsmottak.
        if (harBistandsvurderinger(param.getBehandlingId())) {
            inngangsvilkårVurderingTjeneste.settBistandsvilkårResultat(param.getBehandlingId(), param.getVilkårResultatBuilder());
        }

        var perioderSomSkalVurderesPåNytt = TidslinjeUtil.tilDatoIntervallEntiteter(nyTidslinje);
        inngangsvilkårVurderingTjeneste.settVilkårResultatIkkeVurdertForPeriode(param.getVilkårResultatBuilder(), VilkårType.BISTANDSVILKÅR, perioderSomSkalVurderesPåNytt);
    }

    private boolean harBistandsvurderinger(Long behandlingId) {
        return inngangsvilkårVurderingRepository.hentEksisterendeGrunnlag(behandlingId)
            .flatMap(AktivitetspengerInngangsvilkårResultatGrunnlag::getBistandsvilkårResultatHolder)
            .isPresent();
    }

    @Override
    public void ferdigstillForeslåtteAvklaringer(long behandlingId) {
        vilkårsavklaringGrunnlagRepository.ferdigstillForeslåtteAvklaringer(behandlingId, VilkårType.BISTANDSVILKÅR);
    }

    @Override
    public void settVilkårsperioderTilIkkeVurdertForForeslåtteAvklaringer(long behandlingId) {
        var perioderTidligereVurdertEtterAvklaring = hentForeslåtteAvklaringer(behandlingId).stream().map(VilkårPeriodeAvklaring::getPeriode).toList();
        inngangsvilkårVurderingTjeneste.settVilkårResultatIkkeVurdertForPeriode(behandlingId, VilkårType.BISTANDSVILKÅR, perioderTidligereVurdertEtterAvklaring);
    }

    @Override
    public Optional<Vilkårsavklaring> hentSenesteAvklaringForBehandling(long behandlingId) {
        return vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId, VilkårType.BISTANDSVILKÅR)
            .map(VilkårsavklaringGrunnlag::getForeslåtteAvklaringer)
            .orElse(Set.of())
            .stream()
            .max(Comparator.comparing(VilkårPeriodeAvklaring::getVurdertTidspunkt)
                .thenComparing(avklaring -> avklaring.getPeriode().getFomDato()))
            .map(avklaring -> new Vilkårsavklaring(avklaring.getAvklaringtype(), avklaring.getPeriode(), null, null));
    }
}
