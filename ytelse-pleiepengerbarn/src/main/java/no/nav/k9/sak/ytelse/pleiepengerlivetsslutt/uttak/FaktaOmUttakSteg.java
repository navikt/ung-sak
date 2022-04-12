package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.uttak;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.KONTROLLER_FAKTA_UTTAK;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;

import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.PerioderMedSykdomInnvilgetUtleder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.ArbeidBrukerBurdeSøktOmUtleder;

@ApplicationScoped
@BehandlingStegRef(value = KONTROLLER_FAKTA_UTTAK)
@BehandlingTypeRef
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
public class FaktaOmUttakSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private ArbeidBrukerBurdeSøktOmUtleder arbeidBrukerBurdeSøktOmUtleder;
    private PerioderMedSykdomInnvilgetUtleder perioderMedSykdomInnvilgetUtleder;

    protected FaktaOmUttakSteg() {
        // for proxy
    }

    @Inject
    public FaktaOmUttakSteg(BehandlingRepository behandlingRepository,
                            ArbeidBrukerBurdeSøktOmUtleder arbeidBrukerBurdeSøktOmUtleder,
                            PerioderMedSykdomInnvilgetUtleder perioderMedSykdomInnvilgetUtleder) {
        this.behandlingRepository = behandlingRepository;
        this.arbeidBrukerBurdeSøktOmUtleder = arbeidBrukerBurdeSøktOmUtleder;
        this.perioderMedSykdomInnvilgetUtleder = perioderMedSykdomInnvilgetUtleder;
    }

    @SuppressWarnings("unused")
    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        // TODO PLS: Sjekke mot uttak om kvote er oversteget

        final Long behandlingId = kontekst.getBehandlingId();
        final var behandling = behandlingRepository.hentBehandling(behandlingId);
        var referanse = BehandlingReferanse.fra(behandling);

        // TODO PLS: Avklare om dette er funksjonelt ønskelig
        var innvilgedePerioderTilVurdering = perioderMedSykdomInnvilgetUtleder.utledInnvilgedePerioderTilVurdering(referanse);
        var manglendeAktiviteter = arbeidBrukerBurdeSøktOmUtleder.utledMangler(referanse);
        if (manglendeAktiviteter.entrySet().stream().anyMatch(it -> !it.getValue().isEmpty()) && harNoenGodkjentPerioderMedSykdom(innvilgedePerioderTilVurdering)) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.MANGLER_AKTIVITETER));
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean harNoenGodkjentPerioderMedSykdom(Set<VilkårPeriode> innvilgetePerioder) {
        return !innvilgetePerioder.isEmpty();
    }

}
