package no.nav.k9.sak.mottak.inntektsmelding;

import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.Dokumentmottaker;
import no.nav.k9.sak.mottak.repo.MottattDokument;

@ApplicationScoped
@FagsakYtelseTypeRef
@DokumentGruppeRef("INNTEKTKOMP_FRILANS") // TODO: Allokere ekte brevkode
public class DokumentmottakerSøknadOmsorgspenger implements Dokumentmottaker {

    @Inject
    DokumentmottakerSøknadOmsorgspenger() {
    }

    @Override
    public BehandlingÅrsakType getBehandlingÅrsakType() {
        return BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER;
    }

    @Override
    public void validerDokument(MottattDokument mottattDokument, FagsakYtelseType ytelseType) {
        throw new UnsupportedOperationException("Ikke implementert mottak av søknad for utbetaling av omsorgspenger");
    }

    @Override
    public void mottaDokument(Collection<MottattDokument> mottattDokument, Behandling behandling) {
        throw new UnsupportedOperationException("Ikke implementert mottak av søknad for utbetaling av omsorgspenger");
    }
}
