package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.kronisksyk;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.kompletthet.KompletthetResultat;
import no.nav.k9.sak.kompletthet.Kompletthetsjekker;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess.DokumentVedleggHåndterer;

@FagsakYtelseTypeRef("OMP_KS")
@BehandlingTypeRef
@ApplicationScoped
public class KroniskSykKompletthetSjekker implements Kompletthetsjekker {

    private static final Logger log = LoggerFactory.getLogger(KroniskSykKompletthetSjekker.class);
    private DokumentVedleggHåndterer dokumentVedleggHåndterer;
    private SøknadRepository søknadRepository;

    private Period frist = Period.parse("P2W");
    private HistorikkRepository historikkRepository;

    KroniskSykKompletthetSjekker() {
        // for proxy
    }

    @Inject
    public KroniskSykKompletthetSjekker(DokumentVedleggHåndterer dokumentVedleggHåndterer,
                                         HistorikkRepository historikkRepository,
                                         SøknadRepository søknadRepository) {
        this.dokumentVedleggHåndterer = dokumentVedleggHåndterer;
        this.historikkRepository = historikkRepository;
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
                /*
                 * TODO: klarer foreløpig ikke oppdage om vedlegg er mottatt på annen journalpost, ikke journalført, eller ført på annen sak.
                 * Overlater derfor til saksbehandler å vurdere som del av utvidet rett.
                 */
                log.warn("Frist for vedlegg til søknad mottatt {} utløpt {}, mangler vedlegg/dokumentasjon.", søknad.getMottattDato(), fristTid);
                var historikk = new Historikkinnslag();
                historikk.setBehandlingId(ref.getBehandlingId());
                historikk.setType(HistorikkinnslagType.BEH_GJEN);
                historikk.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
                var historiebygger = new HistorikkInnslagTekstBuilder()
                    .medHendelse(HistorikkinnslagType.BEH_GJEN)
                    .medSkjermlenke(SkjermlenkeType.PUNKT_FOR_UTVIDETRETT)
                    .medBegrunnelse("Frist for kompletthetsjekk utløpt - forventede vedlegg må manuelt vurderes som del av vilkår for utvidet rett");
                historiebygger.build(historikk);
                historikkRepository.lagre(historikk);
                return KompletthetResultat.oppfylt(); // kjører videre et historikkinnslag i stedet.
            } else {
                return KompletthetResultat.ikkeOppfylt(fristTid, Venteårsak.AVV_DOK);
            }
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
