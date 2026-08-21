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
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.SøktStartdato;
import no.nav.ung.sak.behandlingslager.behandling.startdato.VurdertSøktPeriode;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
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
public class AktivitetspengerVurdererSøknadsfristTjeneste implements VurderSøknadsfristTjeneste<SøktStartdato> {

    private MottatteDokumentRepository mottatteDokumentRepository;
    private StartdatoRepository startdatoRepository;

    AktivitetspengerVurdererSøknadsfristTjeneste() {
        // for CDI proxy
    }

    @Inject
    public AktivitetspengerVurdererSøknadsfristTjeneste(MottatteDokumentRepository mottatteDokumentRepository,
                                                        StartdatoRepository startdatoRepository) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.startdatoRepository = startdatoRepository;
    }


    @Override
    public Map<KravDokument, List<VurdertSøktPeriode<SøktStartdato>>> vurderSøknadsfrist(BehandlingReferanse referanse) {
        var søktePerioder = hentPerioderTilVurdering(referanse);
        return vurderSøknadsfrist(referanse.getBehandlingId(), søktePerioder);
    }

    @Override
    public Map<KravDokument, List<SøktPeriode<SøktStartdato>>> hentPerioderTilVurdering(BehandlingReferanse referanse) {
        var result = new HashMap<KravDokument, List<SøktPeriode<SøktStartdato>>>();

        var mottatteDokumenter = mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(referanse.getFagsakId())
            .stream()
            .filter(it -> Brevkode.AKTIVITETSPENGER_SOKNAD.equals(it.getType()))
            .collect(Collectors.toSet());

        if (mottatteDokumenter.isEmpty()) {
            return result;
        }

        Collection<SøktStartdato> søktePerioder = startdatoRepository.hentGrunnlag(referanse.getBehandlingId()).stream().flatMap(it -> it.getRelevanteStartdatoer().getStartdatoer().stream()).collect(Collectors.toSet());

        for (SøktStartdato søktStartdato : søktePerioder) {
            Optional<MottattDokument> dokumentOpt = mottatteDokumenter.stream().filter(md -> md.getJournalpostId().equals(søktStartdato.getJournalpostId())).findFirst();
            if (dokumentOpt.isPresent()) {
                MottattDokument dokument = dokumentOpt.get();
                KravDokument kravdokument = new KravDokument(dokument.getJournalpostId(), dokument.getInnsendingstidspunkt(), KravDokumentType.SØKNAD, dokument.getKildesystem());
                DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMed(søktStartdato.getStartdato());
                SøktPeriode<SøktStartdato> søktPeriode = new SøktPeriode<>(periode, søktStartdato);
                result.computeIfAbsent(kravdokument, _ -> new ArrayList<>())
                    .add(søktPeriode);
            }
        }
        return result;
    }

    @Override
    public Map<KravDokument, List<VurdertSøktPeriode<SøktStartdato>>> vurderSøknadsfrist(Long behandlingId, Map<KravDokument, List<SøktPeriode<SøktStartdato>>> søknaderMedPerioder) {
        var result = new HashMap<KravDokument, List<VurdertSøktPeriode<SøktStartdato>>>();

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
