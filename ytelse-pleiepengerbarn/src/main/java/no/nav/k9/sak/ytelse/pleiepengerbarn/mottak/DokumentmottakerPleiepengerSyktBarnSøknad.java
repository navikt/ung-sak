package no.nav.k9.sak.ytelse.pleiepengerbarn.mottak;

import java.util.ArrayList;
import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.Dokumentmottaker;
import no.nav.k9.sak.mottak.dokumentmottak.SøknadParser;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn;
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarnValidator;

@ApplicationScoped
@FagsakYtelseTypeRef("PSB")
@DokumentGruppeRef(Brevkode.PLEIEPENGER_BARN_SOKNAD_KODE)
class DokumentmottakerPleiepengerSyktBarnSøknad implements Dokumentmottaker {

    private SøknadOversetter pleiepengerBarnSoknadOversetter;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private SøknadParser søknadParser;
    private SykdomsDokumentVedleggHåndterer sykdomsDokumentVedleggHåndterer;

    DokumentmottakerPleiepengerSyktBarnSøknad() {
        // for CDI proxy
    }

    @Inject
    DokumentmottakerPleiepengerSyktBarnSøknad(MottatteDokumentRepository mottatteDokumentRepository,
                                              SøknadParser søknadParser,
                                              SøknadOversetter pleiepengerBarnSoknadOversetter,
                                              SykdomsDokumentVedleggHåndterer sykdomsDokumentVedleggHåndterer) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.søknadParser = søknadParser;
        this.sykdomsDokumentVedleggHåndterer = sykdomsDokumentVedleggHåndterer;
        this.pleiepengerBarnSoknadOversetter = pleiepengerBarnSoknadOversetter;
    }

    @Override
    public void lagreDokumentinnhold(Collection<MottattDokument> dokumenter, Behandling behandling) {
        var behandlingId = behandling.getId();
        for (MottattDokument dokument : dokumenter) {
            Søknad søknad = søknadParser.parseSøknad(dokument);
            dokument.setBehandlingId(behandlingId);
            dokument.setInnsendingstidspunkt(søknad.getMottattDato().toLocalDateTime());
            mottatteDokumentRepository.lagre(dokument, DokumentStatus.BEHANDLER);
            // Søknadsinnhold som persisteres "lokalt" i k9-sak
            persister(søknad, behandling, dokument.getJournalpostId());
        }
        // Søknadsinnhold som persisteres eksternt (abakus)
        lagreOppgittOpptjeningFraSøknader(behandlingId, dokumenter);
    }

    /**
     * Lagrer inntektsmeldinger til abakus fra mottatt dokument.
     */
    private void lagreOppgittOpptjeningFraSøknader(Long behandlingId, Collection<MottattDokument> dokumenter) {
        // TODO: Dette må faktisk opprette en task som sender til abakus hvor det også flippes status til gyldig før prodsetting
        mottatteDokumentRepository.oppdaterStatus(new ArrayList<>(dokumenter), DokumentStatus.GYLDIG);
    }

    private void persister(Søknad søknad, Behandling behandling, JournalpostId journalpostId) {
        new PleiepengerSyktBarnValidator().forsikreValidert(søknad.getYtelse());

        pleiepengerBarnSoknadOversetter.persister(søknad, journalpostId, behandling);

        Boolean søknadenInneholderInfomasjonSomIkkeKanPunsjes = ((PleiepengerSyktBarn) søknad.getYtelse()).getInfoFraPunsj().getSøknadenInneholderInfomasjonSomIkkeKanPunsjes();

        sykdomsDokumentVedleggHåndterer.leggTilDokumenterSomSkalHåndteresVedlagtSøknaden(behandling,
            journalpostId,
            behandling.getFagsak().getPleietrengendeAktørId(),
            søknad.getMottattDato().toLocalDateTime(),
            søknadenInneholderInfomasjonSomIkkeKanPunsjes);
    }

    @Override
    public BehandlingÅrsakType getBehandlingÅrsakType() {
        return BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER;
    }
}
