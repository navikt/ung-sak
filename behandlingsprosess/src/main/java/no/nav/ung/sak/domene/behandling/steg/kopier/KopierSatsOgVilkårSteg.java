package no.nav.ung.sak.domene.behandling.steg.kopier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.sak.behandling.revurdering.GrunnlagKopierer;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.KOPIER_SATS_OG_VILKÅR;
import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

@ApplicationScoped
@BehandlingStegRef(value = KOPIER_SATS_OG_VILKÅR)
@BehandlingTypeRef(BehandlingType.KONTROLLBEHANDLING)
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
public class KopierSatsOgVilkårSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private GrunnlagKopierer grunnlagKopierer;

    @Inject
    public KopierSatsOgVilkårSteg(BehandlingRepository behandlingRepository, @FagsakYtelseTypeRef(UNGDOMSYTELSE) GrunnlagKopierer grunnlagKopierer) {
        this.behandlingRepository = behandlingRepository;
        this.grunnlagKopierer = grunnlagKopierer;
    }

    public KopierSatsOgVilkårSteg() {
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        final var sisteYtelsesbehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(kontekst.getFagsakId()).orElseThrow();
        grunnlagKopierer.kopierGrunnlagVedAutomatiskOpprettelse(sisteYtelsesbehandling, behandlingRepository.hentBehandling(kontekst.getBehandlingId()));

        var behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        behandling.setBehandlingResultatType(sisteYtelsesbehandling.getBehandlingResultatType());
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
