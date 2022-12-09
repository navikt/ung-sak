package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.endring;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.k9.sak.domene.registerinnhenting.impl.behandlingårsak.BehandlingÅrsakUtleder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.ErEndringPåEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlagTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlagsdata;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.EndringUnntakEtablertTilsynTjeneste;

@ApplicationScoped
@GrunnlagRef(MedisinskGrunnlagsdata.class)
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
class BehandlingÅrsakUtlederSykdomGrunnlag implements BehandlingÅrsakUtleder {

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private MedisinskGrunnlagRepository medisinskGrunnlagRepository;
    private MedisinskGrunnlagTjeneste medisinskGrunnlagTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private ErEndringPåEtablertTilsynTjeneste erEndringPåEtablertTilsynTjeneste;
    private EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;

    BehandlingÅrsakUtlederSykdomGrunnlag() {
        // CDI
    }

    @Inject
    BehandlingÅrsakUtlederSykdomGrunnlag(BehandlingRepository behandlingRepository,
                                         BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                         MedisinskGrunnlagRepository medisinskGrunnlagRepository,
                                         MedisinskGrunnlagTjeneste medisinskGrunnlagTjeneste,
                                         VilkårResultatRepository vilkårResultatRepository,
                                         ErEndringPåEtablertTilsynTjeneste erEndringPåEtablertTilsynTjeneste,
                                         EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste,
                                         @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.medisinskGrunnlagRepository = medisinskGrunnlagRepository;
        this.medisinskGrunnlagTjeneste = medisinskGrunnlagTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.erEndringPåEtablertTilsynTjeneste = erEndringPåEtablertTilsynTjeneste;
        this.endringUnntakEtablertTilsynTjeneste = endringUnntakEtablertTilsynTjeneste;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
    }

    @Override
    public Set<BehandlingÅrsakType> utledBehandlingÅrsaker(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {

        var årsaker = new HashSet<>(Set.of(BehandlingÅrsakType.RE_ENDRING_FRA_ANNEN_OMSORGSPERSON));

        var harEndringerForSykdom = harEndringerForSykdom(ref);
        var harEndringerForEtablertTilsyn = harEndringerForEtablertTilsyn(ref);
        var harEndringerForNattevåkOgBeredskap = harEndringerForNattevåkOgBeredskap(ref);

        if (harEndringerForSykdom && harEndringerForEtablertTilsyn && harEndringerForNattevåkOgBeredskap) {
            årsaker.add(BehandlingÅrsakType.RE_SYKDOM_ETABLERT_TILSYN_NATTVÅK_ENDRING_FRA_ANNEN_OMSORGSPERSON);
        } else if (harEndringerForEtablertTilsyn && harEndringerForSykdom) {
            årsaker.add(BehandlingÅrsakType.RE_SYKDOM_ETABLERT_TILSYN_ENDRING_FRA_ANNEN_OMSORGSPERSON);
        } else if (harEndringerForNattevåkOgBeredskap && harEndringerForSykdom) {
            årsaker.add(BehandlingÅrsakType.RE_SYKDOM_NATTEVÅK_ENDRING_FRA_ANNEN_OMSORGSPERSON);
        } else if (harEndringerForNattevåkOgBeredskap && harEndringerForEtablertTilsyn) {
            årsaker.add(BehandlingÅrsakType.RE_NATTEVÅKBEREDSKAP_ETABLERT_TILSYN_ENDRING_FRA_ANNEN_OMSORGSPERSON);
        } else if (harEndringerForSykdom) {
            årsaker.add(BehandlingÅrsakType.RE_SYKDOM_ENDRING_FRA_ANNEN_OMSORGSPERSON);
        } else if (harEndringerForEtablertTilsyn) {
            årsaker.add(BehandlingÅrsakType.RE_ETABLERT_TILSYN_ENDRING_FRA_ANNEN_OMSORGSPERSON);
        } else if (harEndringerForNattevåkOgBeredskap) {
            årsaker.add(BehandlingÅrsakType.RE_NATTEVÅKBEREDSKAP_ENDRING_FRA_ANNEN_OMSORGSPERSON);
        }

        return årsaker;
    }

    private boolean harEndringerForNattevåkOgBeredskap(BehandlingReferanse ref) {
        return endringUnntakEtablertTilsynTjeneste.harEndringerSidenBehandling(ref.getBehandlingId(), ref.getPleietrengendeAktørId()) && skalGiÅrsak(ref, BehandlingStegType.KONTROLLER_FAKTA_UTTAK);
    }

    private boolean harEndringerForEtablertTilsyn(BehandlingReferanse referanse) {
        return erEndringPåEtablertTilsynTjeneste.erEndringerSidenBehandling(referanse) && skalGiÅrsak(referanse, BehandlingStegType.VURDER_UTTAK_V2);
    }

    private boolean harEndringerForSykdom(BehandlingReferanse ref) {
        var sykdomGrunnlag = medisinskGrunnlagRepository.hentGrunnlagForBehandling(ref.getBehandlingUuid())
            .map(MedisinskGrunnlag::getGrunnlagsdata);

        List<DatoIntervallEntitet> nyeVurderingsperioder = utledVurderingsperiode(ref);
        var utledGrunnlag = medisinskGrunnlagTjeneste.utledGrunnlagMedManglendeOmsorgFjernet(ref.getSaksnummer(), ref.getBehandlingUuid(), ref.getBehandlingId(), ref.getPleietrengendeAktørId(), nyeVurderingsperioder);
        var sykdomGrunnlagSammenlikningsresultat = medisinskGrunnlagTjeneste.sammenlignGrunnlag(sykdomGrunnlag, utledGrunnlag);

        return !sykdomGrunnlagSammenlikningsresultat.getDiffPerioder().isEmpty() && skalGiÅrsak(ref, BehandlingStegType.VURDER_MEDISINSKE_VILKÅR);
    }

    private boolean skalGiÅrsak(BehandlingReferanse ref, BehandlingStegType stegType) {
        if (BehandlingStatus.UTREDES.equals(ref.getBehandlingStatus()) && behandlingskontrollTjeneste.erIStegEllerSenereSteg(ref.getBehandlingId(), stegType)) {
            return true;
        }
        if (BehandlingStatus.UTREDES.equals(ref.getBehandlingStatus()) && Objects.equals(behandlingRepository.hentBehandling(ref.getBehandlingId()).getAktivtBehandlingSteg(), stegType)) {
            return true;
        }
        return BehandlingStatus.OPPRETTET.equals(ref.getBehandlingStatus());
    }

    private List<DatoIntervallEntitet> utledVurderingsperiode(BehandlingReferanse ref) {
        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(ref.getBehandlingId());
        if (vilkårene.isEmpty()) {
            return List.of();
        }

        VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, ref.getFagsakYtelseType(), ref.getBehandlingType());
        return perioderTilVurderingTjeneste.definerendeVilkår()
            .stream()
            .map(vilkårType -> vilkårene.get().getVilkår(vilkårType)
                .map(Vilkår::getPerioder))
            .flatMap(Optional::stream)
            .flatMap(Collection::stream)
            .map(VilkårPeriode::getPeriode)
            .toList();
    }
}
