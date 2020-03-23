package no.nav.k9.sak.domene.medlem;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.medlem.impl.AvklarGyldigPeriode;
import no.nav.k9.sak.domene.medlem.impl.AvklarOmErBosatt;
import no.nav.k9.sak.domene.medlem.impl.AvklarOmSøkerOppholderSegINorge;
import no.nav.k9.sak.domene.medlem.impl.AvklaringFaktaMedlemskap;
import no.nav.k9.sak.domene.medlem.impl.MedlemResultat;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;

@ApplicationScoped
public class VurderMedlemskapTjeneste {

    private AvklarOmErBosatt avklarOmErBosatt;
    private AvklarGyldigPeriode avklarGyldigPeriode;
    private AvklarOmSøkerOppholderSegINorge avklarOmSøkerOppholderSegINorge;
    private AvklaringFaktaMedlemskap avklaringFaktaMedlemskap;
    private BehandlingRepository behandlingRepository;

    protected VurderMedlemskapTjeneste() {
        // CDI
    }

    @Inject
    public VurderMedlemskapTjeneste(BehandlingRepositoryProvider provider,
                                    MedlemskapPerioderTjeneste medlemskapPerioderTjeneste,
                                    PersonopplysningTjeneste personopplysningTjeneste,
                                    InntektArbeidYtelseTjeneste iayTjeneste) {
        this.behandlingRepository = provider.getBehandlingRepository();
        var medlemskapRepository = provider.getMedlemskapRepository();
        this.avklarOmErBosatt = new AvklarOmErBosatt(medlemskapRepository, medlemskapPerioderTjeneste, personopplysningTjeneste);
        this.avklarGyldigPeriode = new AvklarGyldigPeriode(medlemskapRepository, medlemskapPerioderTjeneste);
        this.avklarOmSøkerOppholderSegINorge = new AvklarOmSøkerOppholderSegINorge(provider, personopplysningTjeneste, iayTjeneste);
        this.avklaringFaktaMedlemskap = new AvklaringFaktaMedlemskap(provider, medlemskapPerioderTjeneste, personopplysningTjeneste, iayTjeneste);
    }

    /**
     *
     * @param ref behandlingreferanse
     * @param vurderingsdato hvilken dato vurderingstjenesten skal kjøre for
     * @return Liste med MedlemResultat
     */
    public Set<MedlemResultat> vurderMedlemskap(BehandlingReferanse ref, LocalDate vurderingsdato) {
        Long behandlingId = ref.getBehandlingId();
        Set<MedlemResultat> resultat = new HashSet<>();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        avklarOmErBosatt.utled(behandling, vurderingsdato).ifPresent(resultat::add);
        avklarGyldigPeriode.utled(behandlingId, vurderingsdato).ifPresent(resultat::add);
        avklarOmSøkerOppholderSegINorge.utled(ref, vurderingsdato).ifPresent(resultat::add);
        avklaringFaktaMedlemskap.utled(behandling, vurderingsdato).ifPresent(resultat::add);
        return resultat;
    }
}
