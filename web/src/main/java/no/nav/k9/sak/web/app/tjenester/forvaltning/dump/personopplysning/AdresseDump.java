package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.personopplysning;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.web.app.tjenester.forvaltning.CsvOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.OMSORGSPENGER)
public class AdresseDump implements DebugDumpFagsak {

    private EntityManager entityManager;

    AdresseDump() {
        //
    }

    @Inject
    public AdresseDump(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void dump(DumpMottaker dumpMottaker) {
        String sql = "select "
            + "  b.id as behandling_id, "
            + "  pregadr.id as pregadr_id, "
            + "  pregadr.aktoer_id as aktoer_id, "
            + "  pregadr.fom as fom, "
            + "  pregadr.tom as tom, "
            + "  pregadr.adresse_type as adresse_type, "
            + "  pregadr.adresselinje1 as adresselinje1, "
            + "  pregadr.adresselinje2 as adresselinje2, "
            + "  pregadr.adresselinje3 as adresselinje3, "
            + "  pregadr.adresselinje4 as adresselinje4, "
            + "  pregadr.postnummer as postnummer, "
            + "  pregadr.poststed as poststed, "
            + "  pregadr.land as land, "
            + "  replace(cast(pregadr.opprettet_tid as varchar), ' ', 'T') as opprettet_tid "
            + " from  "
            + "  gr_personopplysning gr  "
            + "  inner join behandling b on b.id=gr.behandling_id "
            + "  inner join fagsak f on f.id=b.fagsak_id "
            + "  left outer join PO_ADRESSE pregadr on pregadr.po_informasjon_id=gr.registrert_informasjon_id "
            + " where gr.aktiv = true "
            + "   and f.saksnummer=:saksnummer";

        Query query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("saksnummer", dumpMottaker.getFagsak().getSaksnummer().getVerdi());

        @SuppressWarnings("unchecked")
        List<Tuple> results = query.getResultList();

        Optional<DumpOutput> output = CsvOutput.dumpResultSetToCsv("adresser.csv", results);
        if (output.isPresent()) {
            dumpMottaker.newFile(output.get().getPath());
            dumpMottaker.write(output.get().getContent());
        }
    }
}
