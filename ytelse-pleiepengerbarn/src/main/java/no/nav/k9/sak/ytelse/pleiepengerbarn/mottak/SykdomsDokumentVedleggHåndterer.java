package no.nav.k9.sak.ytelse.pleiepengerbarn.mottak;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
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
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.kontrakt.sykdom.dokument.SykdomDokumentType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.MapTilBrevkode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.PersonRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomDokumentInformasjon;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomDokumentRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.SykdomVurderingRepository;

@Dependent
public class SykdomsDokumentVedleggHåndterer {

    private static final Logger log = LoggerFactory.getLogger(SykdomsDokumentVedleggHåndterer.class);

    private PleietrengendeSykdomDokumentRepository pleietrengendeSykdomDokumentRepository;
    private SykdomVurderingRepository sykdomVurderingRepository;
    private PersonRepository personRepository;
    private SafTjeneste safTjeneste;
    private Instance<MapTilBrevkode> brevkodeMappere;
    private boolean enableUklassifisertDokSjekk;

    private static Set<String> ETTERSENDELSE_BREVKODER = Set.of(Brevkode.ETTERSENDELSE_PLEIEPENGER_SYKT_BARN.getOffisiellKode(), Brevkode.ETTERSENDELSE_PLEIEPENGER_LIVETS_SLUTTFASE.getOffisiellKode());

    @Inject
    public SykdomsDokumentVedleggHåndterer(PleietrengendeSykdomDokumentRepository pleietrengendeSykdomDokumentRepository,
                                           SykdomVurderingRepository sykdomVurderingRepository,
                                           PersonRepository personRepository,
                                           SafTjeneste safTjeneste,
                                           @Any Instance<MapTilBrevkode> brevkodeMappere,
                                           @KonfigVerdi(value = "ENABLE_UKLASSIFISERT_SYKDOMSDOK_SJEKK", defaultVerdi = "true") boolean enableUklassifisertDokSjekk) {
        this.safTjeneste = safTjeneste;
        this.pleietrengendeSykdomDokumentRepository = pleietrengendeSykdomDokumentRepository;
        this.personRepository = personRepository;
        this.sykdomVurderingRepository = sykdomVurderingRepository;
        this.brevkodeMappere = brevkodeMappere;
        this.enableUklassifisertDokSjekk = enableUklassifisertDokSjekk;
    }

    public void leggTilDokumenterSomSkalHåndteresVedlagtSkjema(Behandling behandling, JournalpostId journalpostId, AktørId pleietrengendeAktørId, LocalDateTime mottattidspunkt, boolean harInfoSomIkkeKanPunsjes, boolean harMedisinskeOpplysninger) {
        Journalpost journalpost = hentJournalpost(journalpostId);
        final LocalDateTime mottattDato = utledMottattDato(journalpost);

        log.info("Fant {} vedlegg på søknad eller ettersendelse", journalpost.getDokumenter().size());

        var brevkodeSøknad = MapTilBrevkode.finnBrevkodeMapper(brevkodeMappere, behandling.getFagsakYtelseType()).getBrevkode();

        boolean hoveddokument = true;
        for (DokumentInfo dokumentInfo : journalpost.getDokumenter()) {
            if (skalIgnorereDokument(journalpostId, dokumentInfo)) continue;

            final boolean erDigitalPleiepengerSyktBarnSøknadsskjema =
                hoveddokument
                && journalpost.getKanal() == Kanal.NAV_NO
                && brevkodeSøknad.getOffisiellKode().equals(dokumentInfo.getBrevkode());

            final boolean erDigitalEttersendelseSkjema = enableUklassifisertDokSjekk && hoveddokument
                && journalpost.getKanal() == Kanal.NAV_NO
                && ETTERSENDELSE_BREVKODER.contains(dokumentInfo.getBrevkode());

            final SykdomDokumentType type = erDigitalEttersendelseSkjema
                                            || erDigitalPleiepengerSyktBarnSøknadsskjema
                                            || !harMedisinskeOpplysninger ?
                SykdomDokumentType.ANNET : SykdomDokumentType.UKLASSIFISERT;

            lagreDokument(behandling, journalpostId, pleietrengendeAktørId, mottattidspunkt, harInfoSomIkkeKanPunsjes, dokumentInfo, type, mottattDato);

            hoveddokument = false;
        }

    }


    private Journalpost hentJournalpost(JournalpostId journalpostId) {
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
        return safTjeneste.hentJournalpostInfo(query, projection);
    }

    private boolean skalIgnorereDokument(JournalpostId journalpostId, DokumentInfo dokumentInfo) {
        if (dokumentInfo.getBrevkode() != null && (dokumentInfo.getBrevkode().equals("K9_PUNSJ_INNSENDING")
                                                   || dokumentInfo.getBrevkode().equals(Brevkode.DOKUMENTASJON_AV_OPPLÆRING_KODE))) {
            // Oppsummerings-PDFen fra punsj skal ikke klassifiseres under sykdom.
            // Dokumentasjon av opplæring skal ikke klassifiseres under sykdom.
            return true;
        }

        if (pleietrengendeSykdomDokumentRepository.finnesSykdomDokument(journalpostId, dokumentInfo.getDokumentInfoId())) {
            log.warn("Tidligere innsendt dokument har blitt sendt inn på nytt -- dette skyldes trolig feil hos avsender. Journalpost: " + journalpostId + ", DokumentInfo: " + dokumentInfo.getDokumentInfoId());
            return true;
        }

        return false;
    }

    private void lagreDokument(Behandling behandling, JournalpostId journalpostId, AktørId pleietrengendeAktørId, LocalDateTime mottattidspunkt, boolean harInfoSomIkkeKanPunsjes, DokumentInfo dokumentInfo, SykdomDokumentType type, LocalDateTime mottattDato) {
        final LocalDate datert = type == SykdomDokumentType.ANNET ? mottattDato.toLocalDate() : null;

        final PleietrengendeSykdomDokumentInformasjon informasjon = new PleietrengendeSykdomDokumentInformasjon(
            type,
            harInfoSomIkkeKanPunsjes,
            datert,
            mottattDato,
            0L,
            "VL",
            mottattidspunkt);
        final PleietrengendeSykdomDokument dokument = new PleietrengendeSykdomDokument(
            journalpostId,
            dokumentInfo.getDokumentInfoId(),
            informasjon,
            behandling.getUuid(),
            behandling.getFagsak().getSaksnummer(),
            personRepository.hentEllerLagrePerson(behandling.getFagsak().getAktørId()),
            "VL",
            mottattidspunkt);
        pleietrengendeSykdomDokumentRepository.lagre(dokument, pleietrengendeAktørId);
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
