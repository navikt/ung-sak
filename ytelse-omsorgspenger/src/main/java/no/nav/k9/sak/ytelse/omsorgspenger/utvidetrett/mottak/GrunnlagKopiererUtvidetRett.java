package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.mottak;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_AO;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_KS;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_MA;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.sak.behandling.revurdering.GrunnlagKopierer;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;

@ApplicationScoped
@FagsakYtelseTypeRef(OMSORGSPENGER_KS)
@FagsakYtelseTypeRef(OMSORGSPENGER_MA)
@FagsakYtelseTypeRef(OMSORGSPENGER_AO)
public class GrunnlagKopiererUtvidetRett implements GrunnlagKopierer {

    private PersonopplysningRepository personopplysningRepository;
    private SøknadRepository søknadRepository;

    public GrunnlagKopiererUtvidetRett() {
        // for CDI proxy
    }

    @Inject
    public GrunnlagKopiererUtvidetRett(PersonopplysningRepository personopplysningRepository,
                                       SøknadRepository søknadRepository) {
        this.personopplysningRepository = personopplysningRepository;
        this.søknadRepository = søknadRepository;
    }

    @Override
    public void kopierGrunnlagVedManuellOpprettelse(Behandling original, Behandling ny) {
        Long originalBehandlingId = original.getId();
        Long nyBehandlingId = ny.getId();
        personopplysningRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        søknadRepository.kopierGrunnlagFraEksisterendeBehandling(original, ny); // kopierer også med søknad - kan være manuell revurdering er opprettet.
    }

    @Override
    public void kopierGrunnlagVedAutomatiskOpprettelse(Behandling original, Behandling ny) {
        // Samme kopiering som manuell opprettelse
        kopierGrunnlagVedManuellOpprettelse(original, ny);
    }

}
