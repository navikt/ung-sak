package no.nav.k9.sak.ytelse.pleiepengerbarn.mottak;

import java.time.LocalDateTime;
import java.time.ZoneId;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.felles.integrasjon.saf.Datotype;
import no.nav.k9.felles.integrasjon.saf.DokumentInfo;
import no.nav.k9.felles.integrasjon.saf.DokumentInfoResponseProjection;
import no.nav.k9.felles.integrasjon.saf.DokumentvariantResponseProjection;
import no.nav.k9.felles.integrasjon.saf.JournalpostQueryRequest;
import no.nav.k9.felles.integrasjon.saf.JournalpostResponseProjection;
import no.nav.k9.felles.integrasjon.saf.LogiskVedleggResponseProjection;
import no.nav.k9.felles.integrasjon.saf.RelevantDatoResponseProjection;
import no.nav.k9.felles.integrasjon.saf.SafTjeneste;
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

    void leggTilDokumenterSomSkalHåndteresVedlagtSøknaden(Behandling behandling, JournalpostId journalpostId, AktørId pleietrengendeAktørId, LocalDateTime mottattidspunkt, boolean harInfoSomIkkeKanPunsjes) {
        var query = new JournalpostQueryRequest();
        query.setJournalpostId(journalpostId.getVerdi());
        var projection = new JournalpostResponseProjection()
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
        LocalDateTime mottattDato = journalpost.getRelevanteDatoer() == null
            ? LocalDateTime.now() // TODO: sørge for at alltid er med?
            : journalpost.getRelevanteDatoer().stream()
            .filter(d -> d.getDatotype() == Datotype.DATO_REGISTRERT)
            .findFirst()
            .orElseThrow()
            .getDato()
            .toInstant()
            .atZone(ZoneId.of("Europe/Oslo"))
            .toLocalDateTime();

        log.info("Fant {} vedlegg på søknad", journalpost.getDokumenter().size());
        for (DokumentInfo dokumentInfo : journalpost.getDokumenter()) {
            final SykdomDokumentInformasjon informasjon = new SykdomDokumentInformasjon(
                SykdomDokumentType.UKLASSIFISERT,
                null,
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
                harInfoSomIkkeKanPunsjes,
                sykdomVurderingRepository.hentEllerLagrePerson(behandling.getFagsak().getAktørId()),
                "VL",
                mottattidspunkt);
            sykdomDokumentRepository.lagre(dokument, pleietrengendeAktørId);
        }

    }
}
