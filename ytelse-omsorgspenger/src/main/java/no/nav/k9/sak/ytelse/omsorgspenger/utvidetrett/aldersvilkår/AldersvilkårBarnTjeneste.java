package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aldersvilkår;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.inngangsvilkår.VilkårUtfallOversetter;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aldersvilkår.regelmodell.AldersvilkårBarnVilkår;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aldersvilkår.regelmodell.AldersvilkårBarnVilkårGrunnlag;

@Dependent
public class AldersvilkårBarnTjeneste {

    private VilkårUtfallOversetter utfallOversetter = new VilkårUtfallOversetter();
    private BehandlingRepository behandlingRepository;
    private BasisPersonopplysningTjeneste personopplysningTjeneste;

    @Inject
    public AldersvilkårBarnTjeneste(BehandlingRepository behandlingRepository, BasisPersonopplysningTjeneste personopplysningTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    public List<AldersvilkårBarnVilkårGrunnlag> oversettSystemdataTilRegelModellGrunnlag(Long behandlingId, Vilkår vilkår) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        BehandlingReferanse behandlingReferanse = BehandlingReferanse.fra(behandling);

        List<AldersvilkårBarnVilkårGrunnlag> grunnlagene = new ArrayList<>();
        for (VilkårPeriode vilkårPeriode : vilkår.getPerioder()) {
            DatoIntervallEntitet periode = vilkårPeriode.getPeriode();
            List<LocalDate> barnasFødselsdatoer = hentFødselsdatoForAktuelleBarn(behandlingReferanse, behandling, periode);
            grunnlagene.add(new AldersvilkårBarnVilkårGrunnlag(barnasFødselsdatoer, behandling.getFagsakYtelseType(), periode));
        }
        return grunnlagene;
    }

    private List<LocalDate> hentFødselsdatoForAktuelleBarn(BehandlingReferanse behandlingReferanse, Behandling behandling, DatoIntervallEntitet periode) {
        PersonopplysningerAggregat aggregat = personopplysningTjeneste.hentPersonopplysninger(behandlingReferanse, periode.getFomDato());
        return switch (behandlingReferanse.getFagsakYtelseType()) {
            case OMSORGSPENGER_MA -> aggregat.getBarna().stream().map(PersonopplysningEntitet::getFødselsdato).toList();
            case OMSORGSPENGER_KS, OMSORGSPENGER_AO ->
                List.of(aggregat.getPersonopplysning(behandling.getFagsak().getPleietrengendeAktørId()).getFødselsdato());
            default ->
                throw new IllegalArgumentException("Ikke-støttet ytelse-type: " + behandlingReferanse.getFagsakYtelseType());
        };

    }

    public VilkårData vurder(AldersvilkårBarnVilkårGrunnlag grunnlag) {
        var evaluation = new AldersvilkårBarnVilkår().evaluer(grunnlag);
        return utfallOversetter.oversett(VilkårType.ALDERSVILKÅR_BARN, evaluation, grunnlag, grunnlag.getVilkårsperiode());
    }
}
