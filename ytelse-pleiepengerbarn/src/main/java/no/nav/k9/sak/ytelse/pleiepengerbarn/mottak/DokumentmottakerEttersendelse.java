package no.nav.k9.sak.ytelse.pleiepengerbarn.mottak;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.ettersendelse.Ettersendelse;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.domene.behandling.steg.omsorgenfor.BrukerdialoginnsynTjeneste;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentValideringException;
import no.nav.k9.sak.mottak.dokumentmottak.Dokumentmottaker;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentmottakerFelles;
import no.nav.k9.søknad.JsonUtils;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE)
@DokumentGruppeRef(Brevkode.ETTERSENDELSE_PLEIEPENGER_BARN_KODE)
@DokumentGruppeRef(Brevkode.ETTERSENDELSE_PLEIEPENGER_LIVETS_SLUTTFASE_KODE)
public class DokumentmottakerEttersendelse implements Dokumentmottaker {

    private static final Logger log = LoggerFactory.getLogger(DokumentmottakerEttersendelse.class);

    private MottatteDokumentRepository mottatteDokumentRepository;
    private SykdomsDokumentVedleggHåndterer sykdomsDokumentVedleggHåndterer;
    private Instance<BrukerdialoginnsynTjeneste> brukerdialoginnsynServicer;
    private DokumentmottakerFelles dokumentMottakerFelles;
    private boolean ettersendelseRettTilK9Sak;

    DokumentmottakerEttersendelse() {
        // for CDI proxy
    }

    @Inject
    public DokumentmottakerEttersendelse(MottatteDokumentRepository mottatteDokumentRepository,
                                         SykdomsDokumentVedleggHåndterer sykdomsDokumentVedleggHåndterer,
                                         @Any Instance<BrukerdialoginnsynTjeneste> brukerdialoginnsynServicer,
                                         DokumentmottakerFelles dokumentMottakerFelles,
                                         @KonfigVerdi(value = "ETTERSENDELSE_RETT_TIL_K9SAK", defaultVerdi = "false") boolean ettersendelseRettTilK9Sak) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.sykdomsDokumentVedleggHåndterer = sykdomsDokumentVedleggHåndterer;
        this.brukerdialoginnsynServicer = brukerdialoginnsynServicer;
        this.dokumentMottakerFelles = dokumentMottakerFelles;
        this.ettersendelseRettTilK9Sak = ettersendelseRettTilK9Sak;
    }

    @Override
    public void lagreDokumentinnhold(Collection<MottattDokument> dokumenter, Behandling behandling) {
        if (!ettersendelseRettTilK9Sak) {
            throw new IllegalStateException("Funksjonaliteten er skrudd av");
        }

        var brukerdialoginnsynService = BrukerdialoginnsynTjeneste.finnTjeneste(brukerdialoginnsynServicer, behandling.getFagsakYtelseType());

        var sorterteDokumenter = sorterSøknadsdokumenter(dokumenter);
        log.info("Mottar digital ettersendelse, antall dokumenter: {}", sorterteDokumenter.size());
        for (MottattDokument dokument : sorterteDokumenter) {
            Ettersendelse ettersendelse = parseDokument(dokument);
            brukerdialoginnsynService.publiserDokumentHendelse(behandling, dokument);
            dokument.setBehandlingId(behandling.getId());
            dokument.setInnsendingstidspunkt(ettersendelse.getMottattDato().toLocalDateTime());
            mottatteDokumentRepository.lagre(dokument, DokumentStatus.GYLDIG);
            sykdomsDokumentVedleggHåndterer.leggTilDokumenterSomSkalHåndteresVedlagtSøknaden(
                behandling,
                dokument.getJournalpostId(),
                behandling.getFagsak().getPleietrengendeAktørId(),
                dokument.getMottattDato().atStartOfDay(),
                false,
                true);
            dokumentMottakerFelles.opprettHistorikkinnslagForVedlegg(behandling.getFagsakId(), dokument.getJournalpostId(), dokument.getType());
        }
    }

    private LinkedHashSet<MottattDokument> sorterSøknadsdokumenter(Collection<MottattDokument> dokumenter) {
        return dokumenter
            .stream()
            .sorted(Comparator.comparing(MottattDokument::getMottattTidspunkt))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Ettersendelse parseDokument(MottattDokument mottattDokument) {
        var payload = mottattDokument.getPayload();
        var jsonReader = JsonUtils.getObjectMapper().readerFor(Ettersendelse.class);
        try {
            return jsonReader.readValue(Objects.requireNonNull(payload, "mangler payload"));
        } catch (Exception e) {
            throw new DokumentValideringException("Parsefeil i ettersendelse", e);
        }
    }

    @Override
    public BehandlingÅrsakType getBehandlingÅrsakType(Brevkode brevkode) {
        return BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER;
    }
}
