package no.nav.k9.sak.mottak.repo;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;

import org.hibernate.query.NativeQuery;

import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@Dependent
public class MottatteDokumentRepository {

    private EntityManager entityManager;

    private static final String PARAM_KEY = "param";

    public MottatteDokumentRepository() {
        // for CDI proxy
    }

    @Inject
    public MottatteDokumentRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public MottattDokument lagre(MottattDokument mottattDokument) {
        entityManager.persist(mottattDokument);
        entityManager.flush();

        return mottattDokument;
    }

    public Optional<MottattDokument> hentMottattDokument(long mottattDokumentId) {
        TypedQuery<MottattDokument> query = entityManager.createQuery(
            "select m from MottattDokument m where m.id = :param", MottattDokument.class)
            .setParameter(PARAM_KEY, mottattDokumentId);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    /**
     * Returnerer liste av MottattDokument.
     * NB: Kan returnere samme dokument flere ganger dersom de har ulike eks. mottatt_dato, journalføringsenhet (dersom byttet enhet). Er derfor
     * ikke å anbefale å bruke.
     */
    public List<MottattDokument> hentMottatteDokumentMedFagsakId(long fagsakId) {
        String strQueryTemplate = "select m from MottattDokument m where m.fagsakId = :param AND (m.status IS NULL OR m.status=:gyldig)";
        return entityManager.createQuery(
            strQueryTemplate, MottattDokument.class)
            .setParameter(PARAM_KEY, fagsakId)
            .setParameter("gyldig", DokumentStatus.GYLDIG)
            .getResultList();
    }

    public List<MottattDokument899> hentTSF_899() {
        String sql = "select distinct " +
            "             d2.journalpost_id," +
            "             null as saksnummer, " +
            "             d2.periode_fom, " +
            "             d2.periode_tom, " +
            "             d2.delvis_dato, " +
            "             d2.im, " +
            "             d2.behandling_id," +
            "             null as aksjonspunkt_def," +
            "             d2.fagsak_id," +
            "             d2.opprettet_tid " +
            "             from (" +
            "                   select " +
            "                       substring(im from '.*<fom>\\s*(20[01][0-9]-[0-1][0-9]-[0-3][0-9])\\s*</fom>.*') as periode_fom," +
            "                       substring(im from '.*<tom>\\s*(20[0-2][0-9]-[0-1][0-9]-[0-3][0-9])\\s*</tom>.*') as periode_tom," +
            "                       substring(im from '.*<dato>\\s*(20[01][0-9]-[0-1][0-9]-[0-3][0-9])\\s*</dato>.*') as delvis_dato," +
            "                     d.* " +
            "                   from (" +
            "                    select " +
            "                      m.journalpost_id," +
            "                      m.opprettet_tid," +
            "                      m.behandling_id," +
            "                      m.fagsak_id," +
            "                      convert_from(lo_get(payload), 'UTF8')  as im" +
            "                     from mottatt_dokument m " +
            "                     where type='INNTEKTSMELDING' AND payload IS NOT NULL AND (status IS NULL OR status='GYLDIG')) d" +
            "              ) d2 " +
            "             inner join fagsak f on f.id = d2.fagsak_id"
            + " left outer join behandling b on b.id = d2.behandling_id AND b.behandling_status IN ('OPPRE', 'UTRED') "
            + " left outer join aksjonspunkt a on a.behandling_id=b.id AND a.aksjonspunkt_status='OPPR' "
            + " where (d2.periode_fom is not null or d2.delvis_dato is not null)"
            + " order by d2.periode_fom desc nulls last, d2.periode_tom desc nulls last, d2.delvis_dato desc nulls last, d2.opprettet_tid desc";

        @SuppressWarnings("unchecked")
        NativeQuery<Tuple> query = (NativeQuery<Tuple>) entityManager.createNativeQuery(sql, Tuple.class);

        Stream<Tuple> stream = query.getResultStream();
        return stream.map(t -> new MottattDokument899(
            t.get(0, String.class),
            t.get(1, String.class),
            t.get(2, String.class),
            t.get(3, String.class),
            t.get(4, String.class),
            t.get(5, String.class),
            t.get(6, BigInteger.class),
            t.get(7, String.class)))
            .collect(Collectors.toList());
    }

    /** @deprecated fjernes når TSF-899 er løst. */
    @Deprecated(forRemoval = true)
    public static class MottattDokument899 {
        private Long journalpostId;
        private String saksnummer;
        private LocalDate fraværFom;
        private LocalDate fraværTom;
        private LocalDate delvisFraværDato;
        private String payload;
        private String aksjonspunktKode;
        private Long behandlingId;

        MottattDokument899(String journalpostId, String saksnummer, String fraværFom, String fraværTom, String delvisFraværDato, String payload, BigInteger behandlingId, String aksjonspunktKode) {
            this.behandlingId = behandlingId == null ? null : behandlingId.longValue();
            this.aksjonspunktKode = aksjonspunktKode;
            this.journalpostId = journalpostId == null ? null : Long.parseLong(journalpostId);
            this.saksnummer = saksnummer;
            this.fraværFom = fraværFom == null ? null : LocalDate.parse(fraværFom);
            this.fraværTom = fraværTom == null ? null : LocalDate.parse(fraværTom);
            this.delvisFraværDato = delvisFraværDato == null ? null : LocalDate.parse(delvisFraværDato);
            this.payload = payload;
        }

        public String getSaksnummer() {
            return saksnummer;
        }

        public LocalDate getFraværFom() {
            return fraværFom;
        }

        public LocalDate getFraværTom() {
            return fraværTom;
        }

        public LocalDate getDelvisFraværDato() {
            return delvisFraværDato;
        }

        public String getPayload() {
            return payload;
        }

        public Long getJournalpostId() {
            return journalpostId;
        }

        public Long getBehandlingId() {
            return behandlingId;
        }

        public String getAksjonspunktKode() {
            return aksjonspunktKode;
        }
    }

}
