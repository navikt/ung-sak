package no.nav.k9.sak.dokument.arkiv;

import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.dokument.ArkivFilType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentKategori;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.kodeverk.dokument.Kommunikasjonsretning;
import no.nav.k9.kodeverk.dokument.VariantFormat;
import no.nav.k9.sak.dokument.arkiv.saf.SafTjeneste;
import no.nav.k9.sak.dokument.arkiv.saf.graphql.DokumentoversiktFagsakQuery;
import no.nav.k9.sak.dokument.arkiv.saf.graphql.HentDokumentQuery;
import no.nav.k9.sak.dokument.arkiv.saf.rest.model.Datotype;
import no.nav.k9.sak.dokument.arkiv.saf.rest.model.DokumentInfo;
import no.nav.k9.sak.dokument.arkiv.saf.rest.model.DokumentoversiktFagsak;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;

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

    public byte[] hentDokumnet(JournalpostId journalpostId, String dokumentId) {
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
        DokumentoversiktFagsakQuery query = new DokumentoversiktFagsakQuery(saksnummer.getVerdi(), Fagsystem.K9SAK.getOffisiellKode());

        DokumentoversiktFagsak oversikt = safTjeneste.dokumentoversiktFagsak(query);

        return Optional.ofNullable(oversikt.getJournalposter()).orElse(List.of())
            .stream()
            .map(journalpost -> opprettArkivJournalPost(saksnummer, journalpost).build())
            .collect(toList());
    }

    public Optional<ArkivJournalPost> hentJournalpostForSak(Saksnummer saksnummer, JournalpostId journalpostId) {
        List<ArkivJournalPost> journalPosts = hentAlleJournalposterForSak(saksnummer);
        return journalPosts.stream().filter(jpost -> journalpostId.equals(jpost.getJournalpostId())).findFirst();
    }

    public Set<DokumentTypeId> hentDokumentTypeIdForSak(Saksnummer saksnummer, LocalDate mottattEtterDato) {
        List<ArkivJournalPost> journalPosts = hentAlleJournalposterForSak(saksnummer);
        Set<DokumentTypeId> etterDato = new HashSet<>();
        journalPosts.stream().filter(jpost -> (LocalDate.MIN.equals(mottattEtterDato)) || (jpost.getTidspunkt() != null && !jpost.getTidspunkt().toLocalDate().isBefore(mottattEtterDato)))
            .forEach(jpost -> {
                ekstraherDTID(etterDato, jpost.getHovedDokument());
                jpost.getAndreDokument().forEach(dok -> ekstraherDTID(etterDato, dok));
            });
        return etterDato;
    }

    private void ekstraherDTID(Set<DokumentTypeId> eksisterende, ArkivDokument dokument) {
        if (dokument == null) {
            return;
        }
        if (!eksisterende.contains(dokument.getDokumentType())) {
            eksisterende.add(dokument.getDokumentType());
        }
        for (ArkivDokumentVedlegg vedlegg : dokument.getInterneVedlegg()) {
            if (!eksisterende.contains(vedlegg.getDokumentTypeId())) {
                eksisterende.add(vedlegg.getDokumentTypeId());
            }
        }
    }

    private ArkivJournalPost.Builder opprettArkivJournalPost(Saksnummer saksnummer, no.nav.k9.sak.dokument.arkiv.saf.rest.model.Journalpost journalpost) {
        Optional<LocalDateTime> datoRegistrert = hentRelevantDato(journalpost, Datotype.DATO_REGISTRERT);
        Optional<LocalDateTime> datoJournalFørt = hentRelevantDato(journalpost, Datotype.DATO_JOURNALFOERT);
        LocalDateTime tidspunkt = datoJournalFørt.orElse(datoRegistrert.orElse(null));

        ArkivJournalPost.Builder builder = ArkivJournalPost.Builder.ny()
            .medSaksnummer(saksnummer)
            .medJournalpostId(new JournalpostId(journalpost.getJournalpostId()))
            .medBeskrivelse(journalpost.getTittel())
            .medTidspunkt(tidspunkt)
            .medKommunikasjonsretning(Kommunikasjonsretning.fromKommunikasjonsretningCode(journalpost.getJournalposttype()));

        List<DokumentInfo> dokumenter = journalpost.getDokumenter();
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
            .medTittel(dokumentInfo.getTittel())
            .medDokumentTypeId(mapTilDokumentTypeId(dokumentInfo.getBrevkode()))
            .medDokumentKategori(mapTilDokumentKategori(dokumentInfo.getBrevkode()));

        dokumentInfo.getDokumentvarianter().stream()
            .filter(innhold -> innhold.getSaksbehandlerHarTilgang())
            .forEach(innhold -> {
                builder.leggTilTilgjengeligFormat(ArkivDokumentHentbart.Builder.ny()
                    .medArkivFilType(
                        innhold.getFiltype() != null ? ArkivFilType.finnForKodeverkEiersKode(innhold.getFiltype()) : ArkivFilType.UDEFINERT)
                    .medVariantFormat(innhold.getVariantFormat() != null ? VariantFormat.finnForKodeverkEiersKode(innhold.getVariantFormat().name())
                        : VariantFormat.UDEFINERT)
                    .build());
            });
        return builder;
    }

    // TODO (ESSV): Workaround mens vi venter på avklaring mapping Brevkode -> DokumentTypeId
    private DokumentTypeId mapTilDokumentTypeId(String brevkodeVerdi) {
        Brevkode brevKode = finnBrevKodeFraKodeverk(brevkodeVerdi);

        DokumentTypeId dokumentTypeId = Arrays.stream(DokumentTypeId.values())
            .filter(typeId -> typeId.name().equals(brevKode.name()))
            .findFirst()
            .orElse(DokumentTypeId.UDEFINERT);

        return dokumentTypeId;
    }

    private Brevkode finnBrevKodeFraKodeverk(String brevkodeVerdi) {
        Brevkode brevKode;
        try {
            brevKode = Brevkode.finnForKodeverkEiersKode(brevkodeVerdi);
        } catch (Exception e) {
            brevKode = Brevkode.UDEFINERT;
        }
        return brevKode;
    }

    // TODO (ESSV): Workaround mens vi venter på avklaring mapping Brevkode -> DokumentKategori
    private DokumentKategori mapTilDokumentKategori(String brevkodeVerdi) {
        Brevkode brevKode = finnBrevKodeFraKodeverk(brevkodeVerdi);

        DokumentKategori dokumentKategori = Arrays.stream(DokumentKategori.values())
            .filter(typeId -> typeId.name().equals(brevKode.name()))
            .findFirst()
            .orElse(DokumentKategori.UDEFINERT);

        return dokumentKategori;
    }

    private Optional<LocalDateTime> hentRelevantDato(no.nav.k9.sak.dokument.arkiv.saf.rest.model.Journalpost journalpost, Datotype datotype) {
        return Optional.ofNullable(journalpost.getRelevanteDatoer()).orElse(List.of()).stream()
            .filter(it -> it.getDatotype().equals(datotype))
            .map(it -> it.getDato())
            .findFirst();
    }
}
