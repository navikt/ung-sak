package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.kompletthet.KompletthetResultat;
import no.nav.k9.sak.kompletthet.Kompletthetsjekker;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;

@FagsakYtelseTypeRef("OMP_KS")
@FagsakYtelseTypeRef("OMP_MA")
@BehandlingTypeRef
@ApplicationScoped
public class UtvidetRettKompletthetSjekker implements Kompletthetsjekker {

    private static final Logger log = LoggerFactory.getLogger(UtvidetRettKompletthetSjekker.class);
    private DokumentVedleggHåndterer dokumentVedleggHåndterer;
    private SøknadRepository søknadRepository;

    private Period frist = Period.parse("P2W");

    UtvidetRettKompletthetSjekker() {
        // for proxy
    }

    @Inject
    public UtvidetRettKompletthetSjekker(DokumentVedleggHåndterer dokumentVedleggHåndterer, SøknadRepository søknadRepository) {
        this.dokumentVedleggHåndterer = dokumentVedleggHåndterer;
        this.søknadRepository = søknadRepository;
    }

    @Override
    public KompletthetResultat vurderSøknadMottattForTidlig(BehandlingReferanse ref) {
        return KompletthetResultat.oppfylt();
    }

    @Override
    public KompletthetResultat vurderForsendelseKomplett(BehandlingReferanse ref) {
        boolean harVedlegg = harMottattVedlegg(ref);
        if (harVedlegg) {
            return KompletthetResultat.oppfylt();
        } else {
            var søknad = søknadRepository.hentSøknad(ref.getBehandlingId());
            var fristTid = søknad.getMottattDato().plus(frist).atStartOfDay();

            if (fristTid.isBefore(LocalDateTime.now())) {
                log.warn("Frist for søknad mottatt {} utløpt {}, mangler vedlegg/dokumentasjon", søknad.getMottattDato(), fristTid);
            }

            return KompletthetResultat.ikkeOppfylt(fristTid, Venteårsak.AVV_DOK);
        }
    }

    private boolean harMottattVedlegg(BehandlingReferanse ref) {
        var søknad = søknadRepository.hentSøknadHvisEksisterer(ref.getBehandlingId()).orElseThrow(() -> new IllegalStateException("Mangler søknad for behandling"));
        boolean harVedlegg = dokumentVedleggHåndterer.harVedlegg(søknad.getJournalpostId());
        return harVedlegg;
    }

    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggForForsendelse(BehandlingReferanse ref) {
        return List.of();
    }

    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggSomIkkeKommer(BehandlingReferanse ref) {
        return List.of();
    }

    @Override
    public boolean erForsendelsesgrunnlagKomplett(BehandlingReferanse ref) {
        return harMottattVedlegg(ref);
    }

}