package no.nav.k9.sak.mottak.kompletthettjeneste.psb;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.NoResultException;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadVedleggEntitet;
import no.nav.k9.sak.mottak.kompletthettjeneste.KompletthetssjekkerSøknad;

public abstract class KompletthetssjekkerSøknadImpl implements KompletthetssjekkerSøknad {

    private SøknadRepository søknadRepository;
    private Period ventefristForTidligSøknad;

    KompletthetssjekkerSøknadImpl() {
        // CDI
    }

    KompletthetssjekkerSøknadImpl(Period ventefristForTidligSøknad,
                                SøknadRepository søknadRepository) {
        this.ventefristForTidligSøknad = ventefristForTidligSøknad;
        this.søknadRepository = søknadRepository;
    }

    @Override
    public Optional<LocalDateTime> erSøknadMottattForTidlig(BehandlingReferanse ref) {
        Optional<LocalDate> permisjonsstart = ref.getSkjæringstidspunkt().getSkjæringstidspunktHvisUtledet();
        if (permisjonsstart.isPresent()) {
            LocalDate ventefrist = permisjonsstart.get().minus(ventefristForTidligSøknad);
            boolean erSøknadMottattForTidlig = ventefrist.isAfter(LocalDate.now());
            if (erSøknadMottattForTidlig) {
                LocalDateTime ventefristTidspunkt = ventefrist.atStartOfDay();
                return Optional.of(ventefristTidspunkt);
            }
        }
        return Optional.empty();
    }

    protected List<ManglendeVedlegg> identifiserManglendeVedlegg(Optional<SøknadEntitet> søknad, Set<DokumentTypeId> dokumentTypeIdSet) {

        return getSøknadVedleggListe(søknad)
            .stream()
            .filter(SøknadVedleggEntitet::isErPåkrevdISøknadsdialog)
            .map(SøknadVedleggEntitet::getSkjemanummer)
            .map(this::finnDokumentTypeId)
            .filter(doc -> !dokumentTypeIdSet.contains(doc))
            .map(ManglendeVedlegg::new)
            .collect(Collectors.toList());
    }

    private Set<SøknadVedleggEntitet> getSøknadVedleggListe(Optional<SøknadEntitet> søknad) {
        if (søknad.map(SøknadEntitet::getElektroniskRegistrert).orElse(false)) {
            return søknad.map(SøknadEntitet::getSøknadVedlegg)
                .orElse(Collections.emptySet());
        }
        return Collections.emptySet();
    }

    private DokumentTypeId finnDokumentTypeId(String dokumentTypeIdKode) {
        DokumentTypeId dokumentTypeId;
        try {
            dokumentTypeId = DokumentTypeId.finnForKodeverkEiersKode(dokumentTypeIdKode);
        } catch (NoResultException e) { // NOSONAR
            // skal tåle dette
            dokumentTypeId = DokumentTypeId.UDEFINERT;
        }
        return dokumentTypeId;
    }

    @Override
    public Boolean erSøknadMottatt(BehandlingReferanse ref) {
        final Optional<SøknadEntitet> søknad = søknadRepository.hentSøknadHvisEksisterer(ref.getBehandlingId());
        return søknad.isPresent();
    }
}
