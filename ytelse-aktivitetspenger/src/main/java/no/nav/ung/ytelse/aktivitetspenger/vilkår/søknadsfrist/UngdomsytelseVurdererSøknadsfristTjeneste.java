package no.nav.ung.ytelse.aktivitetspenger.vilkår.søknadsfrist;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.VurdertSøktPeriode;
import no.nav.ung.sak.behandlingslager.behandling.søknadsperiode.AktivitetspengerSøktPeriode;
import no.nav.ung.sak.behandlingslager.behandling.søknadsperiode.AktivitetspengerSøktPeriodeRepository;
import no.nav.ung.sak.søknadsfrist.KravDokument;
import no.nav.ung.sak.søknadsfrist.KravDokumentType;
import no.nav.ung.sak.søknadsfrist.SøktPeriode;
import no.nav.ung.sak.søknadsfrist.VurderSøknadsfristTjeneste;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.AKTIVITETSPENGER;


@ApplicationScoped
@FagsakYtelseTypeRef(AKTIVITETSPENGER)
public class UngdomsytelseVurdererSøknadsfristTjeneste implements VurderSøknadsfristTjeneste<AktivitetspengerSøktPeriode> {

    private MottatteDokumentRepository mottatteDokumentRepository;
    private AktivitetspengerSøktPeriodeRepository aktivitetspengerSøktPeriodeRepository;


    UngdomsytelseVurdererSøknadsfristTjeneste() {
        // CDI
    }

    @Inject
    public UngdomsytelseVurdererSøknadsfristTjeneste(MottatteDokumentRepository mottatteDokumentRepository,
                                                     AktivitetspengerSøktPeriodeRepository aktivitetspengerSøktPeriodeRepository) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.aktivitetspengerSøktPeriodeRepository = aktivitetspengerSøktPeriodeRepository;
    }


    @Override
    public Map<KravDokument, List<VurdertSøktPeriode<AktivitetspengerSøktPeriode>>> vurderSøknadsfrist(BehandlingReferanse referanse) {
        var søktePerioder = hentPerioderTilVurdering(referanse);
        return vurderSøknadsfrist(referanse.getBehandlingId(), søktePerioder);
    }

    @Override
    public Map<KravDokument, List<SøktPeriode<AktivitetspengerSøktPeriode>>> hentPerioderTilVurdering(BehandlingReferanse referanse) {
        var result = new HashMap<KravDokument, List<SøktPeriode<AktivitetspengerSøktPeriode>>>();

        var mottatteDokumenter = mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(referanse.getFagsakId())
            .stream()
            .filter(it -> Brevkode.AKTIVITETSPENGER_SOKNAD.equals(it.getType()))
            .collect(Collectors.toSet());

        if (mottatteDokumenter.isEmpty()) {
            return result;
        }

        Collection<AktivitetspengerSøktPeriode> søktePerioder = aktivitetspengerSøktPeriodeRepository.hentSøktePerioder(referanse.getBehandlingId());

        for (AktivitetspengerSøktPeriode aktivitetspengerSøktPeriode : søktePerioder) {
            Optional<MottattDokument> dokumentOpt = mottatteDokumenter.stream().filter(md -> md.getJournalpostId().equals(aktivitetspengerSøktPeriode.getJournalpostId())).findFirst();
            if (dokumentOpt.isPresent()) {
                MottattDokument dokument = dokumentOpt.get();
                KravDokument kravdokument = new KravDokument(dokument.getJournalpostId(), dokument.getInnsendingstidspunkt(), KravDokumentType.SØKNAD, dokument.getKildesystem());
                SøktPeriode<AktivitetspengerSøktPeriode> søktPeriode = new SøktPeriode<>(aktivitetspengerSøktPeriode.getPeriode(), aktivitetspengerSøktPeriode);
                result.computeIfAbsent(kravdokument, _ -> new ArrayList<>())
                    .add(søktPeriode);
            }
        }
        return result;
    }

    @Override
    public Map<KravDokument, List<VurdertSøktPeriode<AktivitetspengerSøktPeriode>>> vurderSøknadsfrist(Long behandlingId, Map<KravDokument, List<SøktPeriode<AktivitetspengerSøktPeriode>>> søknaderMedPerioder) {
        var result = new HashMap<KravDokument, List<VurdertSøktPeriode<AktivitetspengerSøktPeriode>>>();

        //TODO legge inn ordentlige regler her for vurdering av søknadsfrist, nå vurders alt til oppfylt
        søknaderMedPerioder.forEach((kravDokument, søktPerioder) -> {
            result.put(kravDokument, søktPerioder.stream()
                .map(it -> new VurdertSøktPeriode<>(it.getPeriode(), Utfall.OPPFYLT, it.getRaw()))
                .toList());
        });

        return result;
    }

    @Override
    public Set<KravDokument> relevanteKravdokumentForBehandling(BehandlingReferanse referanse, boolean taHensynTilManuellRevurdering) {
        return mottatteDokumentRepository.hentMottatteDokumentForBehandling(referanse.getFagsakId(), referanse.getBehandlingId(), List.of(Brevkode.AKTIVITETSPENGER_SOKNAD), false, DokumentStatus.GYLDIG)
            .stream()
            .map(it -> new KravDokument(it.getJournalpostId(), it.getInnsendingstidspunkt(), KravDokumentType.SØKNAD, it.getKildesystem()))
            .collect(Collectors.toSet());
    }

}
