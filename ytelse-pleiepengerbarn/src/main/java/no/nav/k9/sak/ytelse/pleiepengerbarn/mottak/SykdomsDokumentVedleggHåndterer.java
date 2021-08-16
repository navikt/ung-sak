package no.nav.k9.sak.ytelse.pleiepengerbarn.mottak;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.felles.integrasjon.saf.Datotype;
import no.nav.k9.felles.integrasjon.saf.DokumentInfo;
import no.nav.k9.felles.integrasjon.saf.DokumentInfoResponseProjection;
import no.nav.k9.felles.integrasjon.saf.DokumentvariantResponseProjection;
import no.nav.k9.felles.integrasjon.saf.Journalpost;
import no.nav.k9.felles.integrasjon.saf.JournalpostQueryRequest;
import no.nav.k9.felles.integrasjon.saf.JournalpostResponseProjection;
import no.nav.k9.felles.integrasjon.saf.Journalposttype;
import no.nav.k9.felles.integrasjon.saf.Kanal;
import no.nav.k9.felles.integrasjon.saf.LogiskVedleggResponseProjection;
import no.nav.k9.felles.integrasjon.saf.RelevantDatoResponseProjection;
import no.nav.k9.felles.integrasjon.saf.SafTjeneste;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.kontrakt.sykdom.dokument.SykdomDokumentType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokumentInformasjon;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokumentRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingRepository;

@Dependent
public class SykdomsDokumentVedleggHåndterer {

    private static final Logger log = LoggerFactory.getLogger(SykdomsDokumentVedleggHåndterer.class);

    private SykdomDokumentRepository sykdomDokumentRepository;
    private SykdomVurderingRepository sykdomVurderingRepository;
    private SafTjeneste safTjeneste;

    @Inject
    public SykdomsDokumentVedleggHåndterer(SykdomDokumentRepository sykdomDokumentRepository, SykdomVurderingRepository sykdomVurderingRepository, SafTjeneste safTjeneste) {
        this.safTjeneste = safTjeneste;
        this.sykdomDokumentRepository = sykdomDokumentRepository;
        this.sykdomVurderingRepository = sykdomVurderingRepository;
    }

    void leggTilDokumenterSomSkalHåndteresVedlagtSøknaden(Behandling behandling, JournalpostId journalpostId, AktørId pleietrengendeAktørId, LocalDateTime mottattidspunkt, boolean harInfoSomIkkeKanPunsjes, boolean harMedisinskeOpplysninger) {
        var query = new JournalpostQueryRequest();
        query.setJournalpostId(journalpostId.getVerdi());
        var projection = new JournalpostResponseProjection()
            .kanal()
            .journalposttype()
            .dokumenter(new DokumentInfoResponseProjection()
                .dokumentInfoId()
                .tittel()
                .brevkode()
                .dokumentvarianter(new DokumentvariantResponseProjection()
                    .variantformat()
                    .filnavn()
                    .filtype()
                    .saksbehandlerHarTilgang())
                .logiskeVedlegg(new LogiskVedleggResponseProjection()
                    .tittel()))
            .datoOpprettet()
            .relevanteDatoer(new RelevantDatoResponseProjection()
                .dato()
                .datotype());
        var journalpost = safTjeneste.hentJournalpostInfo(query, projection);
        final LocalDateTime mottattDato = utledMottattDato(journalpost);

        log.info("Fant {} vedlegg på søknad", journalpost.getDokumenter().size());
        boolean hoveddokument = true;
        for (DokumentInfo dokumentInfo : journalpost.getDokumenter()) {
            if (dokumentInfo.getBrevkode() != null && dokumentInfo.getBrevkode().equals("K9_PUNSJ_INNSENDING")) {
               // Oppsummerings-PDFen fra punsj skal ikke klassifiseres under sykdom.
                continue;
            }
            
            final boolean erDigitalPleiepengerSyktBarnSøknad = hoveddokument
                    && journalpost.getKanal() == Kanal.NAV_NO
                    && Brevkode.PLEIEPENGER_BARN_SOKNAD.getOffisiellKode().equals(dokumentInfo.getBrevkode());
            final SykdomDokumentType type = (erDigitalPleiepengerSyktBarnSøknad || !harMedisinskeOpplysninger) ? SykdomDokumentType.ANNET : SykdomDokumentType.UKLASSIFISERT;
            final LocalDate datert = erDigitalPleiepengerSyktBarnSøknad ? mottattDato.toLocalDate() : null;
            final SykdomDokumentInformasjon informasjon = new SykdomDokumentInformasjon(
                type,
                harInfoSomIkkeKanPunsjes,
                datert,
                mottattDato,
                0L,
                "VL",
                mottattidspunkt);
            final SykdomDokument dokument = new SykdomDokument(
                journalpostId,
                dokumentInfo.getDokumentInfoId(),
                informasjon,
                behandling.getUuid(),
                behandling.getFagsak().getSaksnummer(),
                sykdomVurderingRepository.hentEllerLagrePerson(behandling.getFagsak().getAktørId()),
                "VL",
                mottattidspunkt);
            sykdomDokumentRepository.lagre(dokument, pleietrengendeAktørId);

            hoveddokument = false;
        }

    }

    private LocalDateTime utledMottattDato(Journalpost journalpost) {
        final LocalDateTime mottattDato;
        if (journalpost.getJournalposttype() == Journalposttype.I) {
            mottattDato = hentRelevantDato(journalpost, Datotype.DATO_REGISTRERT);
        } else {
            mottattDato = hentRelevantDato(journalpost, Datotype.DATO_JOURNALFOERT);
        }
        return mottattDato;
    }

    private LocalDateTime hentRelevantDato(Journalpost journalpost, Datotype datotype) {
        if (journalpost.getRelevanteDatoer() == null) {
            // Hack grunnet feil i VTP. Fjernes straks VTP har blitt rettet.
            return LocalDateTime.now();
        }
        return journalpost.getRelevanteDatoer()
                .stream()
                .filter(d -> d.getDatotype() == datotype)
                .findFirst()
                .orElseThrow()
                .getDato();
    }
}
