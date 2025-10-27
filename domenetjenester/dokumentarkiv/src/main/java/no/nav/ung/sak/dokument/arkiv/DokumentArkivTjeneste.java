package no.nav.ung.sak.dokument.arkiv;

import static java.util.stream.Collectors.toList;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.felles.integrasjon.saf.AvsenderMottakerResponseProjection;
import no.nav.k9.felles.integrasjon.saf.BrukerResponseProjection;
import no.nav.k9.felles.integrasjon.saf.Datotype;
import no.nav.k9.felles.integrasjon.saf.DokumentInfo;
import no.nav.k9.felles.integrasjon.saf.DokumentInfoResponseProjection;
import no.nav.k9.felles.integrasjon.saf.DokumentoversiktFagsakQueryRequest;
import no.nav.k9.felles.integrasjon.saf.DokumentoversiktResponseProjection;
import no.nav.k9.felles.integrasjon.saf.DokumentvariantResponseProjection;
import no.nav.k9.felles.integrasjon.saf.FagsakInput;
import no.nav.k9.felles.integrasjon.saf.HentDokumentQuery;
import no.nav.k9.felles.integrasjon.saf.Journalpost;
import no.nav.k9.felles.integrasjon.saf.JournalpostResponseProjection;
import no.nav.k9.felles.integrasjon.saf.LogiskVedleggResponseProjection;
import no.nav.k9.felles.integrasjon.saf.RelevantDato;
import no.nav.k9.felles.integrasjon.saf.RelevantDatoResponseProjection;
import no.nav.k9.felles.integrasjon.saf.SafTjeneste;
import no.nav.k9.felles.integrasjon.saf.SakResponseProjection;
import no.nav.ung.kodeverk.Fagsystem;
import no.nav.ung.kodeverk.dokument.ArkivFilType;
import no.nav.ung.kodeverk.dokument.Kommunikasjonsretning;
import no.nav.ung.kodeverk.dokument.VariantFormat;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Saksnummer;

@ApplicationScoped
public class DokumentArkivTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(DokumentArkivTjeneste.class);
    // Variantformat ARKIV er den eneste varianten som benyttes for denne tjenesten
    private static final VariantFormat VARIANT_FORMAT_ARKIV = VariantFormat.ARKIV;
    private final Set<ArkivFilType> filTyperPdf = byggArkivFilTypeSet();
    private SafTjeneste safTjeneste;

    DokumentArkivTjeneste() {
        // for CDI proxy
    }

    @Inject
    public DokumentArkivTjeneste(SafTjeneste safTjeneste) {
        this.safTjeneste = safTjeneste;
    }

    private static Set<ArkivFilType> byggArkivFilTypeSet() {
        final ArkivFilType arkivFilTypePdf = ArkivFilType.PDF;
        final ArkivFilType arkivFilTypePdfa = ArkivFilType.PDFA;
        return new HashSet<>(Arrays.asList(arkivFilTypePdf, arkivFilTypePdfa));
    }

    public byte[] hentDokument(JournalpostId journalpostId, String dokumentId) {
        LOG.info("HentDokument: input parametere journalpostId {} dokumentId {}", journalpostId, dokumentId);

        HentDokumentQuery query = new HentDokumentQuery(
            journalpostId.getVerdi(),
            dokumentId,
            VariantFormat.ARKIV.getOffisiellKode());

        byte[] pdfDokument = safTjeneste.hentDokument(query);
        if (pdfDokument == null) {
            throw DokumentArkivTjenesteFeil.FACTORY.hentDokumentIkkeFunnet(query).toException();
        }
        return pdfDokument;
    }

    public List<ArkivJournalPost> hentAlleDokumenterForVisning(Saksnummer saksnummer) {
        List<ArkivJournalPost> journalPosterForSak = hentAlleJournalposterForSak(saksnummer);

        List<ArkivJournalPost> journalPosts = new ArrayList<>();

        journalPosterForSak.forEach(jpost -> {
            if (!erDokumentArkivPdf(jpost.getHovedDokument())) {
                jpost.setHovedDokument(null);
            }

            jpost.getAndreDokument().forEach(dok -> {
                if (!erDokumentArkivPdf(jpost.getHovedDokument())) {
                    jpost.getAndreDokument().remove(dok);
                }
            });
        });
        journalPosterForSak.stream()
            .filter(jpost -> jpost.getHovedDokument() != null || !jpost.getAndreDokument().isEmpty())
            .forEach(journalPosts::add);

        return journalPosts;
    }

    private boolean erDokumentArkivPdf(ArkivDokument arkivDokument) {
        for (ArkivDokumentHentbart format : arkivDokument.getTilgjengeligSom()) {
            if (VARIANT_FORMAT_ARKIV.equals(format.getVariantFormat()) && filTyperPdf.contains(format.getArkivFilType())) {
                return true;
            }
        }
        return false;
    }

    public List<ArkivJournalPost> hentAlleJournalposterForSak(Saksnummer saksnummer) {
        var query = new DokumentoversiktFagsakQueryRequest();
        query.setFagsak(new FagsakInput(saksnummer.getVerdi(), Fagsystem.UNG_SAK.getOffisiellKode()));
        query.setFoerste(1000);
        var projection = byggDokumentoversiktResponseProjection();

        var oversikt = safTjeneste.dokumentoversiktFagsak(query, projection);

        return Optional.ofNullable(oversikt.getJournalposter()).orElse(List.of())
            .stream()
            .map(journalpost -> opprettArkivJournalPost(saksnummer, journalpost).build())
            .collect(toList());
    }

    public Optional<ArkivJournalPost> hentJournalpostForSak(Saksnummer saksnummer, JournalpostId journalpostId) {
        List<ArkivJournalPost> journalPosts = hentAlleJournalposterForSak(saksnummer);
        return journalPosts.stream().filter(jpost -> journalpostId.equals(jpost.getJournalpostId())).findFirst();
    }

    private ArkivJournalPost.Builder opprettArkivJournalPost(Saksnummer saksnummer, Journalpost journalpost) {
        Optional<LocalDateTime> datoRegistrert = hentRelevantDato(journalpost, Datotype.DATO_REGISTRERT);
        Optional<LocalDateTime> datoJournalFørt = hentRelevantDato(journalpost, Datotype.DATO_JOURNALFOERT);
        LocalDateTime tidspunkt = datoJournalFørt.orElse(datoRegistrert.orElse(null));

        ArkivJournalPost.Builder builder = ArkivJournalPost.Builder.ny()
            .medSaksnummer(saksnummer)
            .medJournalpostId(new JournalpostId(journalpost.getJournalpostId()))
            .medBeskrivelse(journalpost.getTittel())
            .medTidspunkt(tidspunkt)
            .medKommunikasjonsretning(Kommunikasjonsretning.fromKommunikasjonsretningCode(journalpost.getJournalposttype().name()));

        var dokumenter = journalpost.getDokumenter();
        for (int i = 0; i < dokumenter.size(); i++) {
            var dokumentInfo = dokumenter.get(i);
            if (i == 0) {
                // Første dokument er Hoveddokument
                builder.medHoveddokument(opprettArkivDokument(dokumentInfo).build());
            } else {
                // Alle andre regnes som Andre dokument
                builder.leggTilAnnetDokument(opprettArkivDokument(dokumentInfo).build());
            }
        }
        return builder;
    }

    private ArkivDokument.Builder opprettArkivDokument(DokumentInfo dokumentInfo) {
        ArkivDokument.Builder builder = ArkivDokument.Builder.ny()
            .medDokumentId(dokumentInfo.getDokumentInfoId())
            .medBrevkode(dokumentInfo.getBrevkode())
            .medTittel(dokumentInfo.getTittel());

        dokumentInfo.getDokumentvarianter().stream()
            .filter(innhold -> innhold.getSaksbehandlerHarTilgang())
            .forEach(innhold -> {
                builder.leggTilTilgjengeligFormat(ArkivDokumentHentbart.Builder.ny()
                    .medArkivFilType(
                        innhold.getFiltype() != null ? ArkivFilType.finnForKodeverkEiersKode(innhold.getFiltype()) : ArkivFilType.UDEFINERT)
                    .medVariantFormat(innhold.getVariantformat() != null ? VariantFormat.finnForKodeverkEiersKode(innhold.getVariantformat().name())
                        : VariantFormat.UDEFINERT)
                    .build());
            });
        return builder;
    }

    private Optional<LocalDateTime> hentRelevantDato(Journalpost journalpost, Datotype datotype) {
        return Optional.ofNullable(journalpost.getRelevanteDatoer()).orElse(List.of()).stream()
            .filter(it -> it.getDatotype().equals(datotype))
            .map(RelevantDato::getDato)
            .findFirst();
    }

    private DokumentoversiktResponseProjection byggDokumentoversiktResponseProjection() {
        return new DokumentoversiktResponseProjection()
            .journalposter(new JournalpostResponseProjection()
                .journalpostId()
                .tittel()
                .journalposttype()
                .journalstatus()
                .kanal()
                .tema()
                .behandlingstema()
                .sak(new SakResponseProjection()
                    .arkivsaksnummer()
                    .arkivsaksystem()
                    .fagsaksystem()
                    .fagsakId())
                .bruker(new BrukerResponseProjection()
                    .id()
                    .type())
                .avsenderMottaker(new AvsenderMottakerResponseProjection()
                    .id()
                    .type()
                    .navn())
                .journalfoerendeEnhet()
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
                )
                .eksternReferanseId());
    }
}
