package no.nav.k9.sak.behandling.revurdering;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktKontrollRepository;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;

@ApplicationScoped
@FagsakYtelseTypeRef("FRISINN")
public class GrunnlagKopiererFrisinn implements GrunnlagKopierer {

    private PersonopplysningRepository personopplysningRepository;
    private MedlemskapRepository medlemskapRepository;
    private UttakRepository uttakRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private AksjonspunktKontrollRepository aksjonspunktKontrollRepository;

    public GrunnlagKopiererFrisinn() {
        // for CDI proxy
    }

    @Inject
    public GrunnlagKopiererFrisinn(BehandlingRepositoryProvider repositoryProvider,
                                   UttakRepository uttakRepository, InntektArbeidYtelseTjeneste iayTjeneste, AksjonspunktKontrollRepository aksjonspunktKontrollRepository) {
        this.uttakRepository = uttakRepository;
        this.iayTjeneste = iayTjeneste;
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.aksjonspunktKontrollRepository = aksjonspunktKontrollRepository;
    }


    @Override
    public void kopierAlleGrunnlagFraTidligereBehandling(Behandling original, Behandling ny) {
        Long originalBehandlingId = original.getId();
        Long nyBehandlingId = ny.getId();
        personopplysningRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        medlemskapRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        uttakRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);

        // gjør til slutt, innebærer kall til abakus
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
    }

    @Override
    public void opprettAksjonspunktForSaksbehandlerOverstyring(Behandling revurdering) {
        aksjonspunktKontrollRepository.leggTilAksjonspunkt(revurdering, AksjonspunktDefinisjon.OVERSTYRING_FRISINN_OPPGITT_OPPTJENING);
    }
}
