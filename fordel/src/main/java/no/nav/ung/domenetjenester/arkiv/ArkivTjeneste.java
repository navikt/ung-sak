package no.nav.ung.domenetjenester.arkiv;

import static no.nav.k9.felles.integrasjon.saf.Datotype.DATO_DOKUMENT;
import static no.nav.ung.domenetjenester.arkiv.JournalpostProjectionBuilder.byggJournalpostResponseProjection;
import static no.nav.ung.kodeverk.dokument.VariantFormat.ORIGINAL;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.saf.BrukerIdType;
import no.nav.k9.felles.integrasjon.saf.DokumentInfo;
import no.nav.k9.felles.integrasjon.saf.HentDokumentQuery;
import no.nav.k9.felles.integrasjon.saf.Journalpost;
import no.nav.k9.felles.integrasjon.saf.JournalpostQueryRequest;
import no.nav.k9.felles.integrasjon.saf.SafTjeneste;
import no.nav.k9.felles.integrasjon.saf.Variantformat;
import no.nav.ung.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.PersonIdent;

@Dependent
public class ArkivTjeneste {

    private SafTjeneste safTjeneste;
    private PersoninfoAdapter personinfoAdapter;

    @Inject
    public ArkivTjeneste(SafTjeneste safTjeneste,  PersoninfoAdapter personinfoAdapter) {
        this.safTjeneste = safTjeneste;
        this.personinfoAdapter = personinfoAdapter;
    }

    public JournalpostInfo hentJournalpostInfo(JournalpostId journalPostId) {
        Objects.requireNonNull(journalPostId);
        var jpQuery = new JournalpostQueryRequest();
        jpQuery.setJournalpostId(journalPostId.getVerdi());

        var journalpost = safTjeneste.hentJournalpostInfo(jpQuery, byggJournalpostResponseProjection());

        var info = new JournalpostInfo();
        info.setJournalpostId(journalPostId.getVerdi());
        info.setIdent(mapIdent(journalpost));
        info.setForsendelseTidspunkt(utledForsendelseTidspunkt(journalpost));
        info.setType(journalpost.getJournalposttype().name());
        info.setJournalstatus(journalpost.getJournalstatus());
        info.setTema(journalpost.getTema());

        var sak = journalpost.getSak();
        if (sak != null) {
            info.setFagsakSystem(sak.getFagsaksystem());
            info.setFagsakId(sak.getFagsakId());
        }

        journalpost.getDokumenter().stream().findFirst().ifPresent(hoveddokument -> {
            info.setTittel(hoveddokument.getTittel());
            info.setBrevkode(hoveddokument.getBrevkode());
            info.setDokumentInfoId(hoveddokument.getDokumentInfoId());

            if (inneholderStrukturertInformasjon(hoveddokument)) {
                var payload = hentStrukturertInformasjon(journalPostId, hoveddokument);
                info.setStrukturertPayload(new String(payload));
            }
        });

        return info;
    }

    private boolean inneholderStrukturertInformasjon(DokumentInfo hoveddokument) {
        return hoveddokument.getDokumentvarianter().stream().anyMatch(at -> Variantformat.ORIGINAL.equals(at.getVariantformat()));
    }

    private byte[] hentStrukturertInformasjon(JournalpostId journalPostId, DokumentInfo hoveddokument) {
        var query = new HentDokumentQuery(journalPostId.getVerdi(), hoveddokument.getDokumentInfoId(), ORIGINAL.getOffisiellKode());
        var payload = safTjeneste.hentDokument(query);

        return Objects.requireNonNull(payload);
    }

    private LocalDateTime utledForsendelseTidspunkt(Journalpost journalpost) {
        var datoDokument = journalpost.getRelevanteDatoer()
                .stream()
                .filter(relevantDato -> DATO_DOKUMENT.equals(relevantDato.getDatotype()))
                .findFirst();
        if (datoDokument.isPresent()) {
            return datoDokument.get().getDato();
        }
        return journalpost.getDatoOpprettet();
    }

    private AktørId mapIdent(Journalpost journalpost) {
        var bruker = journalpost.getBruker();
        if (bruker == null) {
            return null;
        }
        if (BrukerIdType.AKTOERID.equals(bruker.getType())) {
            return new AktørId(bruker.getId());
        } else if (BrukerIdType.FNR.equals(bruker.getType())) {
            return personinfoAdapter.hentAktørIdForPersonIdent(new PersonIdent(bruker.getId())).orElseThrow();
        }
        throw new IllegalArgumentException("Ukjent brukerType=" + bruker.getType());
    }
}
