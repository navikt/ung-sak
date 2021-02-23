package no.nav.k9.sak.ytelse.pleiepengerbarn.mottak;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.stream.Stream;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import net.bytebuddy.asm.Advice;
import no.nav.k9.sak.kontrakt.sykdom.dokument.SykdomDokumentType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokumentRepository;
import no.nav.saf.Datotype;
import no.nav.saf.DokumentInfo;
import no.nav.saf.DokumentInfoResponseProjection;
import no.nav.saf.DokumentvariantResponseProjection;
import no.nav.saf.JournalpostQueryRequest;
import no.nav.saf.JournalpostResponseProjection;
import no.nav.saf.LogiskVedleggResponseProjection;
import no.nav.saf.RelevantDato;
import no.nav.saf.RelevantDatoResponseProjection;
import no.nav.vedtak.felles.integrasjon.saf.SafTjeneste;

@Dependent
public class SykdomsDokumentVedleggHåndterer {

    private SykdomDokumentRepository sykdomDokumentRepository;
    private SafTjeneste safTjeneste;

    @Inject
    public SykdomsDokumentVedleggHåndterer(SykdomDokumentRepository sykdomDokumentRepository, SafTjeneste safTjeneste) {
        this.safTjeneste = safTjeneste;
        this.sykdomDokumentRepository = sykdomDokumentRepository;
    }

    void leggTilDokumenterSomSkalHåndteresVedlagtSøknaden(JournalpostId journalpostId, AktørId pleietrengendeAktørId, LocalDateTime mottattidspunkt) {
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
                    .saksbehandlerHarTilgang()
                )
                .logiskeVedlegg(new LogiskVedleggResponseProjection()
                    .tittel()))
            .datoOpprettet()
            .relevanteDatoer(new RelevantDatoResponseProjection()
                .dato()
                .datotype()
            );
        var journalpost = safTjeneste.hentJournalpostInfo(query, projection);
        LocalDateTime mottattDato = journalpost.getRelevanteDatoer().stream().filter(d -> d.getDatotype() == Datotype.DATO_REGISTRERT).findFirst().get().getDato().toInstant().atZone(ZoneId.of("Europe/Oslo")).toLocalDateTime();
        for (DokumentInfo dokumentInfo : journalpost.getDokumenter()) {
            final SykdomDokument dokument = new SykdomDokument(
                SykdomDokumentType.UKLASSIFISERT,
                mottattDato,
                journalpostId,
                dokumentInfo.getDokumentInfoId(),
                "VL",
                mottattidspunkt,
                "VL",
                mottattidspunkt);
            sykdomDokumentRepository.lagre(dokument, pleietrengendeAktørId);
        }

    }
}
