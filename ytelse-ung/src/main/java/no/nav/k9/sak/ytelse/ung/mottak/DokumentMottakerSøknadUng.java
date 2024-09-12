package no.nav.k9.sak.ytelse.ung.mottak;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.UNG;

import java.util.Collection;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.Dokumentmottaker;


@ApplicationScoped
@FagsakYtelseTypeRef(UNG)
@DokumentGruppeRef(Brevkode.UNG_SOKNAD_KODE)
public class DokumentMottakerSøknadUng implements Dokumentmottaker {

    @Override
    public void lagreDokumentinnhold(Collection<MottattDokument> mottattDokument, Behandling behandling) {
        // DENNE GJØR INGENTING
    }

    @Override
    public BehandlingÅrsakType getBehandlingÅrsakType(Brevkode brevkode) {
        return BehandlingÅrsakType.RE_ANNET;
    }

}
